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

import com.adobe.acs.commons.remoteassets.RemoteAssetsNodeSync;
import com.adobe.acs.commons.remoteassets.RemoteAssetsConfig;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.commons.jcr.JcrUtil;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.tagging.Tag;
import com.day.cq.tagging.TagManager;
import com.day.cq.wcm.api.NameConstants;
import com.google.common.net.MediaType;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.oak.spi.security.authorization.accesscontrol.AccessControlConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
    private static final Set<String> PROTECTED_PROPERTIES = new HashSet<>(Arrays.asList(
            JcrConstants.JCR_CREATED, JcrConstants.JCR_CREATED_BY, JcrConstants.JCR_VERSIONHISTORY, JcrConstants.JCR_BASEVERSION,
            JcrConstants.JCR_ISCHECKEDOUT, JcrConstants.JCR_UUID, JcrConstants.JCR_PREDECESSORS
    ));
    private static final Set<String> PROTECTED_NODES = new HashSet<>(Arrays.asList(
            DamConstants.THUMBNAIL_NODE, AccessControlConstants.REP_POLICY
    ));

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private RemoteAssetsConfig remoteAssetsConfig;

    @Reference
    private DynamicClassLoaderManager dynamicClassLoaderManager;

    private ResourceResolver resourceResolver;
    private Session session;
    private ValueFactory valueFactory;
    private int saveRefreshCount = 0;

    /**
     * Method to run on activation.
     * @throws RepositoryException exception
     */
    @Activate
    protected void activate() throws RepositoryException {
        this.resourceResolver = RemoteAssets.logIn(this.resourceResolverFactory);
        this.session = this.resourceResolver.adaptTo(Session.class);
        if (StringUtils.isNotBlank(remoteAssetsConfig.getEventUserData())) {
            this.session.getWorkspace().getObservationManager().setUserData(this.remoteAssetsConfig.getEventUserData());
        }
        this.valueFactory = this.session.getValueFactory();
    }

    /**
     * Method to run on deactivation.
     */
    @Deactivate
    protected void deactivate() {
        if (this.session != null) {
            try {
                this.session.logout();
            } catch (Exception e) {
                LOG.warn("Failed session.logout()", e);
            }
        }

        if (this.resourceResolver != null) {
            try {
                this.resourceResolver.close();
            } catch (Exception e) {
                LOG.warn("Failed resourceResolver.close()", e);
            }
        }
    }

    /**
     * @see RemoteAssetsNodeSync#syncAssets()
     */
    @Override
    public void syncAssets() {
        List<String> syncPaths = new ArrayList<>();
        syncPaths.addAll(this.remoteAssetsConfig.getTagSyncPaths());
        syncPaths.addAll(this.remoteAssetsConfig.getDamSyncPaths());

        try {
            for (String syncPath : syncPaths) {
                this.session.refresh(true);
                JSONObject topLevelJson = getJsonFromUri(syncPath);
                Node topLevelSyncNode = getOrCreateNode(syncPath, (String) topLevelJson.get(JcrConstants.JCR_PRIMARYTYPE));
                createOrUpdateNodes(topLevelJson, topLevelSyncNode);
                this.session.save();
            }
        } catch (IOException e) {
            LOG.error("IO Exception {}", e);
        } catch (JSONException e) {
            LOG.error("Json Exception {}", e);
        } catch (RepositoryException e) {
            LOG.error("Repository Exception {}", e);
        }
    }

    /**
     * Grab JSON from the provided path and sync to the JCR.
     * @param nextPath String
     * @param primaryType String
     * @return Node
     */
    private Node getOrCreateNode(final String nextPath, final String primaryType) {
        Node node = null;

        try {
            node = JcrUtils.getNodeIfExists(nextPath, this.session);
            if (node == null) {
                node = JcrUtil.createPath(nextPath, primaryType, this.session);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("New node '{}' created.", node.getPath());
                }
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Node '{}' retrieved from JCR.", node.getPath());
                }
            }
        } catch (RepositoryException e) {
            LOG.error("Repository Exception. Unable to get or create node '{}'", nextPath, e);
        }

        return node;
    }

    /**
     * Get {@link JSONObject} from URL response.
     * @param path String
     * @return JSONObject
     * @throws IOException exception
     * @throws JSONException exception
     */
    private JSONObject getJsonFromUri(final String path) throws IOException, JSONException {
        try {
            URI pathUi = new URI(null, null, path, null);
            // we want to traverse the JCR one level at a time, hence the '1' selector.
            URL url = new URL(this.remoteAssetsConfig.getServer().concat(pathUi.toString()).concat(".1.json"));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            String rawAuth = String.format("%s:%s", this.remoteAssetsConfig.getUsername(), this.remoteAssetsConfig.getPassword());
            String encodedAuth = Base64.getEncoder().encodeToString(rawAuth.getBytes(StandardCharsets.UTF_8));
            connection.setRequestProperty("Authorization", String.format("Basic %s", encodedAuth));

            InputStream is = connection.getInputStream();
            InputStreamReader isReader = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isReader);
            StringBuilder sb = new StringBuilder();

            try {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }

                String sbString = sb.toString();

                if (StringUtils.startsWith(sbString, "{")) {
                    return new JSONObject(sbString);
                } else {
                    if (LOG.isErrorEnabled()) {
                        LOG.error("Unable to grab JSON Object. Please ensure URL {} is valid. \nRaw Response: {}", url.toString(), sbString);
                    }
                }
            } finally {
                is.close();
                isReader.close();
                br.close();

                if (LOG.isDebugEnabled()) {
                    LOG.debug("JSON successfully fetched for URL '{}'.", url.toString());
                }
            }
        } catch (URISyntaxException e) {
            LOG.error("URI Syntax Exception {}", e);
        }

        return new JSONObject();
    }

    /**
     * Create or update nodes from remote JSON.
     * @param json JSONObject
     * @param parentNode Node
     * @throws IOException exception
     * @throws JSONException exception
     * @throws RepositoryException exception
     */
    private void createOrUpdateNodes(final JSONObject json, final Node parentNode) throws IOException, JSONException, RepositoryException {
        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (json.get(key) instanceof JSONObject) {
                if (PROTECTED_NODES.contains(key)) {
                    continue;
                }

                String objectPath = String.format("%s/%s", parentNode.getPath(), key);
                JSONObject objectJson = getJsonFromUri(objectPath);
                Node node = getOrCreateNode(objectPath, (String) objectJson.get(JcrConstants.JCR_PRIMARYTYPE));

                createOrUpdateNodes(objectJson, node);

                if (DamConstants.NT_DAM_ASSET.equals(parentNode.getProperty(JcrConstants.JCR_PRIMARYTYPE).getValue().getString())) {
                    node.setProperty("isRemoteAsset", true);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Property 'isRemoteAsset' added for node '{}'.", key, node.getPath());
                    }

                    // Save and refresh the session after the save refresh count has reached the configured amount.
                    this.saveRefreshCount++;
                    if (this.saveRefreshCount == this.remoteAssetsConfig.getSaveInterval()) {
                        this.session.save();
                        this.session.refresh(true);
                        this.saveRefreshCount = 0;

                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Session has been saved and refreshed.");
                        }
                    }
                }
            } else if (json.get(key) instanceof JSONArray) {
                try {
                    if (PROTECTED_PROPERTIES.contains(key)) {
                        continue;
                    } else if (JcrConstants.JCR_MIXINTYPES.equals(key)) {
                        setMixinsProperty(json, key, parentNode);
                    } else if (NameConstants.PN_TAGS.equals(key)) {
                        setTagsProperty(json, key, parentNode);
                    } else {
                        setArrayProperty(json, key, parentNode);
                    }
                } catch (RepositoryException re) {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn("Repository exception thrown. Skipping {} array property for node '{}'.", key, parentNode.getPath());
                    }
                }
            } else {
                try {
                    Object value = json.get(key);

                    if (":".concat(JcrConstants.JCR_DATA).equals(key)) {
                        setJcrDataProperty(parentNode, json.getString(JcrConstants.JCR_LASTMODIFIED));
                    } else if (key.startsWith(":")) {
                        // Skip binary properties, since they do not come across in JSON
                        continue;
                    } else if (parentNode.hasProperty(key) && parentNode.getProperty(key).getString().equals(value)) {
                        continue;
                    } else if (PROTECTED_PROPERTIES.contains(key)) {
                        continue;
                    } else if (JcrConstants.JCR_PRIMARYTYPE.equals(key)) {
                        parentNode.setPrimaryType((String) value);
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Property '{}' added for node '{}'.", JcrConstants.JCR_PRIMARYTYPE, parentNode.getPath());
                        }
                    } else {
                        setProperty(value, key, parentNode);
                    }
                } catch (RepositoryException re) {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn("Repository exception thrown. Skipping '{}' single property for node '{}'.", key, parentNode.getPath());
                    }
                }
            }
        }
    }

    /**
     * Set mixins property for the property's node.
     * @param json JSONObject
     * @param key String
     * @param node Node
     * @throws JSONException exception
     * @throws RepositoryException exception
     */
    private void setMixinsProperty(final JSONObject json, final String key, final Node node) throws JSONException, RepositoryException {
        JSONArray mixins = (JSONArray) json.get(key);
        for (int i = 0; i < mixins.length(); i++) {
            node.addMixin(mixins.getString(i));
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Mixins added for node '{}'.", node.getPath());
        }
    }

    /**
     * Set tags property for the property's node.
     * @param json JSONObject
     * @param key String
     * @param node Node
     * @throws JSONException exception
     * @throws RepositoryException exception
     */
    private void setTagsProperty(final JSONObject json, final String key, final Node node) throws JSONException, RepositoryException {
        TagManager tagManager = this.resourceResolver.adaptTo(TagManager.class);
        JSONArray tags = (JSONArray) json.get(key);
        ArrayList<Tag> tagList = new ArrayList<>();

        for (int i = 0; i < tags.length(); i++) {
            Tag tag = tagManager.resolve(tags.getString(i));
            if (tag == null) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Tag '{}' could not be found. Skipping tag for node '{}'.", tags.getString(i), node.getPath());
                }

                continue;
            }

            tagList.add(tag);
        }

        if (tagList.size() > 0) {
            Resource parentResource = this.resourceResolver.getResource(node.getPath());
            tagManager.setTags(parentResource, tagList.toArray(new Tag[0]));

            if (LOG.isDebugEnabled()) {
                LOG.debug("Tags added for node '{}'.", node.getPath());
            }
        }
    }

    /**
     * Set generic array property for the property's node.
     * @param json JSONObject
     * @param key String
     * @param node Node
     * @throws JSONException exception
     * @throws RepositoryException exception
     */
    private void setArrayProperty(final JSONObject json, final String key, final Node node) throws JSONException, RepositoryException {
        JSONArray rawValues = (JSONArray) json.get(key);
        Object firstVal = rawValues.get(0);
        Value[] propertyValues = new Value[rawValues.length()];

        if (firstVal instanceof Boolean) {
            for (int i = 0; i < rawValues.length(); i++) {
                propertyValues[i] = this.valueFactory.createValue(rawValues.getBoolean(i));
            }
        } else if (firstVal instanceof Double) {
            for (int i = 0; i < rawValues.length(); i++) {
                propertyValues[i] = this.valueFactory.createValue(rawValues.getDouble(i));
            }
        } else if (firstVal instanceof Integer) {
            for (int i = 0; i < rawValues.length(); i++) {
                propertyValues[i] = this.valueFactory.createValue(rawValues.getInt(i));
            }
        } else if (firstVal instanceof Long) {
            for (int i = 0; i < rawValues.length(); i++) {
                propertyValues[i] = this.valueFactory.createValue(rawValues.getLong(i));
            }
        } else {
            for (int i = 0; i < rawValues.length(); i++) {
                propertyValues[i] = this.valueFactory.createValue(rawValues.getString(i));
            }
        }

        node.setProperty(key, propertyValues);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Array property '{}' added for node '{}'.", key, node.getPath());
        }
    }

    /**
     * Set JCR Data property to a temporary binary for the property's node.
     * Also, property 'jcr:data' is a mandatory field.
     * @param node Node
     * @param rawResponseLastModified String
     * @throws IOException exception
     * @throws RepositoryException exception
     */
    private void setJcrDataProperty(final Node node, final String rawResponseLastModified) throws IOException, RepositoryException {
        if (node.hasProperty(JcrConstants.JCR_LASTMODIFIED) && node.hasProperty(JcrConstants.JCR_DATA)
                && StringUtils.isNotEmpty(rawResponseLastModified)) {

            String nodeLastModified = node.getProperty(JcrConstants.JCR_LASTMODIFIED).getString();
            Calendar responseLastModified = getFormattedDate(rawResponseLastModified);

            if (responseLastModified != null && nodeLastModified.equals(this.valueFactory.createValue(responseLastModified).getString())) {
                return;
            }
        }

        InputStream inputStream = new ByteArrayInputStream(StringUtils.EMPTY.getBytes());

        try {
            Resource resource = this.resourceResolver.getResource(node.getPath());
            String mimeType = (String) resource.getValueMap().get(JcrConstants.JCR_MIMETYPE);

            if (MediaType.JPEG.toString().equals(mimeType) || "image/jpg".equals(mimeType)) {
                Node assetNode = node;
                while (!DamConstants.NT_DAM_ASSET.equals(assetNode.getPrimaryNodeType().getName())) {
                    assetNode = assetNode.getParent();
                }

                if ("jpg".equalsIgnoreCase(FilenameUtils.getExtension(assetNode.getName()))) {
                    inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream("/remoteassets/remote_asset.jpg");
                } else {
                    inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream("/remoteassets/remote_asset.jpeg");
                }
            } else if (MediaType.PNG.toString().equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream("/remoteassets/remote_asset.png");
            } else if (MediaType.BMP.toString().equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream("/remoteassets/remote_asset.bmp");
            } else if ("text/css".equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream("/remoteassets/remote_asset.css");
            } else if (MediaType.OPENDOCUMENT_TEXT.toString().equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream("/remoteassets/remote_asset.doc");
            } else if (MediaType.OOXML_DOCUMENT.toString().equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream("/remoteassets/remote_asset.docx");
            } else if (MediaType.EPUB.toString().equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream("/remoteassets/remote_asset.epub");
            } else if (MediaType.GIF.toString().equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream("/remoteassets/remote_asset.gif");
            } else if ("text/html".equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream("/remoteassets/remote_asset.html");
            } else if (MediaType.PDF.toString().equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream("/remoteassets/remote_asset.pdf");
            } else if (MediaType.OPENDOCUMENT_PRESENTATION.toString().equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream("/remoteassets/remote_asset.ppt");
            } else if (MediaType.OOXML_PRESENTATION.toString().equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream("/remoteassets/remote_asset.pptx");
            } else if (MediaType.PSD.toString().equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream("/remoteassets/remote_asset.psd");
            } else if ("image/svg+xml".equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream("/remoteassets/remote_asset.svg");
            } else if (MediaType.TIFF.toString().equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream("/remoteassets/remote_asset.tiff");
            } else if ("text/plain".equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream("/remoteassets/remote_asset.txt");
            } else if (MediaType.OPENDOCUMENT_SPREADSHEET.toString().equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream("/remoteassets/remote_asset.xls");
            } else if (MediaType.OOXML_SHEET.toString().equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream("/remoteassets/remote_asset.xlsx");
            } else if ("application/xml".equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream("/remoteassets/remote_asset.xml");
            } else if (MediaType.ZIP.toString().equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream("/remoteassets/remote_asset.zip");
            } else {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream("/remoteassets/remote_asset.jpeg");
            }

            node.setProperty(JcrConstants.JCR_DATA, this.valueFactory.createBinary(inputStream));
        } finally {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Binary added for node '{}'.", node.getPath());
            }
            inputStream.close();
        }
    }

    /**
     * Set generic property for the property's node.
     * @param value Object
     * @param key String
     * @param node Node
     * @throws RepositoryException exception
     */
    private void setProperty(final Object value, final String key, final Node node) throws RepositoryException {
        if (value instanceof String && DATE_REGEX.matcher((String) value).matches()) {
            try {
                node.setProperty(key, getFormattedDate((String) value));
            } catch (DateTimeParseException e) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Unable to parse date '{}' for node '{}'.", value, node.getPath());
                }
            }
        } else if (value instanceof String && DECIMAL_REGEX.matcher((String) value).matches()) {
            node.setProperty(key, new BigDecimal((String) value));
        } else if (value instanceof Boolean) {
            node.setProperty(key, (Boolean) value);
        } else if (value instanceof Double) {
            node.setProperty(key, (Double) value);
        } else if (value instanceof Long) {
            node.setProperty(key, (Long) value);
        } else if (value instanceof Integer) {
            node.setProperty(key, (Integer) value);
        } else {
            node.setProperty(key, value == null ? null : value.toString());
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Property '{}' added for node '{}'.", key, node.getPath());
        }
    }

    /**
     * Get formatted {@link Calendar} object.
     * @param dateStr String
     * @return Calendar if parsable, else null.
     * @throws DateTimeParseException exception
     */
    private Calendar getFormattedDate(final String dateStr) throws DateTimeParseException {
        if (StringUtils.isNotEmpty(dateStr)) {
            return GregorianCalendar.from(ZonedDateTime.parse(dateStr, DATE_TIME_FORMATTER));

        }
        return null;
    }
}
