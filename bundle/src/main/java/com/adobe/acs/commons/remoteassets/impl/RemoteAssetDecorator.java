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
import com.day.cq.dam.api.DamConstants;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.oak.spi.security.user.UserConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceDecorator;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import java.util.Calendar;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * ResourceDecorator that instruments remote assets to sync binaries as needed.
 * This "decorator" is used to detect the first time a "remote" asset is
 * referenced by the system and sync that asset from the remote server to
 * make it now a "true" asset.
 */
@Component(
        label = "ACS AEM Commons - Remote Assets - Asset Resource Decorator",
        description = "Captures a request for a remote asset so that the binary can be sync'd to the current server making it a true local asset",
        policy = ConfigurationPolicy.REQUIRE
)
@Service
public class RemoteAssetDecorator implements ResourceDecorator {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteAssetDecorator.class);
    private static int SYNC_WAIT_SECONDS = 100;

    /**
     * This set stores resource paths for remote assets that are in the process
     * of being sync'd from the remote server.  This prevents an infinite loop
     * when the RemoteAssetSync service fetches the asset in order to update it.
     */
    private static Set<String> remoteResourcesSyncing = new ConcurrentSkipListSet<>();

    @Reference
    private RemoteAssetsBinarySync assetSync;

    @Reference
    private RemoteAssetsConfig config;

    /**
     * When resolving a remote asset, first sync the asset from the remote server.
     * @param resource The resource being resolved.
     * @return The current resource.  If the resource is a "remote" asset, it will
     * first be converted to a true local AEM asset by sync'ing in the rendition
     * binaries from the remote server.
     */
    @Override
    public Resource decorate(final Resource resource) {
        try {
            if (!this.accepts(resource)) {
                return resource;
            }
        } catch (Exception e) {
            // Logging at debug level b/c if this happens it could represent a ton of logging
            LOG.debug("Failed binary sync check for remote asset: {} - {}", resource.getPath(), e);
            return resource;
        }

        if (remoteResourcesSyncing.contains(resource.getPath())) {
            return waitForSyncInProgress(resource);
        } else {
            return syncAssetBinaries(resource);
        }
    }

    /**
     * @deprecated
     * When resolving a remote asset, first sync the asset from the remote server.
     * @param resource The resource being resolved.
     * @param request HttpServletRequest
     * @return The current resource.  If the resource is a "remote" asset, it will
     * first be converted to a true local AEM asset by sync'ing in the rendition
     * binaries from the remote server.
     */
    @Deprecated
    @Override
    public Resource decorate(final Resource resource, final HttpServletRequest request) {
        return this.decorate(resource);
    }

    /**
     * Check if this resource is a remote resource.
     * @param resource Resource to check
     * @return true if resource is remote, else false
     */
    private boolean accepts(final Resource resource) throws RepositoryException {
        if (resource == null) {
            return false;
        }

        ValueMap props = resource.getValueMap();
        if (!DamConstants.NT_DAM_ASSETCONTENT.equals(props.get(JcrConstants.JCR_PRIMARYTYPE))) {
            return false;
        }

        if (!props.get(RemoteAssets.IS_REMOTE_ASSET, false)) {
            return false;
        }

        Calendar lastFailure = props.get(RemoteAssets.REMOTE_SYNC_FAILED, (Calendar) null);
        if (lastFailure != null && System.currentTimeMillis() < (lastFailure.getTimeInMillis() + (this.config.getRetryDelay() * 60000))) {
            return false;
        }

        boolean matchesSyncPath = false;
        for (String syncPath : this.config.getDamSyncPaths()) {
            if (resource.getPath().startsWith(syncPath)) {
                matchesSyncPath = true;
            }
        }

        if (matchesSyncPath) {
            Session session = resource.getResourceResolver().adaptTo(Session.class);
            String userId = session.getUserID();
            if (!userId.equals(UserConstants.DEFAULT_ADMIN_ID)) {
                if (this.config.getWhitelistedServiceUsers().contains(userId)) {
                    return true;
                }

                User currentUser = (User) AccessControlUtil.getUserManager(session).getAuthorizable(userId);
                if (currentUser != null && !currentUser.isSystemUser()) {
                    return true;
                } else {
                    LOG.debug("Avoiding binary sync b/c this is a non-whitelisted service user: {}", session.getUserID());
                }
            } else {
                LOG.debug("Avoiding binary sync for admin user");
            }
        }

        return false;
    }

    private Resource waitForSyncInProgress(Resource resource) {
        String resourcePath = resource.getPath();

        LOG.debug("Already sync'ing {} - waiting for parallel sync to complete", resourcePath);
        try {
            // Wait for asset already sync'ing
            long originalTime = System.currentTimeMillis();
            while (remoteResourcesSyncing.contains(resourcePath) && (System.currentTimeMillis() - originalTime) < (1000 * SYNC_WAIT_SECONDS)) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
            }
            if (remoteResourcesSyncing.contains(resourcePath)) {
                LOG.warn("Waited {} seconds for parallel binary sync to complete for: {} - giving up", SYNC_WAIT_SECONDS, resourcePath);
            } else {
                LOG.debug("Parallel sync of {} complete, re-fetching resource", resourcePath);
                resource.getResourceResolver().refresh();
                return resource.getResourceResolver().getResource(resourcePath);
            }
        } catch (Exception e) {
            LOG.error("Failed to wait for parallel binary sync for remote asset: {} - {}", resourcePath, e);
        }
        return resource;
    }

    private Resource syncAssetBinaries(Resource resource) {
        String resourcePath = resource.getPath();
        try {
            remoteResourcesSyncing.add(resourcePath);
            LOG.info("Sync'ing remote asset binaries: {}", resourcePath);
            return this.assetSync.syncAsset(resource);
        } catch (Exception e) {
            LOG.error("Failed to sync binaries for remote asset: {} - {}", resourcePath, e);
        } finally {
            remoteResourcesSyncing.remove(resourcePath);
        }
        return resource;
    }
}
