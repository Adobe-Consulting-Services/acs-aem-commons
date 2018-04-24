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
import com.adobe.acs.commons.remoteassets.RemoteAssetsConfig;
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
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.apache.jackrabbit.oak.spi.security.authorization.accesscontrol.AccessControlConstants;
import org.apache.jackrabbit.vault.util.JcrConstants;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Service to sync node tree from supplied felix configuration. Implements {@link RemoteAssetsNodeSync}.
 */
@Component(
        immediate = true,
        metatype = true,
        label = "ACS AEM Commons - Remote Assets - Node Sync Service"
)
@Service
public class RemoteAssetsNodeSyncImpl implements RemoteAssetsNodeSync {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteAssetsNodeSyncImpl.class);
    private static final Pattern DATE_REGEX = Pattern
            .compile("[A-Za-z]{3}\\s[A-Za-z]{3}\\s\\d\\d\\s\\d\\d\\d\\d\\s\\d\\d:\\d\\d:\\d\\d\\sGMT[-+]\\d\\d\\d\\d");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("EEE MMM dd yyyy HH:mm:ss 'GMT'Z");
    private static final Pattern DECIMAL_REGEX = Pattern.compile("-?\\d+\\.\\d+");
    private static final String ASSET_FILE_PREFIX = "/remoteassets/remote_asset";
    private static final Set<String> PROTECTED_PROPERTIES = new HashSet<>(Arrays.asList(
            JcrConstants.JCR_CREATED, JcrConstants.JCR_CREATED_BY, JcrConstants.JCR_VERSIONHISTORY, JcrConstants.JCR_BASEVERSION,
            JcrConstants.JCR_ISCHECKEDOUT, JcrConstants.JCR_UUID, JcrConstants.JCR_PREDECESSORS
    ));
    private static final Set<String> PROTECTED_NODES = new HashSet<>(Arrays.asList(
            DamConstants.THUMBNAIL_NODE, AccessControlConstants.REP_POLICY
    ));

    @Reference
    private RemoteAssetsConfig remoteAssetsConfig;

    @Reference
    private DynamicClassLoaderManager dynamicClassLoaderManager;

    @Reference
    private HttpClientBuilderFactory httpClientBuilderFactory;

    private int saveRefreshCount = 0;

    /**
     * @see RemoteAssetsNodeSync#syncAssetNodes()
     */
    @Override
    public void syncAssetNodes() {
        ResourceResolver remoteAssetsResolver = this.remoteAssetsConfig.getResourceResolver();
        try {
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
        } finally {
            this.remoteAssetsConfig.closeResourceResolver(remoteAssetsResolver);
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
        if (DamConstants.NT_DAM_ASSET.equals(parentResource.getValueMap().get(JcrConstants.JCR_PRIMARYTYPE, String.class))) {
            resourceProperties.put(RemoteAssets.IS_REMOTE_ASSET, true);
            LOG.debug("Property '{}' added for resource '{}'.", RemoteAssets.IS_REMOTE_ASSET, resource.getPath());

            // Save and refresh the session after the save refresh count has reached the configured amount.
            this.saveRefreshCount++;
            if (this.saveRefreshCount == this.remoteAssetsConfig.getSaveInterval()) {
                this.saveRefreshCount = 0;
                remoteAssetsResolver.commit();
                remoteAssetsResolver.refresh();
                LOG.debug("Executed incremental save of node sync.");
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
                setNodeTagsProperty(remoteAssetsResolver, jsonArray, key, resource);
            } else {
                setNodeArrayProperty(remoteAssetsResolver, jsonArray, key, resource);
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
    private void setNodeMixinsProperty(final JsonArray jsonArray, final String key, final Resource resource) throws RepositoryException {
        Node node = resource.adaptTo(Node.class);
        for (JsonElement jsonElement : jsonArray) {
            LOG.debug("Adding mixin '{}' for resource '{}'.", jsonElement.getAsString(), resource.getPath());
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
    private void setNodeTagsProperty(final ResourceResolver remoteAssetsResolver, final JsonArray jsonArray, final String key, final Resource resource) throws RepositoryException {
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
            tagManager.setTags(resource.getParent(), tagList.toArray(new Tag[tagList.size()]));
            LOG.debug("Tags added for resource '{}'.", resource.getPath());
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
    private void setNodeArrayProperty(final ResourceResolver remoteAssetsResolver, final JsonArray jsonArray, final String key, final Resource resource) throws RepositoryException {
        JsonPrimitive firstVal = jsonArray.get(0).getAsJsonPrimitive();

        try {
            Object[] values;
            if (firstVal.isBoolean()) {
                values = new Boolean[jsonArray.size()];
                for (int i = 0; i < jsonArray.size(); i++) {
                    values[i] = jsonArray.get(i).getAsBoolean();
                }
            } else if (firstVal.isNumber()) {
                if (DECIMAL_REGEX.matcher(firstVal.getAsString()).matches()) {
                    values = new Double[jsonArray.size()];
                    for (int i = 0; i < jsonArray.size(); i++) {
                        values[i] = jsonArray.get(i).getAsDouble();
                    }
                } else {
                    values = new Long[jsonArray.size()];
                    for (int i = 0; i < jsonArray.size(); i++) {
                        values[i] = jsonArray.get(i).getAsLong();
                    }
                }
            } else {
                values = new String[jsonArray.size()];
                for (int i = 0; i < jsonArray.size(); i++) {
                    values[i] = jsonArray.get(i).getAsString();
                }
            }

            ValueMap resourceProperties = resource.adaptTo(ModifiableValueMap.class);
            resourceProperties.put(key, values);
            LOG.debug("Array property '{}' added for resource '{}'.", key, resource.getPath());
        } catch (Exception e) {
            LOG.error("Unable to assign property:resource to '{}' {}", key + resource.getPath(), e);
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

        InputStream inputStream = new ByteArrayInputStream(StringUtils.EMPTY.getBytes());

        try {
            String mimeType = (String) resource.getValueMap().get(JcrConstants.JCR_MIMETYPE);

            if (FileExtensionMimeTypeConstants.EXT_3G2.equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".3g2");
            } else if (FileExtensionMimeTypeConstants.EXT_3GP.equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".3gp");
            } else if (FileExtensionMimeTypeConstants.EXT_AAC.equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".aac");
            } else if (FileExtensionMimeTypeConstants.EXT_AIFF.equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".aiff");
            } else if (FileExtensionMimeTypeConstants.EXT_AVI.equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".avi");
            } else if (FileExtensionMimeTypeConstants.EXT_BMP.equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".bmp");
            } else if (FileExtensionMimeTypeConstants.EXT_CSS.equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".css");
            } else if (FileExtensionMimeTypeConstants.EXT_DOC.equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".doc");
            } else if (FileExtensionMimeTypeConstants.EXT_DOCX.equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".docx");
            } else if (FileExtensionMimeTypeConstants.EXT_AI_EPS_PS.equals(mimeType)) {
                inputStream = getCorrectBinaryTypeStream(resource, "ai", "eps", "ps");
            } else if (FileExtensionMimeTypeConstants.EXT_EPUB.equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".epub");
            } else if (FileExtensionMimeTypeConstants.EXT_F4V.equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".f4v");
            } else if (FileExtensionMimeTypeConstants.EXT_FLA_SWF.equals(mimeType)) {
                inputStream = getCorrectBinaryTypeStream(resource, "fla", "swf");
            } else if (FileExtensionMimeTypeConstants.EXT_GIF.equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".gif");
            } else if (FileExtensionMimeTypeConstants.EXT_HTML.equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".html");
            } else if (FileExtensionMimeTypeConstants.EXT_INDD.equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".indd");
            } else if (FileExtensionMimeTypeConstants.EXT_JAR.equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".jar");
            } else if (FileExtensionMimeTypeConstants.EXT_JPEG_JPG.equals(mimeType)) {
                inputStream = getCorrectBinaryTypeStream(resource, "jpeg", "jpg");
            } else if (FileExtensionMimeTypeConstants.EXT_M4V.equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".m4v");
            } else if (FileExtensionMimeTypeConstants.EXT_MIDI.equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".midi");
            } else if (FileExtensionMimeTypeConstants.EXT_MOV.equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".mov");
            } else if (FileExtensionMimeTypeConstants.EXT_MP3.equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".mp3");
            } else if (FileExtensionMimeTypeConstants.EXT_MP4.equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".mp4");
            } else if (FileExtensionMimeTypeConstants.EXT_M2V_MPEG_MPG.equals(mimeType)) {
                inputStream = getCorrectBinaryTypeStream(resource, "m2v", "mpeg", "mpg");
            } else if (FileExtensionMimeTypeConstants.EXT_OGG.equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".ogg");
            } else if (FileExtensionMimeTypeConstants.EXT_OGV.equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".ogv");
            } else if (FileExtensionMimeTypeConstants.EXT_PDF.equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".pdf");
            } else if (FileExtensionMimeTypeConstants.EXT_PNG.equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".png");
            } else if (FileExtensionMimeTypeConstants.EXT_PPT.equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".ppt");
            } else if (FileExtensionMimeTypeConstants.EXT_PPTX.equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".pptx");
            } else if (FileExtensionMimeTypeConstants.EXT_PSD.equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".psd");
            } else if (FileExtensionMimeTypeConstants.EXT_RAR.equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".rar");
            } else if (FileExtensionMimeTypeConstants.EXT_RTF.equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".rtf");
            } else if (FileExtensionMimeTypeConstants.EXT_SVG.equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".svg");
            } else if (FileExtensionMimeTypeConstants.EXT_TAR.equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".tar");
            } else if (FileExtensionMimeTypeConstants.EXT_TIF_TIFF.equals(mimeType)) {
                inputStream = getCorrectBinaryTypeStream(resource, "tif", "tiff");
            } else if (FileExtensionMimeTypeConstants.EXT_TXT.equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".txt");
            } else if (FileExtensionMimeTypeConstants.EXT_WAV.equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".wav");
            } else if (FileExtensionMimeTypeConstants.EXT_WEBM.equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".webm");
            } else if (FileExtensionMimeTypeConstants.EXT_WMA.equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".wma");
            } else if (FileExtensionMimeTypeConstants.EXT_WMV.equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".wmv");
            } else if (FileExtensionMimeTypeConstants.EXT_XLS.equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".xls");
            } else if (FileExtensionMimeTypeConstants.EXT_XLSX.equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".xlsx");
            } else if (FileExtensionMimeTypeConstants.EXT_XML.equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".xml");
            } else if (FileExtensionMimeTypeConstants.EXT_ZIP.equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".zip");
            } else {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream(ASSET_FILE_PREFIX + ".jpeg");
            }

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
                resourceProperties.put(key, value.getAsDouble());
            } else {
                resourceProperties.put(key, value.getAsLong());
            }
        } else if (value.isJsonNull()) {
            resourceProperties.remove(key);
        } else {
            resourceProperties.put(key, value.getAsString());
        }

        LOG.debug("Property '{}' added for resource '{}'.", key, resource.getPath());
    }

    /**
     * Get the correct temporary binary (file type) based on the renditions file extension
     * or the overall asset's file extension if it is the original rendition.
     *
     * @param fileResource Resource
     * @param files String...
     * @return InputStream
     * @throws RepositoryException exception
     */
    private InputStream getCorrectBinaryTypeStream(final Resource fileResource, String... files) throws RepositoryException {
        Resource renditionResource = fileResource.getParent();
        Asset assetResource = DamUtil.resolveToAsset(renditionResource);

        String remoteAssetFileUri = ASSET_FILE_PREFIX + "." + files[0];
        String assetFileExtension = FilenameUtils.getExtension(assetResource.getName());
        String renditionParentFileExtension = FilenameUtils.getExtension(renditionResource.getName());
        for (String file : files) {
            if (DamConstants.ORIGINAL_FILE.equals(renditionResource.getName()) && file.equals(assetFileExtension)
                    || !DamConstants.ORIGINAL_FILE.equals(renditionResource.getName()) && file.equals(renditionParentFileExtension)) {

                remoteAssetFileUri = ASSET_FILE_PREFIX + "." + file;
                break;
            }
        }

        return this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream(remoteAssetFileUri);
    }
}
