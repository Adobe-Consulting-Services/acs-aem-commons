/*
 * ACS AEM Commons
 *
 * Copyright (C) 2024 Konrad Windszus
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
package com.adobe.acs.commons.http.impl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.http.osgi.services.ProxyConfiguration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.adobe.acs.commons.http.impl.ProxyConfigurationSelector.NetworkAddress;

/**
 * This service is configured via existing {@link ProxyConfiguration} services from Apache HTTP Client, instead of providing a dedicated configuration.
 * 
 */
@Component(service = ProxySelector.class,
        property = {"source=Apache Http Components Proxy Configuration"})
public class ProxySelectorImpl extends ProxySelector {

    private final ProxyConfigurationSelector proxyConfigurationSelector;

    @Activate
    public ProxySelectorImpl(@Reference ProxyConfigurationSelector proxyConfigurationSelector) {
        this.proxyConfigurationSelector = proxyConfigurationSelector;
    }

    @Override
    public List<Proxy> select(URI uri) {
        return proxyConfigurationSelector.findApplicableConfiguration(uri).stream().map(c -> new Proxy(Type.HTTP, getInetSocketAddress(c))).collect(Collectors.toList());
    }

    private InetSocketAddress getInetSocketAddress(ProxyConfiguration configuration) {
        // TODO: Support IPv6, https://issues.apache.org/jira/browse/HTTPCLIENT-2320
        final NetworkAddress na =ProxyConfigurationSelector.NetworkAddress.parse(configuration.getHostname());
        if (na != null) {
            return new InetSocketAddress(na.getInetAddress(), configuration.getPort());
        } else {
            return new InetSocketAddress(configuration.getHostname(), configuration.getPort());
        }
    }

    @Override
    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        // not considered for now
    }

}
