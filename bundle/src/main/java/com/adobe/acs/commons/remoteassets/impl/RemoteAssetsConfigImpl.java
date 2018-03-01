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
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;

@Component(immediate = true, metatype = true,
        label = "ACS AEM Commons - Remote Assets - Config",
        policy = ConfigurationPolicy.REQUIRE)

@Service(RemoteAssetsConfig.class)
public class RemoteAssetsConfigImpl implements RemoteAssetsConfig {
    private String server = "";
    private String username = "";
    private String password = "";
    private Integer retryDelay;
    private String eventUserData = "";

    @Property(label = "Server")
    private static final String SERVER = "server";

    @Property(label = "Username")
    private static final String USERNAME = "username";

    @Property(label = "Password")
    private static final String PASSWORD = "password";

    @Property(
            label = "Asset sync paths",
            description = "Paths to sync assets from the remote server (e.g. /content/dam)",
            cardinality = Integer.MAX_VALUE,
            value = {}
    )
    private static final String SYNC_PATHS = "paths";

    @Property(
            label = "Failure retry delay (in minutes)",
            description = "Number of minutes the server will wait to attempt to sync a remote asset that failed a sync attempt (minimum 1)",
            intValue = 15
    )
    private static final String RETRY_DELAY = "retryDelay";

    @Property(
            label = "Event User Data",
            description = "The event user data that will be set during all JCR manipulations performed by remote assets. This can be used in workflow launchers that listen to DAM paths (such as for DAM Update Assets) to exclude unnecessary processing such as rendition generation.",
            value = "changedByWorkflowProcess")
    private static final String EVENT_USER_DATA = "event-user-data";

    private List<String> syncPaths = new ArrayList<>();

    @Override
    public String getServer() {
        return server;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public List<String> getSyncPaths() {
        return syncPaths;
    }

    @Override
    public Integer getRetryDelay() {
        return retryDelay;
    }

    @Override
    public String getEventUserData() {
        return eventUserData;
    }

    @Activate
    @Modified
    private void activate(final ComponentContext componentContext) {
        final Dictionary<?, ?> properties = componentContext.getProperties();

        server = PropertiesUtil.toString(properties.get(SERVER), "");
        if (StringUtils.isBlank(server)) {
            throw new IllegalArgumentException("Remote server must be specified");
        }

        username = PropertiesUtil.toString(properties.get(USERNAME), "");
        if (StringUtils.isBlank(username)) {
            throw new IllegalArgumentException("Remote server username must be specified");
        }

        password = PropertiesUtil.toString(properties.get(PASSWORD), "");
        if (StringUtils.isBlank(password)) {
            throw new IllegalArgumentException("Remote server password must be specified");
        }

        syncPaths = Arrays.asList(PropertiesUtil.toStringArray(properties.get(SYNC_PATHS), new String[0]));
        if (syncPaths.size() == 0) {
            throw new IllegalArgumentException("At least one sync path must be specified");
        }

        retryDelay = PropertiesUtil.toInteger(properties.get(RETRY_DELAY), 1);
        if (retryDelay < 1) {
            retryDelay = 1;
        }

        eventUserData = PropertiesUtil.toString(properties.get(EVENT_USER_DATA), "");
    }

    @Deactivate
    private void deactivate() {
    }

}