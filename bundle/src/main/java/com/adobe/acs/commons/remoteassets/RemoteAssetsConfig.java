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
package com.adobe.acs.commons.remoteassets;

import org.apache.http.client.fluent.Executor;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.List;
import java.util.Set;

/**
 * Configuration for Remote Assets.
 */
public interface RemoteAssetsConfig {

    /**
     * Server from which to pull the assets from.
     */
    String getServer();

    /**
     * Username to log into the remote assets server.
     */
    String getUsername();

    /**
     * Password to log into the remote assets server.
     */
    String getPassword();

    /**
     * Tag paths to sync from the remote assets server.
     */
    List<String> getTagSyncPaths();

    /**
     * Asset paths to sync from the remote assets server.
     */
    List<String> getDamSyncPaths();

    /**
     * Number of minutes the server will wait to attempt to sync a remote asset that failed a sync attempt.
     */
    Integer getRetryDelay();

    /**
     * Number of asset nodes to sync before saving and refreshing the session during the remote asset node sync.
     */
    Integer getSaveInterval();

    /**
     * The event user data that will be set during all JCR manipulations performed by remote assets.
     * This can be used in workflow launchers that listen to DAM paths (such as for DAM Update Assets)
     * to exclude unnecessary processing such as rendition generation.
     */
    String getEventUserData();

    /**
     * Service users that are allowed to trigger remote asset binary syncs.
     * By default, service user activity never triggers an asset binary sync.
     */
    Set<String> getWhitelistedServiceUsers();

    /**
     * Get an HTTP Executor for making calls to the remote AEM Assets server.
     */
    Executor getRemoteAssetsHttpExecutor();

    /**
     * Get a resourceResolver for the remote assets service user.
     */
    ResourceResolver getResourceResolver();

    /**
     * Close resourceResolver for the remote assets service user, along with associated session.
     */
    void closeResourceResolver(ResourceResolver resourceResolver);
}