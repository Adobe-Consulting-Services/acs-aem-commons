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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.jcr.Session;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.fluent.Executor;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration service for Remote Asset feature.
 */
@Component(
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        service = RemoteAssetsConfigImpl.class
)
@Designate(ocd=RemoteAssetsConfigImpl.Config.class)
public class RemoteAssetsConfigImpl {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteAssetsConfigImpl.class);

    @ObjectClassDefinition(name = "ACS AEM Commons - Remote Assets - Config")
    public @interface Config {
        boolean DEFAULT_ALLOW_INSECURE = false;
        String DEFAULT_EVENT_USER_DATA = "changedByWorkflowProcess";
        int DEFAULT_RETRY_DELAY = 15;
        int DEFAULT_SAVE_INTERVAL = 100;

        @AttributeDefinition(
                name = "Server",
                description = "URL to remote server from which to fetch assets (e.g. https://dev-aem-author.client.com:4502)"
        )
        String server_url() default StringUtils.EMPTY;

        @AttributeDefinition(
                name = "Username",
                description = "User to log into the remote server"
        )
        String server_user() default StringUtils.EMPTY;

        @AttributeDefinition(
                name = "Password",
                description = "Password to log into the remote server"
        )
        String server_pass() default StringUtils.EMPTY;

        @AttributeDefinition(
                name = "Allow Insecure Connection",
                description = "Allow non-https connection to remote assets server, "
                        + "allowing potential compromise of connection credentials"
        )
        boolean server_insecure() default DEFAULT_ALLOW_INSECURE;

        @AttributeDefinition(
                name = "Tag Sync Paths",
                description = "Paths to sync tags from the remote server (e.g. /content/cq:tags/client)",
                cardinality = Integer.MAX_VALUE
        )
        String[] tag_paths() default {};

        @AttributeDefinition(
                name = "Asset Sync Paths",
                description = "Paths to sync assets from the remote server (e.g. /content/dam)",
                cardinality = Integer.MAX_VALUE
        )
        String[] dam_paths() default {};

        @AttributeDefinition(
                name = "Failure Retry Delay (in minutes)",
                description = "Number of minutes the server will wait to attempt to sync a remote"
                        + "asset that failed a sync attempt (minimum 1)"
        )
        int retry_delay() default DEFAULT_RETRY_DELAY;

        @AttributeDefinition(
                name = "Number of Assets to Sync Before Saving",
                description = "Number of asset nodes to sync before saving and refreshing the session during a node "
                        + "sync. The lower the number, the longer the sync will take (default 100)"
        )
        int save_interval() default DEFAULT_SAVE_INTERVAL;

        @AttributeDefinition(
                name = "Event User Data",
                description = "The event user data that will be set during all JCR manipulations performed by "
                        + "remote assets. This can be used in workflow launchers that listen to DAM paths (such as "
                        + "for DAM Update Assets) to exclude unnecessary processing such as rendition generation."
        )
        String event_user_data() default DEFAULT_EVENT_USER_DATA;

        @AttributeDefinition(
                name = "Whitelisted Service Users",
                description = "Service users that are allowed to trigger remote asset binary syncs. By default, service "
                        + "user activity never triggers an asset binary sync.",
                cardinality = Integer.MAX_VALUE
        )
        String[] whitelisted_service_users() default {};
    }

    private String server = StringUtils.EMPTY;
    private String username = StringUtils.EMPTY;
    private String password = StringUtils.EMPTY;
    private boolean allowInsecureRemote = false;
    private List<String> tagSyncPaths = new ArrayList<>();
    private List<String> damSyncPaths = new ArrayList<>();
    private Integer retryDelay;
    private Integer saveInterval;
    private String eventUserData = StringUtils.EMPTY;
    private Set<String> whitelistedServiceUsers = new HashSet<>();

    private Executor remoteAssetsHttpExecutor;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    /**
     * Method to run on activation.
     */
    @Activate
    protected final void activate(RemoteAssetsConfigImpl.Config config) {
        this.server = config.server_url();
        if (StringUtils.isBlank(this.server)) {
            throw new IllegalArgumentException("Remote server must be specified");
        }
        this.username = config.server_user();
        if (StringUtils.isBlank(this.username)) {
            throw new IllegalArgumentException("Remote server username must be specified");
        }
        this.password = config.server_pass();
        if (StringUtils.isBlank(this.password)) {
            throw new IllegalArgumentException("Remote server password must be specified");
        }
        this.allowInsecureRemote = config.server_insecure();
        this.tagSyncPaths = Stream.of(ObjectUtils.defaultIfNull(config.tag_paths(), new String[]{}))
                .filter(item -> StringUtils.isNotBlank(item))
                .collect(Collectors.toList());
        this.damSyncPaths = Stream.of(ObjectUtils.defaultIfNull(config.dam_paths(), new String[]{}))
                .filter(item -> StringUtils.isNotBlank(item))
                .collect(Collectors.toList());
        this.retryDelay = config.retry_delay();
        this.saveInterval = config.save_interval();
        this.eventUserData = config.event_user_data();
        this.whitelistedServiceUsers = Stream.of(ObjectUtils.defaultIfNull(config.whitelisted_service_users(), new String[]{}))
                .filter(item -> StringUtils.isNotBlank(item))
                .collect(Collectors.toSet());

        buildRemoteHttpExecutor();
    }

    /**
     * @return String
     */
    public String getServer() {
        return this.server;
    }

    /**
     * @return String
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * @return String
     */
    public String getPassword() {
        return this.password;
    }

    /**
     * @return List<String>
     */
    public List<String> getTagSyncPaths() {
        return this.tagSyncPaths;
    }

    /**
     * @return List<String>
     */
    public List<String> getDamSyncPaths() {
        return this.damSyncPaths;
    }

    /**
     * @return Integer
     */
    public Integer getRetryDelay() {
        return this.retryDelay;
    }

    /**
     * @return Integer
     */
    public Integer getSaveInterval() {
        return this.saveInterval;
    }

    /**
     * @return String
     */
    public String getEventUserData() {
        return this.eventUserData;
    }

    /**
     * @return String
     */
    public Set<String> getWhitelistedServiceUsers() {
        return this.whitelistedServiceUsers;
    }

    /**
     * @return Executor
     */
    public Executor getRemoteAssetsHttpExecutor() {
        return remoteAssetsHttpExecutor;
    }

    /**
     */
    public ResourceResolver getResourceResolver() {
        try {
            Map<String, Object> userParams = new HashMap<>();
            userParams.put(ResourceResolverFactory.SUBSERVICE, RemoteAssets.SERVICE_NAME);
            ResourceResolver resourceResolver = this.resourceResolverFactory.getServiceResourceResolver(userParams);
            Session session = resourceResolver.adaptTo(Session.class);
            if (StringUtils.isNotBlank(this.getEventUserData())) {
                session.getWorkspace().getObservationManager().setUserData(this.getEventUserData());
            }
            return resourceResolver;
        } catch (Exception e) {
            LOG.error("Remote assets functionality cannot be enabled - service user login failed");
            throw new RemoteAssetsServiceException(e);
        }
    }

    private void buildRemoteHttpExecutor() {
        URL url;
        try {
            url = new URL(this.server);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Remote server address is malformed");
        }

        if (!url.getProtocol().equalsIgnoreCase("https")) {
            if (this.allowInsecureRemote) {
                LOG.warn("Remote Assets connection is not HTTPS - authentication username and password will be"
                        + " communicated in CLEAR TEXT.  This configuration is NOT recommended, as it may allow"
                        + " credentials to be compromised!");
            } else {
                throw new IllegalArgumentException("Remote server address must be HTTPS so that credentials"
                        + " cannot be compromised.  As an alternative, you may configure remote assets to allow"
                        + " use of a non-HTTPS connection, allowing connection credentials to potentially be"
                        + " compromised AT YOUR OWN RISK.");
            }
        }

        HttpHost host = new HttpHost(url.getHost(), url.getPort(), url.getProtocol());
        this.remoteAssetsHttpExecutor = Executor.newInstance()
                .auth(host, username, password)
                .authPreemptive(host);
    }
}