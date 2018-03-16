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

import com.adobe.acs.commons.remoteassets.RemoteAssetsConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.component.ComponentContext;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Configuration service for Remote Asset feature. Implements {@link RemoteAssetsConfig}.
 */
@Component(
        immediate = true,
        metatype = true,
        label = "ACS AEM Commons - Remote Assets - Config",
        policy = ConfigurationPolicy.REQUIRE
)
@Service()
public class RemoteAssetsConfigImpl implements RemoteAssetsConfig {

    @Property(label = "Server")
    private static final String SERVER = "server";

    @Property(label = "Username")
    private static final String USERNAME = "username";

    @Property(label = "Password")
    private static final String PASSWORD = "password";

    @Property(
            label = "Tag Sync Paths",
            description = "Paths to sync tags from the remote server (e.g. /etc/tags/asset)",
            cardinality = Integer.MAX_VALUE,
            value = {}
    )
    private static final String TAG_SYNC_PATHS = "tag.paths";

    @Property(
            label = "Asset Sync Paths",
            description = "Paths to sync assets from the remote server (e.g. /content/dam)",
            cardinality = Integer.MAX_VALUE,
            value = {}
    )
    private static final String DAM_SYNC_PATHS = "dam.paths";

    @Property(
            label = "Failure Retry Delay (in minutes)",
            description = "Number of minutes the server will wait to attempt to sync a remote asset that failed a sync attempt (minimum 1)",
            intValue = 15
    )
    private static final String RETRY_DELAY = "retry.delay";

    @Property(
            label = "Number of Assets to Sync Before Saving",
            description = "Number of asset nodes to sync before saving and refreshing the session during a node sync. The lower the number, " +
                    "the longer the sync will take (default 100)",
            intValue = 100
    )
    private static final String SAVE_INTERVAL = "save.interval";

    @Property(
            label = "Event User Data",
            description = "The event user data that will be set during all JCR manipulations performed by remote assets. This can be used in " +
                    "workflow launchers that listen to DAM paths (such as for DAM Update Assets) to exclude unnecessary processing such as " +
                    "rendition generation.",
            value = "changedByWorkflowProcess")
    private static final String EVENT_USER_DATA = "event.user.data";

    @Property(
            label = "Whitelisted Service Users",
            description = "Service users that are allowed to trigger remote asset binary syncs.  By defualt, service user activity never triggers an asset binary sync.",
            cardinality = Integer.MAX_VALUE,
            value = {}
    )
    private static final String WHITELISTED_SERVICE_USERS = "whitelisted.service.users";

    private String server = StringUtils.EMPTY;
    private String username = StringUtils.EMPTY;
    private String password = StringUtils.EMPTY;
    private List<String> tagSyncPaths = new ArrayList<>();
    private List<String> damSyncPaths = new ArrayList<>();
    private Integer retryDelay;
    private Integer saveInterval;
    private String eventUserData = StringUtils.EMPTY;
    private Set<String> whitelistedServiceUsers = new HashSet<>();

    /**
     * Method to run on activation.
     * @param componentContext ComponentContext
     */
    @Activate
    @Modified
    private void activate(final ComponentContext componentContext) {
        final Dictionary<String, Object> properties = componentContext.getProperties();

        this.server = PropertiesUtil.toString(properties.get(SERVER), StringUtils.EMPTY);
        if (StringUtils.isBlank(this.server)) {
            throw new IllegalArgumentException("Remote server must be specified");
        }
        this.username = PropertiesUtil.toString(properties.get(USERNAME), StringUtils.EMPTY);
        if (StringUtils.isBlank(this.username)) {
            throw new IllegalArgumentException("Remote server username must be specified");
        }
        this.password = PropertiesUtil.toString(properties.get(PASSWORD), StringUtils.EMPTY);
        if (StringUtils.isBlank(this.password)) {
            throw new IllegalArgumentException("Remote server password must be specified");
        }
        this.tagSyncPaths = Stream.of(PropertiesUtil.toStringArray(properties.get(TAG_SYNC_PATHS), new String[0]))
                .filter(item -> StringUtils.isNotBlank(item))
                .collect(Collectors.toList());
        this.damSyncPaths = Stream.of(PropertiesUtil.toStringArray(properties.get(DAM_SYNC_PATHS), new String[0]))
                .filter(item -> StringUtils.isNotBlank(item))
                .collect(Collectors.toList());
        this.retryDelay = PropertiesUtil.toInteger(properties.get(RETRY_DELAY), 1);
        this.saveInterval = PropertiesUtil.toInteger(properties.get(SAVE_INTERVAL), 100);
        this.eventUserData = PropertiesUtil.toString(properties.get(EVENT_USER_DATA), StringUtils.EMPTY);
        this.whitelistedServiceUsers = Stream.of(PropertiesUtil.toStringArray(properties.get(WHITELISTED_SERVICE_USERS), new String[0]))
                .filter(item -> StringUtils.isNotBlank(item))
                .collect(Collectors.toSet());
    }

    /**
     * Method to run on deactivation.
     */
    @Deactivate
    private void deactivate() {
        // Do nothing.
    }

    /**
     * @see RemoteAssetsConfig#getServer()
     * @return String
     */
    @Override
    public String getServer() {
        return this.server;
    }

    /**
     * @see RemoteAssetsConfig#getUsername()
     * @return String
     */
    @Override
    public String getUsername() {
        return this.username;
    }

    /**
     * @see RemoteAssetsConfig#getPassword()
     * @return String
     */
    @Override
    public String getPassword() {
        return this.password;
    }

    /**
     * @see RemoteAssetsConfig#getTagSyncPaths()
     * @return List<String>
     */
    @Override
    public List<String> getTagSyncPaths() {
        return this.tagSyncPaths;
    }

    /**
     * @see RemoteAssetsConfig#getDamSyncPaths()
     * @return List<String>
     */
    @Override
    public List<String> getDamSyncPaths() {
        return this.damSyncPaths;
    }

    /**
     * @see RemoteAssetsConfig#getRetryDelay()
     * @return Integer
     */
    @Override
    public Integer getRetryDelay() {
        return this.retryDelay;
    }

    /**
     * @see RemoteAssetsConfig#getSaveInterval()
     * @return Integer
     */
    @Override
    public Integer getSaveInterval() {
        return this.saveInterval;
    }

    /**
     * @see RemoteAssetsConfig#getEventUserData()
     * @return String
     */
    @Override
    public String getEventUserData() {
        return this.eventUserData;
    }

    /**
     * @see RemoteAssetsConfig#getWhitelistedServiceUsers()
     * @return String
     */
    @Override
    public Set<String> getWhitelistedServiceUsers() {
        return this.whitelistedServiceUsers;
    }
}