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

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.Optional;

import org.apache.http.osgi.services.ProxyConfiguration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * This service implements an {@link Authenticator} for proxies configured 
 * via existing {@link ProxyConfiguration} services from Apache HTTP Client, instead of providing a dedicated configuration.
 */
@Component(service = Authenticator.class,
        property = {"source=Apache Http Components Proxy Configuration"})
public class ProxyAuthenticatorImpl extends Authenticator {

    private final ProxyConfigurationSelector proxyConfigurationSelector;

    @Activate()
    public ProxyAuthenticatorImpl(@Reference ProxyConfigurationSelector proxyConfigurationSelector) {
        this.proxyConfigurationSelector = proxyConfigurationSelector;
    }

    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
        if (getRequestorType() != RequestorType.PROXY) {
            Optional<ProxyConfiguration> configuration = proxyConfigurationSelector.findConfigurationForProxy(getRequestingHost(), getRequestingPort());
            return configuration.map(c-> new PasswordAuthentication(c.getUsername(), c.getPassword().toCharArray())).orElse(null);
        }
        return null;
    }

}
