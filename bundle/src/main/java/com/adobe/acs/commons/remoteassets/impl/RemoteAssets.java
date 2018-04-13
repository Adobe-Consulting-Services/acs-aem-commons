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
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Common functionality for Remote Assets.
 */
public class RemoteAssets {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteAssets.class);
    public static final String SERVICE_NAME = "remote-assets";

    /**
     * Private constructor.
     */
    private RemoteAssets() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Retrieve a {@link ResourceResolver} after logging in.
     * @param resourceResolverFactory ResourceResolverFactory
     * @return ResourceResolver
     */
    public static ResourceResolver logIn(ResourceResolverFactory resourceResolverFactory) {
        try {
            Map<String, Object> userParams = new HashMap<>();
            userParams.put(ResourceResolverFactory.SUBSERVICE, SERVICE_NAME);
            return resourceResolverFactory.getServiceResourceResolver(userParams);
        } catch (LoginException le2) {
            LOG.error("Remote assets functionality cannot be enabled - service user login failed");
            throw new RemoteAssetsServiceException(le2);
        }
    }

    /**
     * Retrieve the Base64 encoded authentication string.
     * @param config RemoteAssetsConfig
     * @return String
     */
    public static String encodeForBasicAuth(final RemoteAssetsConfig config) {
        String rawAuth = String.format("%s:%s", config.getUsername(), config.getPassword());
        return Base64.getEncoder().encodeToString(rawAuth.getBytes(StandardCharsets.UTF_8));
    }
}
