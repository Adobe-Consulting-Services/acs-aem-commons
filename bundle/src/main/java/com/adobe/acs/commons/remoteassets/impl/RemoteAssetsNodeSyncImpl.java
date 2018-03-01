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
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
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
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Iterator;

@Component(
        immediate = true,
        metatype = true,
        label = "ACS AEM Commons - Remote Assets - Node Sync Service"
)
@Service
public class RemoteAssetsNodeSyncImpl implements RemoteAssetsNodeSync {

    private static final Logger log = LoggerFactory.getLogger(RemoteAssetsNodeSyncImpl.class);

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private RemoteAssetsConfig remoteAssetsConfig;

    private ResourceResolver resourceResolver;
    private Session session;

    @Activate
    protected void activate() throws RepositoryException, org.apache.sling.api.resource.LoginException {
        resourceResolver = RemoteAssets.logIn(resourceResolverFactory);
        session = resourceResolver.adaptTo(Session.class);
        session.getWorkspace().getObservationManager().setUserData(remoteAssetsConfig.getEventUserData());
    }

    @Deactivate
    protected void deactivate() {
        if (session != null) {
            try {
                session.logout();
            } catch (Exception e) {
                log.warn("Failed session.logout()", e);
            }
        }
        if (resourceResolver != null) {
            try {
                resourceResolver.close();
            } catch (Exception e) {
                log.warn("Failed resourceResolver.close()", e);
            }
        }
    }

    @Override
    public void syncAssets() {
        log.info("Asset Sync Service..");
        try {
            URL url = new URL(remoteAssetsConfig.getServer() + remoteAssetsConfig.getSyncPaths().get(0) + ".infinity.json");
            log.info("URL {}", url);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            String encoded = Base64.getEncoder().encodeToString((remoteAssetsConfig.getUsername() + ":" + remoteAssetsConfig.getPassword()).getBytes(StandardCharsets.UTF_8));
            connection.setRequestProperty("Authorization", "Basic " + encoded);
            InputStream is = connection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (connection.getInputStream()))); // Getting the response from the webservice

            StringBuilder sb = new StringBuilder();

            String line;
            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                sb.append(line);
                //log.info("line : "+line);
            }
            JSONObject json = new JSONObject(sb.toString());
            Iterator keys = json.keys();
            try {
                Node contentNode = JcrUtils.getOrCreateByPath(remoteAssetsConfig.getSyncPaths().get(0), "sling:OrderedFolder", session);
                while (keys.hasNext()) {
                    Object key = keys.next();
                    if (json.get((String) key) instanceof JSONObject) {
                        Node childNode = contentNode.addNode((String) key);
                        log.info("JSON Object for {}", (String) key);
                    } else {
                        //contentNode.setProperty((String)key, (String)json.get((String)key));
                        log.info("Not JSON Object for {}", (String) key);
                    }
                }
                session.save();

            } catch (RepositoryException e) {
                log.error("Repository Exception {}", e);
            }

            log.info("JSON {}", json);
        } catch (ProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


}
