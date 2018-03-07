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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.TimeZone;

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

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private RemoteAssetsConfig remoteAssetsConfig;

    @Reference
    private DynamicClassLoaderManager dynamicClassLoaderManager;

    private ResourceResolver resourceResolver;
    private Session session;

    /**
     * Method to run on activation.
     * @throws RepositoryException Exception
     */
    @Activate
    protected void activate() throws RepositoryException {
        this.resourceResolver = RemoteAssets.logIn(this.resourceResolverFactory);
        this.session = this.resourceResolver.adaptTo(Session.class);
        this.session.getWorkspace().getObservationManager().setUserData(this.remoteAssetsConfig.getEventUserData());
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
        } catch (MalformedURLException e) {
            LOG.error("Malformed URL Exception {}", e);
        } catch (JSONException e) {
            LOG.error("Json Exception {}", e);
        } catch (RepositoryException e) {
            LOG.error("Repository Exception {}", e);
        } catch (IOException e) {
            LOG.error("IO Exception {}", e);
        } catch (ParseException e) {
            LOG.error("Parse Exception {}", e);
        }
    }

    /**
     * Grab JSON from the provided path and sync to the JCR.
     * @param nextPath String
     * @throws RepositoryException exception
     */
    private Node getOrCreateNode(final String nextPath, final String primaryType) throws RepositoryException {
        Node node = JcrUtils.getNodeIfExists(nextPath, this.session);
        if (node == null) {
            node = JcrUtil.createPath(nextPath, primaryType, this.session);
            LOG.info("Node {} created!!", node.getPath());
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
        path = path.replace(" ", "%20");
        URL url = new URL(this.remoteAssetsConfig.getServer().concat(path).concat(".1.json"));
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
     * @throws JSONException exception
     * @throws ParseException exception
     * @throws RepositoryException exception
     */
    private void createOrUpdateNodes(final JSONObject json, final Node parentNode)
            throws JSONException, ParseException, RepositoryException, IOException {

        Iterator keys = json.keys();
        while (keys.hasNext()) {
            Object key = keys.next();
            if (json.get((String) key) instanceof JSONObject) {
                String objectPath = String.format("%s/%s", parentNode.getPath(), key);
                JSONObject objectJson = getJsonFromUri(objectPath);
                Node node = getOrCreateNode(objectPath, (String) objectJson.get(JcrConstants.JCR_PRIMARYTYPE));
                createOrUpdateNodes(objectJson, node);
            } else if (json.get((String) key) instanceof JSONArray) {
                if (JcrConstants.JCR_PREDECESSORS.equals(key)) {
                    continue;
                }

                if (JcrConstants.JCR_MIXINTYPES.equals(key)) {
                    JSONArray mixins = (JSONArray) json.get((String) key);
                    for (int i = 0; i < mixins.length(); i++) {
                        parentNode.addMixin(mixins.getString(i));
                    }
                } else {
                    JSONArray rawValues = (JSONArray) json.get((String) key);
                    String[] values = new String[rawValues.length()];

                    for (int i = 0; i < rawValues.length(); i++) {
                        values[i] = rawValues.getString(i);
                    }

                    parentNode.setProperty((String) key, values);
                }
            } else {
                try {
                    // When retrieving JSON from Sling, ':' before a property name denotes a property with a binary value.
                    if (JcrConstants.JCR_DATA.equals(key) || ":".concat(JcrConstants.JCR_DATA).equals(key)) {
                        setTemporaryBinary(parentNode);
                    } else if (parentNode.hasProperty((String) key) && parentNode.getProperty((String) key).getString().equals(json.get((String) key))) {
                        continue;
                    } else if (isPropertyProtected((String) key)) {
                        continue;
                    } else if (JcrConstants.JCR_PRIMARYTYPE.equals(key)) {
                        parentNode.setPrimaryType((String) json.get((String) key));
                    } else if (JcrConstants.JCR_LASTMODIFIED.equals(key)) {
                        parentNode.setProperty((String) key, getFormattedDate((String) json.get((String) key)));
                    } else {
                        if (json.get((String) key) instanceof Boolean) {
                            parentNode.setProperty((String) key, (Boolean) json.get((String) key));
                        } else if (json.get((String) key) instanceof Double) {
                            parentNode.setProperty((String) key, (Double) json.get((String) key));
                        } else if (json.get((String) key) instanceof Integer) {
                            parentNode.setProperty((String) key, (Integer) json.get((String) key));
                        } else {
                            parentNode.setProperty((String) key, (String) json.get((String) key));
                        }
                    }

                    LOG.info("Property {} set on Node {}!!", key, parentNode.getPath());
                } catch (RepositoryException re) {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn("Repository exception thrown. Skipping {} property.", key, re);
                    }
                }
            }
        }
    }

    /**
     * Set a temporary binary. Also, property 'jcr:data' is a mandatory field.
     * @param node Node
     * @throws RepositoryException exception
     */
    private void setTemporaryBinary(final Node node) throws RepositoryException {
//        try {
            ValueFactory valueFactory = this.session.getValueFactory();
            Resource resource = this.resourceResolver.getResource(node.getPath());
            String mimeType = (String) resource.getValueMap().get(JcrConstants.JCR_MIMETYPE);
            InputStream inputStream;

            if (MediaType.PNG.toString().equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream("/remoteassets/remote_asset.png");
            } else if (MediaType.JPEG.toString().equals(mimeType)) {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream("/remoteassets/remote_asset.jpg");
            } else {
                inputStream = this.dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream("/remoteassets/remote_asset.jpg");
            }

            node.setProperty(JcrConstants.JCR_DATA, valueFactory.createBinary(inputStream));
//        } catch (Exception e) {
//            LOG.error("***********************", e);
//        }
    }

    /**
     * Determine whether the provided property is a protected property.
     * @param key String
     */
    private boolean isPropertyProtected(final String key) {
        String[] protectedProperties = new String[] {
                JcrConstants.JCR_CREATED, JcrConstants.JCR_CREATED_BY, JcrConstants.JCR_VERSIONHISTORY, JcrConstants.JCR_BASEVERSION,
                JcrConstants.JCR_ISCHECKEDOUT, JcrConstants.JCR_UUID
        };

        for (String property: protectedProperties) {
            if (property.equals(key)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get formatted {@link Calendar} object.
     * @param date String
     * @return Calendar
     * @throws ParseException exception
     */
    private Calendar getFormattedDate(final String date) throws ParseException {
//        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.getDefault());
//        sdf.parse(date);
//        return sdf.getCalendar();


        Calendar cal = Calendar.getInstance();
        // TODO make use of non-deprecated methods.
        cal.setTime(new Date(date));
        new Date();
        return cal;
    }
}
