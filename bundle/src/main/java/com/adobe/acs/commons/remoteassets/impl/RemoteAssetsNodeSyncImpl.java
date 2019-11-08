/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2018 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.adobe.acs.commons.remoteassets.impl;

import com.adobe.acs.commons.assets.FileExtensionMimeTypeConstants;
import com.adobe.acs.commons.remoteassets.RemoteAssetsNodeSync;
import com.day.cq.commons.jcr.JcrUtil;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.dam.commons.util.DamUtil;
import com.day.cq.tagging.Tag;
import com.day.cq.tagging.TagManager;
import com.day.cq.wcm.api.NameConstants;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.jackrabbit.oak.spi.security.authorization.accesscontrol.AccessControlConstants;
import org.apache.jackrabbit.vault.util.JcrConstants;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Remote Assets service to sync a node tree from a remote server.
 */
@Component(
        immediate = true,
        service = RemoteAssetsNodeSync.class
)
public class RemoteAssetsNodeSyncImpl implements RemoteAssetsNodeSync {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteAssetsNodeSyncImpl.class);
    private static final Pattern DATE_REGEX = Pattern
            .compile("[A-Za-z]{3}\\s[A-Za-z]{3}\\s\\d\\d\\s\\d\\d\\d\\d\\s\\d\\d:\\d\\d:\\d\\d\\sGMT[-+]\\d\\d\\d\\d");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("EEE MMM dd yyyy HH:mm:ss 'GMT'Z", Locale.US);
    private static final Pattern DECIMAL_REGEX = Pattern.compile("-?\\d+\\.\\d+");
    private static final String ASSET_FILE_PREFIX = "remoteassets/remote_asset";
    private static final Set<String> PROTECTED_PROPERTIES = new HashSet<>(Arrays.asList(
            JcrConstants.JCR_CREATED, JcrConstants.JCR_CREATED_BY, JcrConstants.JCR_VERSIONHISTORY, JcrConstants.JCR_BASEVERSION,
            JcrConstants.JCR_ISCHECKEDOUT, JcrConstants.JCR_UUID, JcrConstants.JCR_PREDECESSORS
    ));
    private static final Set<String> PROTECTED_NODES = new HashSet<>(Arrays.asList(
            DamConstants.THUMBNAIL_NODE, AccessControlConstants.REP_POLICY
    ));

    @Reference
    private RemoteAssetsConfigImpl remoteAssetsConfig;

    private int saveRefreshCount = 0;

    /**
     * @see RemoteAssetsNodeSync#syncAssetNodes()
     */
    @Override
    public void syncAssetNodes() {
        
        try (ResourceResolver remoteAssetsResolver = this.remoteAssetsConfig.getResourceResolver();) {
            List<String> syncPaths = new ArrayList<>();
            syncPaths.addAll(this.remoteAssetsConfig.getTagSyncPaths());
            syncPaths.addAll(this.remoteAssetsConfig.getDamSyncPaths());
            for (String syncPath : syncPaths) {
                LOG.info("Starting sync of nodes for {}", syncPath);
                remoteAssetsResolver.refresh();
                JsonObject topLevelJsonWithChildren = getJsonFromUri(syncPath);
                String resourcePrimaryType = topLevelJsonWithChildren.getAsJsonPrimitive(JcrConstants.JCR_PRIMARYTYPE).getAsString();
                Resource topLevelSyncResource = getOrCreateNode(remoteAssetsResolver, syncPath, resourcePrimaryType);
                createOrUpdateNodes(remoteAssetsResolver, topLevelJsonWithChildren, topLevelSyncResource);
                remoteAssetsResolver.commit();
                LOG.info("Completed sync of nodes for {}", syncPath);
            }
        } catch (Exception e) {
            LOG.error("Unexpected error sync'ing remote asset nodes", e);
        } 
    }

    /**
     * Retrieve or create a node in the JCR.
     *
     * @param nextPath String
     * @param primaryType String
     * @return Resource
     * @throws RepositoryException exception
     */
    private Resource getOrCreateNode(final ResourceResolver remoteAssetsResolver, final String nextPath, final String primaryType) throws RepositoryException {
        Resource resource;

        try {
            resource = remoteAssetsResolver.getResource(nextPath);
            if (resource == null) {
                Node node = JcrUtil.createPath(nextPath, primaryType, remoteAssetsResolver.adaptTo(Session.class));
                resource = remoteAssetsResolver.getResource(node.getPath());
                LOG.debug("New resource '{}' created.", resource.getPath());
            } else {
                LOG.debug("Resource '{}' retrieved from JCR.", resource.getPath());
            }
        } catch (RepositoryException re) {
            LOG.error("Repository Exception. Unable to get or create resource '{}'", nextPath, re);
            throw re;
        }

        return resource;
    }

    /**
     * Get {@link JsonObject} from URL response.
     *
     * @param path String
     * @return JsonObject
     * @throws IOException exception
     */
    private JsonObject getJsonFromUri(final String path) throws IOException {
        URI pathUri;
        try {
            pathUri = new URI(null, null, path, null);
        } catch (URISyntaxException e) {
            LOG.error("URI Syntax Exception", e);
            throw new IOException("Invalid URI", e);
        }

        // we want to traverse the JCR one level at a time, hence the '1' selector.
        String url = this.remoteAssetsConfig.getServer() + pathUri.toString() + ".1.json";
        Executor executor = this.remoteAssetsConfig.getRemoteAssetsHttpExecutor();
        String responseString = executor.execute(Request.Get(url)).returnContent().asString();

        try {
            JsonObject responseJson = new JsonParser().parse(responseString).getAsJsonObject();
            LOG.debug("JSON successfully fetched for URL '{}'.", url);
            return responseJson;
        } catch (JsonSyntaxException | IllegalStateException e) {
            LOG.error("Unable to grab JSON Object. Please ensure URL {} is valid. \nRaw Response: {}", url, responseString);
            throw new IOException("Invalid JSON response", e);
        }
    }

    /**
     * Create or update resources from remote JSON.
     *
     * @param json JsonObject
     * @param resource Resource
     * @throws IOException exception
     * @throws RepositoryException exception
     */
    private void createOrUpdateNodes(final ResourceResolver remoteAssetsResolver, final JsonObject json, final Resource resource) throws IOException, RepositoryException {
        for (Map.Entry<String, JsonElement> jsonEntry : json.entrySet()) {
            JsonElement jsonElement = jsonEntry.getValue();
            if (jsonElement.isJsonObject()) {
                createOrUpdateNodesForJsonObject(remoteAssetsResolver, jsonEntry.getKey(), resource);
            } else if (jsonElement.isJsonArray()) {
                setNodeArrayProperty(remoteAssetsResolver, jsonEntry.getKey(), jsonElement.getAsJsonArray(), resource);
            } else {
                setNodeProperty(remoteAssetsResolver, jsonEntry.getKey(), json, resource);
            }
        }
    }

    /**
     * Handler for when a JSON element is an Object, representing a resource.
     *
     * @param key String
     * @param parentResource Resource
     * @throws IOException exception
     * @throws RepositoryException exception
     */
    private void createOrUpdateNodesForJsonObject(final ResourceResolver remoteAssetsResolver, final String key, final Resource parentResource) throws IOException, RepositoryException {
        if (PROTECTED_NODES.contains(key)) {
            return;
        }

        String objectPath = String.format("%s/%s", parentResource.getPath(), key);
        JsonObject jsonObjectWithChildren = getJsonFromUri(objectPath);
        String resourcePrimaryType = jsonObjectWithChildren.getAsJsonPrimitive(JcrConstants.JCR_PRIMARYTYPE).getAsString();
        Resource resource = getOrCreateNode(remoteAssetsResolver, objectPath, resourcePrimaryType);
        createOrUpdateNodes(remoteAssetsResolver, jsonObjectWithChildren, resource);

        ValueMap resourceProperties = resource.adaptTo(ModifiableValueMap.class);
        if (DamConstants.NT_DAM_ASSET.equals(parentResource.getValueMap().get(JcrConstants.JCR_PRIMARYTYPE, String.class))
                && DamConstants.NT_DAM_ASSETCONTENT.equals(resourceProperties.get(JcrConstants.JCR_PRIMARYTYPE, String.class))) {
            resourceProperties.put(RemoteAssets.IS_REMOTE_ASSET, true);
            LOG.trace("Property '{}' added for resource '{}'.", RemoteAssets.IS_REMOTE_ASSET, resource.getPath());

            // Save and refresh the session after the save refresh count has reached the configured amount.
            this.saveRefreshCount++;
            if (this.saveRefreshCount == this.remoteAssetsConfig.getSaveInterval()) {
                this.saveRefreshCount = 0;
                remoteAssetsResolver.commit();
                remoteAssetsResolver.refresh();
                LOG.info("Executed incremental save of node sync.");
            }
        }
    }

    /**
     * Handler for when a JSON element represents a resource property.
     *
     * @param key String
     * @param json JsonObject
     * @param resource Resource
     * @throws RepositoryException exception
     */
    private void setNodeProperty(final ResourceResolver remoteAssetsResolver, final String key, final JsonObject json, final Resource resource) throws RepositoryException {
        try {
            JsonElement value = json.get(key);

            if (":".concat(JcrConstants.JCR_DATA).equals(key)) {
                setNodeJcrDataProperty(remoteAssetsResolver, resource, json.getAsJsonPrimitive(JcrConstants.JCR_LASTMODIFIED).getAsString());
            } else if (key.startsWith(":")) {
                // Skip binary properties, since they do not come across in JSON
                return;
            } else if (PROTECTED_PROPERTIES.contains(key)) {
                // Skipping due to the property being unmodifiable.
                return;
            } else if (resource.getValueMap().get(key) != null && resource.getValueMap().get(key, String.class).equals(value.getAsString())) {
                // Skipping due to the property already existing and being equal
                return;
            } else {
                setNodeSimpleProperty(value.getAsJsonPrimitive(), key, resource);
            }
        } catch (RepositoryException re) {
            LOG.warn("Repository exception thrown. Skipping '{}' single property for resource '{}'.", key, resource.getPath());
        }
    }

    /**
     * Handler for when a JSON element is an array.
     *
     * @param key String
     * @param jsonArray JsonArray
     * @param resource Resource
     * @throws RepositoryException exception
     */
    private void setNodeArrayProperty(final ResourceResolver remoteAssetsResolver, final String key, final JsonArray jsonArray, final Resource resource) throws RepositoryException {
        try {
            if (PROTECTED_PROPERTIES.contains(key)) {
                // Skipping due to the property being unmodifiable.
                return;
            } else if (JcrConstants.JCR_MIXINTYPES.equals(key)) {
                setNodeMixinsProperty(jsonArray, key, resource);
            } else if (NameConstants.PN_TAGS.equals(key)) {
                setNodeTagsProperty(remoteAssetsResolver, jsonArray, resource);
            } else {
                setNodeSimpleArrayProperty(jsonArray, key, resource);
            }
        } catch (RepositoryException re) {
            LOG.warn("Repository exception thrown. Skipping {} array property for resource '{}'.", key, resource.getPath());
        }
    }

    /**
     * Set mixins property for a resource, based on an array found in the retrieved JSON..
     *
     * @param jsonArray JsonArray
     * @param key String
     * @param resource Resource
     * @throws RepositoryException exception
     */
    protected void setNodeMixinsProperty(final JsonArray jsonArray, final String key, final Resource resource) throws RepositoryException {
        Node node = resource.adaptTo(Node.class);
        for (JsonElement jsonElement : jsonArray) {
            LOG.trace("Adding mixin '{}' for resource '{}'.", jsonElement.getAsString(), resource.getPath());
            node.addMixin(jsonElement.getAsString());
        }
    }

    /**
     * Set tags property for a resource, based on an array found in the retrieved JSON..
     *
     * @param jsonArray JsonArray
     * @param key String
     * @param resource Resource
     * @throws RepositoryException exception
     */
    private void setNodeTagsProperty(final ResourceResolver remoteAssetsResolver, final JsonArray jsonArray, final Resource resource) throws RepositoryException {
        TagManager tagManager = remoteAssetsResolver.adaptTo(TagManager.class);
        ArrayList<Tag> tagList = new ArrayList<>();

        for (JsonElement jsonElement : jsonArray) {
            Tag tag = tagManager.resolve(jsonElement.getAsString());
            if (tag == null) {
                LOG.warn("Tag '{}' could not be found. Skipping tag for resource '{}'.", jsonElement.getAsString(), resource.getPath());
                continue;
            }

            tagList.add(tag);
        }

        if (tagList.size() > 0) {
            tagManager.setTags(resource, tagList.toArray(new Tag[tagList.size()]));
            LOG.trace("Tags added for resource '{}'.", resource.getPath());
        }
    }

    /**
     * Set generic array property for a resource, based on an array found in the retrieved JSON.
     *
     * @param jsonArray JsonArray
     * @param key String
     * @param resource Resource
     * @throws RepositoryException exception
     */
    private void setNodeSimpleArrayProperty(final JsonArray jsonArray, final String key, final Resource resource) throws RepositoryException {
        JsonPrimitive firstVal = jsonArray.get(0).getAsJsonPrimitive();

        try {
            Object[] values;
            if (firstVal.isBoolean()) {
                values = new Boolean[jsonArray.size()];
                for (int i = 0; i < jsonArray.size(); i++) {
                    values[i] = jsonArray.get(i).getAsBoolean();
                }
            } else if (DECIMAL_REGEX.matcher(firstVal.getAsString()).matches()) {
                values = new BigDecimal[jsonArray.size()];
                for (int i = 0; i < jsonArray.size(); i++) {
                    values[i] = jsonArray.get(i).getAsBigDecimal();
                }
            } else if (firstVal.isNumber()) {
                values = new Long[jsonArray.size()];
                for (int i = 0; i < jsonArray.size(); i++) {
                    values[i] = jsonArray.get(i).getAsLong();
                }
            } else {
                values = new String[jsonArray.size()];
                for (int i = 0; i < jsonArray.size(); i++) {
                    values[i] = jsonArray.get(i).getAsString();
                }
            }

            ValueMap resourceProperties = resource.adaptTo(ModifiableValueMap.class);
            resourceProperties.put(key, values);
            LOG.trace("Array property '{}' added for resource '{}'", key, resource.getPath());
        } catch (Exception e) {
            LOG.error("Unable to assign property '{}' to resource '{}'", key, resource.getPath(), e);
        }
    }

    /**
     * Set jcr:data property to a temporary binary for a rendition resource.
     *
     * @param resource Resource
     * @param rawResponseLastModified String
     * @throws RepositoryException exception
     */
    private void setNodeJcrDataProperty(final ResourceResolver remoteAssetsResolver, final Resource resource, final String rawResponseLastModified) throws RepositoryException {
        ValueMap resourceProperties = resource.adaptTo(ModifiableValueMap.class);
        // first checking to make sure existing resource has lastModified and jcr:data properties then seeing if binaries
        // should be updated based off of whether the resource's lastModified matches the JSON's lastModified
        if (resourceProperties.get(JcrConstants.JCR_LASTMODIFIED) != null && resourceProperties.get(JcrConstants.JCR_DATA) != null
                && StringUtils.isNotEmpty(rawResponseLastModified)) {

            String resourceLastModified = resourceProperties.get(JcrConstants.JCR_LASTMODIFIED, String.class);
            Calendar remoteLastModified = GregorianCalendar.from(ZonedDateTime.parse(rawResponseLastModified, DATE_TIME_FORMATTER));

            ValueFactory valueFactory = remoteAssetsResolver.adaptTo(Session.class).getValueFactory();
            if (resourceLastModified.equals(valueFactory.createValue(remoteLastModified).getString())) {
                LOG.debug("Not creating binary for resource '{}' because binary has not been updated.", resource.getPath());
                return;
            }
        }

        InputStream inputStream = getRemoteAssetPlaceholder(resource);
        try {
            resourceProperties.put(JcrConstants.JCR_DATA, inputStream);
            LOG.debug("Binary added for resource '{}'.", resource.getPath());
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException ie) {
                LOG.error("IOException thrown {}", ie);
            }
        }
    }

    /**
     * Set a simple resource property from the fetched JSON.
     *
     * @param value Object
     * @param key String
     * @param resource Resource
     * @throws RepositoryException exception
     */
    private void setNodeSimpleProperty(final JsonPrimitive value, final String key, final Resource resource) throws RepositoryException {
        ValueMap resourceProperties = resource.adaptTo(ModifiableValueMap.class);
        if (value.isString() && DATE_REGEX.matcher(value.getAsString()).matches()) {
            try {
                resourceProperties.put(key, GregorianCalendar.from(ZonedDateTime.parse(value.getAsString(), DATE_TIME_FORMATTER)));
            } catch (DateTimeParseException e) {
                LOG.warn("Unable to parse date '{}' for property:resource '{}'.", value, key + ":" + resource.getPath());
            }
        } else if (value.isString() && DECIMAL_REGEX.matcher(value.getAsString()).matches()) {
            resourceProperties.put(key, value.getAsBigDecimal());
        } else if (value.isBoolean()) {
            resourceProperties.put(key, value.getAsBoolean());
        } else if (value.isNumber()) {
            if (DECIMAL_REGEX.matcher(value.getAsString()).matches()) {
                resourceProperties.put(key, value.getAsBigDecimal());
            } else {
                resourceProperties.put(key, value.getAsLong());
            }
        } else if (value.isJsonNull()) {
            resourceProperties.remove(key);
        } else {
            resourceProperties.put(key, value.getAsString());
        }

        LOG.trace("Property '{}' added for resource '{}'.", key, resource.getPath());
    }

    /**
     * Get the placeholder binary for a given rendition.
     *
     * @param renditionContentResource Asset rendition jcr:content resource
     * @return InputStream for the placeholder binary
     * @throws RepositoryException
     */
    @SuppressWarnings("squid:S1479")
    protected InputStream getRemoteAssetPlaceholder(Resource renditionContentResource) throws RepositoryException {
        String mimeType = (String) renditionContentResource.getValueMap().get(JcrConstants.JCR_MIMETYPE);
        InputStream inputStream;

        switch (mimeType) {
            case FileExtensionMimeTypeConstants.EXT_3G2: {
                inputStream = this.getClass().getClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".3g2");
                break;
            }
            case FileExtensionMimeTypeConstants.EXT_3GP: {
                inputStream = this.getClass().getClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".3gp");
                break;
            }
            case FileExtensionMimeTypeConstants.EXT_AAC: {
                inputStream = this.getClass().getClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".aac");
                break;
            }
            case FileExtensionMimeTypeConstants.EXT_AIFF: {
                inputStream = this.getClass().getClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".aiff");
                break;
            }
            case FileExtensionMimeTypeConstants.EXT_AVI: {
                inputStream = this.getClass().getClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".avi");
                break;
            }
            case FileExtensionMimeTypeConstants.EXT_BMP: {
                inputStream = this.getClass().getClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".bmp");
                break;
            }
            case FileExtensionMimeTypeConstants.EXT_CSS: {
                inputStream = this.getClass().getClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".css");
                break;
            }
            case FileExtensionMimeTypeConstants.EXT_DOC: {
                inputStream = this.getClass().getClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".doc");
                break;
            }
            case FileExtensionMimeTypeConstants.EXT_DOCX: {
                inputStream = this.getClass().getClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".docx");
                break;
            }
            case FileExtensionMimeTypeConstants.EXT_AI_EPS_PS: {
                inputStream = getCorrectBinaryTypeStream(renditionContentResource, "ai", "eps", "ps");
                break;
            }
            case FileExtensionMimeTypeConstants.EXT_EPUB: {
                inputStream = this.getClass().getClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".epub");
                break;
            }
            case FileExtensionMimeTypeConstants.EXT_F4V: {
                inputStream = this.getClass().getClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".f4v");
                break;
            }
            case FileExtensionMimeTypeConstants.EXT_FLA_SWF: {
                inputStream = this.getClass().getClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".swf");
                break;
            }
            case FileExtensionMimeTypeConstants.EXT_GIF: {
                inputStream = this.getClass().getClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".gif");
                break;
            }
            case FileExtensionMimeTypeConstants.EXT_HTML: {
                inputStream = this.getClass().getClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".html");
                break;
            }
            case FileExtensionMimeTypeConstants.EXT_INDD: {
                inputStream = this.getClass().getClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".indd");
                break;
            }
            case FileExtensionMimeTypeConstants.EXT_JAR: {
                inputStream = this.getClass().getClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".jar");
                break;
            }
            case FileExtensionMimeTypeConstants.EXT_JPEG_JPG: {
                inputStream = getCorrectBinaryTypeStream(renditionContentResource, "jpeg", "jpg");
                break;
            }
            case FileExtensionMimeTypeConstants.EXT_M4V: {
                inputStream = this.getClass().getClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".m4v");
                break;
            }
            case FileExtensionMimeTypeConstants.EXT_MIDI: {
                inputStream = this.getClass().getClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".midi");
                break;
            }
            case FileExtensionMimeTypeConstants.EXT_MOV: {
                inputStream = this.getClass().getClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".mov");
                break;
            }
            case FileExtensionMimeTypeConstants.EXT_MP3: {
                inputStream = this.getClass().getClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".mp3");
                break;
            }
            case FileExtensionMimeTypeConstants.EXT_MP4: {
                inputStream = this.getClass().getClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".mp4");
                break;
            }
            case FileExtensionMimeTypeConstants.EXT_M2V_MPEG_MPG: {
                inputStream = getCorrectBinaryTypeStream(renditionContentResource, "m2v", "mpeg", "mpg");
                break;
            }
            case FileExtensionMimeTypeConstants.EXT_OGG: {
                inputStream = this.getClass().getClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".ogg");
                break;
            }
            case FileExtensionMimeTypeConstants.EXT_OGV: {
                inputStream = this.getClass().getClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".ogv");
                break;
            }
            case FileExtensionMimeTypeConstants.EXT_PDF: {
                inputStream = this.getClass().getClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".pdf");
                break;
            }
            case FileExtensionMimeTypeConstants.EXT_PNG: {
                inputStream = this.getClass().getClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".png");
                break;
            }
            case FileExtensionMimeTypeConstants.EXT_PPT: {
                inputStream = this.getClass().getClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".ppt");
                break;
            }
            case FileExtensionMimeTypeConstants.EXT_PPTX: {
                inputStream = this.getClass().getClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".pptx");
                break;
            }
            case FileExtensionMimeTypeConstants.EXT_PSD: {
                inputStream = this.getClass().getClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".psd");
                break;
            }
            case FileExtensionMimeTypeConstants.EXT_RAR: {
                inputStream = this.getClass().getClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".rar");
                break;
            }
            case FileExtensionMimeTypeConstants.EXT_RTF: {
                inputStream = this.getClass().getClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".rtf");
                break;
            }
            case FileExtensionMimeTypeConstants.EXT_SVG: {
                inputStream = this.getClass().getClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".svg");
                break;
            }
            case FileExtensionMimeTypeConstants.EXT_TAR: {
                inputStream = this.getClass().getClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".tar");
                break;
            }
            case FileExtensionMimeTypeConstants.EXT_TIF_TIFF: {
                inputStream = getCorrectBinaryTypeStream(renditionContentResource, "tif", "tiff");
                break;
            }
            case FileExtensionMimeTypeConstants.EXT_TXT: {
                inputStream = this.getClass().getClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".txt");
                break;
            }
            case FileExtensionMimeTypeConstants.EXT_WAV: {
                inputStream = this.getClass().getClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".wav");
                break;
            }
            case FileExtensionMimeTypeConstants.EXT_WEBM: {
                inputStream = this.getClass().getClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".webm");
                break;
            }
            case FileExtensionMimeTypeConstants.EXT_WMA: {
                inputStream = this.getClass().getClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".wma");
                break;
            }
            case FileExtensionMimeTypeConstants.EXT_WMV: {
                inputStream = this.getClass().getClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".wmv");
                break;
            }
            case FileExtensionMimeTypeConstants.EXT_XLS: {
                inputStream = this.getClass().getClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".xls");
                break;
            }
            case FileExtensionMimeTypeConstants.EXT_XLSX: {
                inputStream = this.getClass().getClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".xlsx");
                break;
            }
            case FileExtensionMimeTypeConstants.EXT_XML: {
                inputStream = this.getClass().getClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".xml");
                break;
            }
            case FileExtensionMimeTypeConstants.EXT_ZIP: {
                inputStream = this.getClass().getClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".zip");
                break;
            }
            default: {
                inputStream = this.getClass().getClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".jpeg");
                break;
            }
        }


        return inputStream;
    }

    /**
     * Get the correct temporary binary (file type) based on the renditions file extension
     * or the overall asset's file extension if it is the original rendition.
     *
     * @param renditionContentResource Resource
     * @param fileExtensions String...
     * @return InputStream
     * @throws RepositoryException exception
     */
    private InputStream getCorrectBinaryTypeStream(final Resource renditionContentResource, String... fileExtensions) throws RepositoryException {
        Resource renditionResource = renditionContentResource.getParent();
        Asset assetResource = DamUtil.resolveToAsset(renditionResource);

        String remoteAssetFileUri = ASSET_FILE_PREFIX + "." + fileExtensions[0];
        String assetFileExtension = FilenameUtils.getExtension(assetResource.getName());
        String renditionParentFileExtension = FilenameUtils.getExtension(renditionResource.getName());
        for (String fileExtension : fileExtensions) {
            if (DamConstants.ORIGINAL_FILE.equals(renditionResource.getName()) && fileExtension.equals(assetFileExtension)
                    || !DamConstants.ORIGINAL_FILE.equals(renditionResource.getName()) && fileExtension.equals(renditionParentFileExtension)) {

                remoteAssetFileUri = ASSET_FILE_PREFIX + "." + fileExtension;
                break;
            }
        }

        return this.getClass().getClassLoader().getResourceAsStream(remoteAssetFileUri);
    }
}
