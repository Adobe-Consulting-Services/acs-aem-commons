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

import java.util.Calendar;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.oak.spi.security.user.UserConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceDecorator;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.remoteassets.RemoteAssetsBinarySync;
import com.adobe.acs.commons.remoteassets.RemoteAssetsConfig;
import com.day.cq.dam.api.DamConstants;

/**
 * ResourceDecorator that instruments remote assets to sync binaries as needed.
 *
 * This "decorator" is used to detect the first time a "remote" asset is
 * referenced by the system and sync that asset from the remote server to
 * make it now a "true" asset.
 */
@Component(
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        service = ResourceDecorator.class
)
public class RemoteAssetDecorator implements ResourceDecorator {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteAssetDecorator.class);

    private static int SYNC_WAIT_SECONDS = 100;

    /**
     * This set stores resource paths for remote assets that are in the process
     * of being sync'd from the remote server.  This prevents an infinite loop
     * when the RemoteAssetSync service fetches the asset in order to update it.
     */
    private static final Set<String> remoteResourcesSyncing = new ConcurrentSkipListSet<>();

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
        } catch (final RepositoryException e) {
            // Logging at debug level b/c if this happens it could represent a ton of logging
            LOG.debug("Failed binary sync check for remote asset: {}", resource.getPath());
            return resource;
        }

        boolean syncSuccessful = false;
        if (isAlreadySyncing(resource.getPath())) {
            syncSuccessful = waitForSyncInProgress(resource);
        } else {
            syncSuccessful = syncAssetBinaries(resource);
        }

        if (syncSuccessful) {
            LOG.trace("Refreshing resource after binary sync of {}", resource.getPath());
            final ResourceResolver resourceResolver = resource.getResourceResolver();
            resourceResolver.refresh();
            return resourceResolver.getResource(resource.getPath());
        } else {
            return resource;
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
    protected boolean accepts(final Resource resource) throws RepositoryException {
        final ValueMap props = resource.getValueMap();
        if (!DamConstants.NT_DAM_ASSETCONTENT.equals(props.get(JcrConstants.JCR_PRIMARYTYPE))) {
            return false;
        }

        if (!props.get(RemoteAssets.IS_REMOTE_ASSET, false)) {
            return false;
        }

        final Calendar lastFailure = props.get(RemoteAssets.REMOTE_SYNC_FAILED, (Calendar) null);
        if (lastFailure != null && System.currentTimeMillis() < (lastFailure.getTimeInMillis() + (config.getRetryDelay() * 60000))) {
            return false;
        }

        final boolean isAllowedUser = isAllowedUser(resource);
        if (!isAllowedUser) {
            return false;
        }

        final String path = resource.getPath();
        for (final String syncPath : config.getDamSyncPaths()) {
            if (path.startsWith(syncPath)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if the user is allowed to sync binaries.
     *
     * Service users, as well as the admin user, are prevented from sync'ing
     * binaries to ensure that some back end procress traversing the DAM doesn't
     * trigger a sync of the entire DAM, thus subverting the benefits of
     * remote assets.
     *
     * Service users can be whitelisted via remote aseets configuration if it
     * is desired for a particular service user to be able to sync binaries.
     *
     * @param resource The asset content Resource to sync binaries for.
     * @return True if user is allowed to sync binaries, else false.
     * @throws RepositoryException
     */
    private boolean isAllowedUser(final Resource resource) throws RepositoryException {
        final ResourceResolver resourceResolver = resource.getResourceResolver();
        final String userId = resourceResolver.getUserID();
        if (userId.equals(UserConstants.DEFAULT_ADMIN_ID)) {
            LOG.trace("Avoiding binary sync for admin user");
        } else {
            if (config.getWhitelistedServiceUsers().contains(userId)) {
                return true;
            }

            final Session session = resourceResolver.adaptTo(Session.class);
            final User currentUser = (User) getUserManager(session).getAuthorizable(userId);
            if (currentUser != null && !currentUser.isSystemUser()) {
                return true;
            } else {
                LOG.trace("Avoiding binary sync b/c this is a non-whitelisted service user: {}", session.getUserID());
            }
        }

        return false;
    }

    protected UserManager getUserManager(final Session session) throws RepositoryException {
        return AccessControlUtil.getUserManager(session);
    }

    protected boolean isAlreadySyncing(final String resourcePath) {
        return remoteResourcesSyncing.contains(resourcePath);
    }

    private boolean waitForSyncInProgress(final Resource resource) {
        final String resourcePath = resource.getPath();
        LOG.debug("Already sync'ing {} - waiting for parallel sync to complete", resourcePath);
        try {
            // Wait for asset already sync'ing
            long originalTime = System.currentTimeMillis();
            while (isAlreadySyncing(resourcePath) && (System.currentTimeMillis() - originalTime) < (1000 * SYNC_WAIT_SECONDS)) {
                Thread.sleep(100);
            }

            if (isAlreadySyncing(resourcePath)) {
                LOG.warn("Waited {} seconds for parallel binary sync to complete for: {} - giving up", SYNC_WAIT_SECONDS, resourcePath);
            } else {
                LOG.debug("Parallel sync of {} complete", resourcePath);
                return true;
            }
        } catch (final Exception e) {
            LOG.error("Failed to wait for parallel binary sync for remote asset: {}", resourcePath, e);
        }

        return false;
    }

    private boolean syncAssetBinaries(final Resource resource) {
        final String resourcePath = resource.getPath();
        try {
            remoteResourcesSyncing.add(resourcePath);
            LOG.info("Sync'ing remote asset binaries: {}", resourcePath);
            if (assetSync.syncAsset(resource)) {
                LOG.debug("Sync of remote asset binaries for {} complete", resourcePath);
                return true;
            } else {
                LOG.error("Failed to sync binaries for remote asset: {}", resourcePath);
            }
        } catch (final Exception e) {
            LOG.error("Failed to sync binaries for remote asset: {}", resourcePath, e);
        } finally {
            remoteResourcesSyncing.remove(resourcePath);
        }

        return false;
    }

}
