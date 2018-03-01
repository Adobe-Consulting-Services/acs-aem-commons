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

import com.adobe.acs.commons.remoteassets.RemoteAssetsConfig;
import com.adobe.acs.commons.remoteassets.RemoteAssetsBinarySync;
import com.adobe.granite.asset.api.Asset;
import com.adobe.granite.asset.api.Rendition;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.value.DateValue;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @see com.adobe.acs.commons.remoteassets.RemoteAssetsBinarySync
 */
@Component(
    label = "ACS AEM Commons - Remote Assets - Binary Sync Service",
    description = "Pulls the binaries for a remote asset in order to make it a true local asset."
)
@Service
public class RemoteAssetsBinarySyncImpl implements RemoteAssetsBinarySync {
    private final Logger log = LoggerFactory.getLogger(RemoteAssetsBinarySyncImpl.class);

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private RemoteAssetsConfig remoteAssetsConfig;

    private ResourceResolver resourceResolver;
    private Session session;

    @Activate
    protected void activate() throws RepositoryException, LoginException {
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
    public Resource syncAsset(Resource resource) {
        try {
            session.refresh(true);
            Resource localRes = resourceResolver.getResource(resource.getPath());
            Node node = localRes.adaptTo(Node.class);

            Asset asset = localRes.getParent().adaptTo(Asset.class);
            String baseUrl = remoteAssetsConfig.getServer() + asset.getPath().replace(" ", "%20") + "/_jcr_content/renditions/";

            Iterator<? extends Rendition> renditions = asset.listRenditions();
            while (renditions.hasNext()) {
                Rendition assetRendition = renditions.next();
                String renditionName = assetRendition.getName();

                URL url = new URL(String.format("%s%s", baseUrl, renditionName));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                String authName = remoteAssetsConfig.getUsername();
                String authPassword = remoteAssetsConfig.getPassword();
                String encoded = Base64.getEncoder().encodeToString((String.format("%s:%s", authName, authPassword).getBytes(StandardCharsets.UTF_8)));
                connection.setRequestProperty("Authorization", "Basic "+encoded);

                log.info("syncing from remote asset url {}", url);
                InputStream inputStream = connection.getInputStream();

                Map<String, Object> props = new HashMap<>();
                props.put("rendition.mime", assetRendition.getMimeType());
                asset.setRendition(renditionName, inputStream, props);

                inputStream.close();
            }

            node.setProperty("isRemoteAsset", (Value)null);
            node.setProperty("remoteSyncFailed", (Value)null);
            session.save();
            return localRes;
        } catch (Exception e) {
            log.error("Error transfering remote asset '{}' to local server", resource.getPath(), e);
            try {
                session.refresh(false);
            } catch (RepositoryException re) {
                log.error("Failed to rollback asset changes", re);
            }
            return flagAssetAsFailedSync(resource);
        }
    }

    private Resource flagAssetAsFailedSync(Resource resource) {
        try {
            Resource localRes = resourceResolver.getResource(resource.getPath());
            Node node = localRes.adaptTo(Node.class);
            node.setProperty("remoteSyncFailed", new DateValue(new GregorianCalendar()));
            session.save();
            return localRes;
        } catch (Exception e) {
            log.error("Error flagging remote asset '{}' as failed - asset may attempt to sync numerous times in succession", resource.getPath(), e);
            try {
                session.refresh(false);
            } catch (RepositoryException re) {
                log.error("Failed to rollback asset changes", re);
            }
        }
        return resource;
    }

}
