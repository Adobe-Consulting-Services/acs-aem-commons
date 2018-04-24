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

import com.adobe.acs.commons.remoteassets.RemoteAssetsBinarySync;
import com.adobe.acs.commons.remoteassets.RemoteAssetsConfig;
import com.adobe.granite.asset.api.RenditionHandler;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.dam.api.Rendition;
import com.day.cq.dam.commons.util.DamUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.jackrabbit.value.DateValue;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

/**
 * Service to sync a remote asset's binaries a from remote server. Implements {@link RemoteAssetsBinarySync}.
 */
@Component(
    label = "ACS AEM Commons - Remote Assets - Binary Sync Service",
    description = "Pulls the binaries for a remote asset in order to make it a true local asset."
)
@Service
public class RemoteAssetsBinarySyncImpl implements RemoteAssetsBinarySync {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteAssetsBinarySyncImpl.class);

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private RemoteAssetsConfig remoteAssetsConfig;

    private ResourceResolver resourceResolver;
    private Session session;

    /**
     * Method to run on activation.
     * @throws RepositoryException exception
     */
    @Activate
    protected void activate() throws RepositoryException {
        this.resourceResolver = RemoteAssets.logIn(this.resourceResolverFactory);
        this.session = this.resourceResolver.adaptTo(Session.class);
        if (StringUtils.isNotBlank(this.remoteAssetsConfig.getEventUserData())) {
            this.session.getWorkspace().getObservationManager().setUserData(this.remoteAssetsConfig.getEventUserData());
        }
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
     * @see RemoteAssetsBinarySync#syncAsset(Resource)
     * @param resource Resource
     * @return Resource
     */
    @Override
    public Resource syncAsset(Resource resource) {
        try {
            this.session.refresh(true);
            Resource localRes = this.resourceResolver.getResource(resource.getPath());
            Node node = localRes.adaptTo(Node.class);

            Asset asset = DamUtil.resolveToAsset(localRes);
            URI pathUri = new URI(null, null, asset.getPath(), null);
            String baseUrl = this.remoteAssetsConfig.getServer().concat(pathUri.toString()).concat("/_jcr_content/renditions/");

            Iterator<? extends Rendition> renditions = asset.listRenditions();
            while (renditions.hasNext()) {
                Rendition assetRendition = renditions.next();
                if (StringUtils.isEmpty(assetRendition.getMimeType())) {
                    continue;
                }
                String renditionName = assetRendition.getName();
                String remoteUrl = String.format("%s%s", baseUrl, renditionName);
                setRenditionOnAsset(remoteUrl, assetRendition, asset, renditionName);
            }

            node.setProperty(RemoteAssets.IS_REMOTE_ASSET, (Value)null);
            node.setProperty(RemoteAssets.REMOTE_SYNC_FAILED, (Value)null);
            this.session.save();
            return localRes;
        } catch (Exception e) {
            LOG.error("Error transferring remote asset '{}' to local server", resource.getPath(), e);
            try {
                this.session.refresh(false);
            } catch (RepositoryException re) {
                LOG.error("Failed to rollback asset changes", re);
            }
            return flagAssetAsFailedSync(resource);
        }
    }

    /**
     * Fetch binary from URL and set into the asset rendition.
     * @param remoteUrl String
     * @param assetRendition Rendition
     * @param asset Asset
     * @param renditionName String
     * @throws FileNotFoundException exception
     */
    private void setRenditionOnAsset(String remoteUrl, Rendition assetRendition, Asset asset, String renditionName)
            throws IOException {

        LOG.debug("Syncing from remote asset url {}", remoteUrl);
        Executor executor = this.remoteAssetsConfig.getRemoteAssetsHttpExecutor();
        try (InputStream inputStream = executor.execute(Request.Get(remoteUrl)).returnContent().asStream()) {
            Map<String, Object> props = new HashMap<>();
            props.put(RenditionHandler.PROPERTY_RENDITION_MIME_TYPE, assetRendition.getMimeType());
            asset.addRendition(renditionName, inputStream, props);
        } catch (HttpResponseException fne) {
            if (DamConstants.ORIGINAL_FILE.equals(renditionName) || fne.getStatusCode() != HTTP_NOT_FOUND) {
                throw fne;
            }

            asset.removeRendition(renditionName);
            LOG.warn("Rendition '{}' not found on remote environment. Removing local rendition.", renditionName);
        }
    }

    /**
     * Sets a property on the resource if the asset sync failed.
     * @param resource Resource
     * @return Resource
     */
    private Resource flagAssetAsFailedSync(Resource resource) {
        try {
            Resource localRes = this.resourceResolver.getResource(resource.getPath());
            Node node = localRes.adaptTo(Node.class);
            node.setProperty(RemoteAssets.REMOTE_SYNC_FAILED, new DateValue(new GregorianCalendar()));
            this.session.save();
            return localRes;
        } catch (Exception e) {
            LOG.error("Error flagging remote asset '{}' as failed - asset may attempt to sync numerous times in succession", resource.getPath(), e);
            try {
                this.session.refresh(false);
            } catch (RepositoryException re) {
                LOG.error("Failed to rollback asset changes", re);
            }
        }
        return resource;
    }
}
