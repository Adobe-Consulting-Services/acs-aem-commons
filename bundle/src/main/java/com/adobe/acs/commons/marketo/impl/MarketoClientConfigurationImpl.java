/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.marketo.impl;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.adobe.acs.commons.marketo.MarketoClientConfiguration;

/**
 * A Model retrieving the configuration for interacting with the Marketo REST
 * API
 */
@Model(adaptables = Resource.class, adapters = MarketoClientConfiguration.class)
public class MarketoClientConfigurationImpl implements MarketoClientConfiguration {

    private final String clientId;

    private final String clientSecret;

    private final String endpointHost;

    private final String munchkinId;

    private final String serverInstance;

    @Inject
    public MarketoClientConfigurationImpl(@ValueMapValue @Named("clientId") String clientId,
            @ValueMapValue @Named("clientSecret") String clientSecret,
            @ValueMapValue @Named("endpointHost") String endpointHost,
            @ValueMapValue @Named("munchkinId") String munchkinId,
            @ValueMapValue @Named("serverInstance") String serverInstance) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.endpointHost = endpointHost;
        this.munchkinId = munchkinId;
        this.serverInstance = serverInstance;
    }

    private String getHost(String url) {
        int start = url.indexOf("://");
        if (url.startsWith("//")) { // handle the special case of starting with //
            start = 2;
        } else if (start < 0) { // no protocol
            start = 0;
        } else { // has protocol
            start += 3;
        }
        int end = url.indexOf('/', start);
        if (end < 0) {
            end = url.length();
        }
        String domainName = url.substring(start, end);

        // handle port
        int port = domainName.indexOf(':');
        if (port >= 0) {
            domainName = domainName.substring(0, port);
        }
        return domainName;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        MarketoClientConfigurationImpl other = (MarketoClientConfigurationImpl) obj;
        if (clientId == null) {
            if (other.clientId != null) {
                return false;
            }
        } else if (!clientId.equals(other.clientId)) {
            return false;
        }
        return true;
    }

    @Override
    public String getClientId() {
        return clientId;
    }

    @Override
    public String getClientSecret() {
        return clientSecret;
    }

    @Override
    public String getEndpointHost() {
        return getHost(endpointHost);
    }

    @Override
    public String getMunchkinId() {
        return munchkinId;
    }

    @Override
    public String getServerInstance() {
        return getHost(serverInstance);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((clientId == null) ? 0 : clientId.hashCode());
        return result;
    }

}
