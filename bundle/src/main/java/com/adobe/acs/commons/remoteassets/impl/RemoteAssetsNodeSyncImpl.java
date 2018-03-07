/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.adobe.acs.commons.remoteassets.impl;

import com.adobe.acs.commons.remoteassets.RemoteAssetsNodeSync;
import com.adobe.acs.commons.remoteassets.RemoteAssetsConfig;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.commons.jcr.JcrUtil;
import com.day.cq.dam.api.DamConstants;
import com.google.common.net.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.xss.XSSAPI;
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
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

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
    private static final String DATE_REGEX = "[A-Za-z]{3}\\s[A-Za-z]{3}\\s\\d\\d\\s\\d\\d\\d\\d\\s\\d\\d:\\d\\d:\\d\\d\\s[A-Za-z]{3}[-+]\\d\\d\\d\\d";
    private static final Set<String> PROTECTED_PROPERTIES = new HashSet<>(Arrays.asList(
            JcrConstants.JCR_CREATED, JcrConstants.JCR_CREATED_BY, JcrConstants.JCR_VERSIONHISTORY, JcrConstants.JCR_BASEVERSION,
            JcrConstants.JCR_ISCHECKEDOUT, JcrConstants.JCR_UUID
    ));

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private RemoteAssetsConfig remoteAssetsConfig;

    @Reference
    private DynamicClassLoaderManager dynamicClassLoaderManager;

    @Reference
    private XSSAPI xssApi;

    private ResourceResolver resourceResolver;
    private Session session;
    private ValueFactory valueFactory;

    /**
     * Method to run on activation.
     * @throws RepositoryException Exception
     */
    @Activate
    protected void activate() throws RepositoryException {
        this.resourceResolver = RemoteAssets.logIn(this.resourceResolverFactory);
        this.session = this.resourceResolver.adaptTo(Session.class);
        this.session.getWorkspace().getObservationManager().setUserData(this.remoteAssetsConfig.getEventUserData());
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
        try {
            for (String syncPath : this.remoteAssetsConfig.getSyncPaths()) {
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
        } catch (ParseException e) {
            LOG.error("Parse Exception {}", e);
        } catch (RepositoryException e) {
            LOG.error("Repository Exception {}", e);
        }
    }

    /**
     * Grab JSON from the provided path and sync to the JCR.
     * @param nextPath String
     * @param primaryType String
     * @throws RepositoryException exception
     */
    private Node getOrCreateNode(final String nextPath, final String primaryType) throws RepositoryException {
        Node node = JcrUtils.getNodeIfExists(nextPath, this.session);
        if (node == null) {
            node = JcrUtil.createPath(nextPath, primaryType, this.session);
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
    private JSONObject getJsonFromUri(String path) throws IOException, JSONException {
        // we want to traverse the JCR one level at a time, hence the '1' selector.
        URL url = new URL(this.remoteAssetsConfig.getServer().concat(this.xssApi.getValidHref(path)).concat(".1.json"));
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        String rawAuth = String.format("%s:%s", this.remoteAssetsConfig.getUsername(), this.remoteAssetsConfig.getPassword());
        String encodedAuth = Base64.getEncoder().encodeToString(rawAuth.getBytes(StandardCharsets.UTF_8));
        connection.setRequestProperty("Authorization", String.format("Basic %s", encodedAuth));

        InputStream is = connection.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

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
            return new JSONObject();
        }
    }

    /**
     * Create or update nodes from remote JSON.
     * @param json JSONObject
     * @param parentNode Node
     * @throws IOException exception
     * @throws JSONException exception
     * @throws ParseException exception
     * @throws RepositoryException exception
     */
    private void createOrUpdateNodes(final JSONObject json, final Node parentNode)
            throws IOException, JSONException, ParseException, RepositoryException {

        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (json.get(key) instanceof JSONObject) {
                if (key.equals(DamConstants.THUMBNAIL_NODE)) {
                    continue;
                }

                String objectPath = String.format("%s/%s", parentNode.getPath(), key);
                JSONObject objectJson = getJsonFromUri(objectPath);
                Node node = getOrCreateNode(objectPath, (String) objectJson.get(JcrConstants.JCR_PRIMARYTYPE));
                createOrUpdateNodes(objectJson, node);
            } else if (json.get(key) instanceof JSONArray) {
                if (JcrConstants.JCR_PREDECESSORS.equals(key)) {
                    continue;
                }

                if (JcrConstants.JCR_MIXINTYPES.equals(key)) {
                    JSONArray mixins = (JSONArray) json.get(key);
                    for (int i = 0; i < mixins.length(); i++) {
                        parentNode.addMixin(mixins.getString(i));
                    }
                } else {
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
                    } else {
                        for (int i = 0; i < rawValues.length(); i++) {
                            propertyValues[i] = this.valueFactory.createValue(rawValues.getString(i));
                        }
                    }

                    parentNode.setProperty(key, propertyValues);
                }
            } else {
                try {
                    Node grandparentNode = parentNode.getParent();
                    if (DamConstants.NT_DAM_ASSET.equals(grandparentNode.getProperty(JcrConstants.JCR_PRIMARYTYPE).getValue().getString())
                            && !grandparentNode.hasProperty("isRemoteAsset")) {
                        parentNode.setProperty("isRemoteAsset", true);
                    }

                    // When retrieving JSON from Sling, ':' before a property name denotes a property with a binary value.
                    if (key.startsWith(":")) {
                        setBinary(key, parentNode, json);
                    } else if (parentNode.hasProperty(key) && parentNode.getProperty(key).getString().equals(json.get(key))) {
                        continue;
                    } else if (PROTECTED_PROPERTIES.contains(key)) {
                        continue;
                    } else if (JcrConstants.JCR_PRIMARYTYPE.equals(key)) {
                        parentNode.setPrimaryType((String) json.get(key));
                    } else if (json.get(key) instanceof String && ((String) json.get(key)).matches(DATE_REGEX)) {
                        parentNode.setProperty(key, getFormattedDate((String) json.get(key)));
                    } else {
                        if (json.get(key) instanceof Boolean) {
                            parentNode.setProperty(key, (Boolean) json.get(key));
                        } else if (json.get(key) instanceof Double) {
                            parentNode.setProperty(key, (Double) json.get(key));
                        } else if (json.get(key) instanceof Integer) {
                            parentNode.setProperty(key, (Integer) json.get(key));
                        } else {
                            parentNode.setProperty(key, (String) json.get(key));
                        }
                    }
                } catch (RepositoryException re) {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn("Repository exception thrown. Skipping {} property.", key);
                    }
                }
            }
        }
    }

    /**
     * Set a temporary binary. Also, property 'jcr:data' is a mandatory field.
     * @param key String
     * @param node Node
     * @param jsonObject JSONObject
     * @throws JSONException exception
     * @throws RepositoryException exception
     */
    private void setBinary(final String key, final Node node, final JSONObject jsonObject) throws JSONException, RepositoryException {
        InputStream inputStream;
        if (":".concat(JcrConstants.JCR_DATA).equals(key)) {
            Resource resource = this.resourceResolver.getResource(node.getPath());
            String mimeType = (String) resource.getValueMap().get(JcrConstants.JCR_MIMETYPE);

            if (MediaType.PNG.toString().equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream("/remoteassets/remote_asset.png");
            } else if (MediaType.JPEG.toString().equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream("/remoteassets/remote_asset.jpeg");
            } else if ("image/jpg".equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream("/remoteassets/remote_asset.jpg");
            } else if (MediaType.BMP.toString().equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream("/remoteassets/remote_asset.bmp");
            } else if (MediaType.CSS_UTF_8.toString().equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream("/remoteassets/remote_asset.css");
            } else if (MediaType.OPENDOCUMENT_TEXT.toString().equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream("/remoteassets/remote_asset.doc");
            } else if (MediaType.OOXML_DOCUMENT.toString().equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream("/remoteassets/remote_asset.docx");
            } else if (MediaType.EPUB.toString().equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream("/remoteassets/remote_asset.epub");
            } else if (MediaType.GIF.toString().equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream("/remoteassets/remote_asset.gif");
            } else if (MediaType.HTML_UTF_8.toString().equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream("/remoteassets/remote_asset.html");
            } else if (MediaType.PDF.toString().equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream("/remoteassets/remote_asset.pdf");
            } else if (MediaType.OPENDOCUMENT_PRESENTATION.toString().equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream("/remoteassets/remote_asset.ppt");
            } else if (MediaType.OOXML_PRESENTATION.toString().equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream("/remoteassets/remote_asset.pptx");
            } else if (MediaType.PSD.toString().equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream("/remoteassets/remote_asset.psd");
            } else if (MediaType.SVG_UTF_8.toString().equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream("/remoteassets/remote_asset.svg");
            } else if (MediaType.TIFF.toString().equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream("/remoteassets/remote_asset.tiff");
            } else if (MediaType.PLAIN_TEXT_UTF_8.toString().equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream("/remoteassets/remote_asset.txt");
            } else if (MediaType.OPENDOCUMENT_SPREADSHEET.toString().equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream("/remoteassets/remote_asset.xls");
            } else if (MediaType.OOXML_SHEET.toString().equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream("/remoteassets/remote_asset.xlsx");
            } else if (MediaType.APPLICATION_XML_UTF_8.toString().equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream("/remoteassets/remote_asset.xml");
            } else if (MediaType.ZIP.toString().equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream("/remoteassets/remote_asset.zip");
            } else {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream("/remoteassets/remote_asset.jpeg");
            }

            node.setProperty(JcrConstants.JCR_DATA, this.valueFactory.createBinary(inputStream));
        } else {
            inputStream = new ByteArrayInputStream(String.valueOf(jsonObject.get(key)).getBytes(StandardCharsets.UTF_8));
            node.setProperty(key.replace(":", StringUtils.EMPTY), this.valueFactory.createBinary(inputStream));
        }
    }

    /**
     * Get formatted {@link Calendar} object.
     * @param date String
     * @return Calendar
     * @throws ParseException exception
     */
    private Calendar getFormattedDate(final String date) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss Z");
        sdf.parse(date.replace("GMT", ""));
        return sdf.getCalendar();
    }
}
