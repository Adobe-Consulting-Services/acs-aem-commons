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
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Iterator;

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
                URL url = new URL(this.remoteAssetsConfig.getServer().concat(syncPath).concat(".infinity.json"));
                JSONObject json = getJsonObjectFromUri(url);
                Iterator keys = json.keys();
                Node contentNode = JcrUtils.getNodeIfExists(syncPath, this.session);
                if (contentNode == null) {
                    contentNode = JcrUtil.createPath(syncPath, JcrConstants.NT_UNSTRUCTURED, this.session);
                    LOG.info("Node {} created!!", contentNode.getPath());
                }

                createOrUpdateNodesFromJson(json, keys, contentNode);
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
        }
    }

    /**
     * Get {@link JSONObject} from URL response.
     * @param url URL
     * @return JSONObject
     * @throws IOException exception
     * @throws JSONException exception
     */
    private JSONObject getJsonObjectFromUri(final URL url) throws IOException, JSONException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        String rawAuth = this.remoteAssetsConfig.getUsername().concat(":").concat(this.remoteAssetsConfig.getPassword());
        String encodedAuth = Base64.getEncoder().encodeToString(rawAuth.getBytes(StandardCharsets.UTF_8));
        connection.setRequestProperty("Authorization", "Basic " + encodedAuth);

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
        } else if (StringUtils.startsWith(sbString, "[")) {
            JSONArray jsonArray = new JSONArray(sbString);
            URL newUrl = new URL(this.remoteAssetsConfig.getServer().concat(jsonArray.get(0).toString()));
            return getJsonObjectFromUri(newUrl);
        } else {
            if (LOG.isErrorEnabled()) {
                LOG.error("Unable to grab JSON object. Please ensure URL {} is valid. \nRaw Response: {}", url.toString(), sbString);
            }
            return new JSONObject();
        }
    }

    /**
     * Create or update nodes from remote JSON.
     * @param json JSONObject
     * @param keys Iterator
     * @param parentNode Node
     * @throws JSONException exception
     * @throws RepositoryException exception
     */
    private void createOrUpdateNodesFromJson(final JSONObject json, final Iterator keys, final Node parentNode)
            throws JSONException, RepositoryException {

        while (keys.hasNext()) {
            Object key = keys.next();
            if (json.get((String) key) instanceof JSONObject) {
                Node childNode = JcrUtils.getNodeIfExists(parentNode.getPath().concat((String) key), this.session);
                if (childNode == null) {
                    childNode = JcrUtil.createPath(parentNode.getPath()
                            .concat("/")
                            .concat((String) key), JcrConstants.NT_UNSTRUCTURED, this.session);
//                    LOG.info("Node {} created!!", childNode.getPath());
                }

                JSONObject objectJson = json.getJSONObject((String) key);
                Iterator objectKeys = objectJson.keys();
                createOrUpdateNodesFromJson(objectJson, objectKeys, childNode);
            } else if (json.get((String) key) instanceof JSONArray) {
                continue;
            } else {
                try {
                    if (parentNode.hasProperty((String) key) && parentNode.getProperty((String) key).getString().equals(json.get((String) key))) {
                        continue;
                    }

                    if (isPropertyProtected((String) key)) {
                        continue;
                    }

                    if (JcrConstants.JCR_PRIMARYTYPE.equals(key)) {
                        parentNode.setPrimaryType((String) json.get((String) key));
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

//                    LOG.info("Property {} set on Node {}!!", key, parentNode.getPath());
                } catch (RepositoryException re) {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn("Repository exception thrown. Skipping {} property.", key, re);
                    }
                }
            }
        }
    }

    /**
     * Determine whether the provided property is a protected property.
     * @param key String
     */
    private boolean isPropertyProtected(final String key) {
        String[] potectedProperties = new String[] {
                JcrConstants.JCR_CREATED, JcrConstants.JCR_CREATED_BY, JcrConstants.JCR_VERSIONHISTORY, JcrConstants.JCR_BASEVERSION,
                JcrConstants.JCR_ISCHECKEDOUT, JcrConstants.JCR_UUID
        };

        for (String property: potectedProperties) {
            if (property.equals(key)) {
                return true;
            }
        }

        return false;
    }
}
