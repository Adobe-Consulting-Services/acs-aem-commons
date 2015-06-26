/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2015 Adobe
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
package com.adobe.acs.commons.http.impl;

import com.adobe.acs.commons.http.HttpClientFactory;
import org.apache.felix.scr.annotations.*;
import org.apache.http.HttpHost;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.sling.commons.osgi.PropertiesUtil;

import java.util.Map;


@Component(
        label = "Http Components Fluent Executor Factory",
        description = "Http Components Fluent Executor Factory", immediate =
        false,
        metatype = false, configurationFactory = true, policy =
        ConfigurationPolicy.REQUIRE)
@Service
public class HttpClientFactoryImpl implements HttpClientFactory {
    @Property(label = "gateway domain", description = "gateway domain", value
            = "127.0.0.1")
    private static final String GATEWAY_DOMAIN = "gateway.domain";


    @Property(label = "gateway port", description = "port of the gateway",
            intValue =
                    8050)
    private static final String GATEWAY_PORT = "gateway.port";

    @Property(label = "Use SSL", description = "Select it if only using https" +
            " connection for calls."
            , boolValue = false)
    private static final String USE_SSL = "use.ssl";

    @Property(label = "Disable certificate check", description = "If selected" +
            " it will disable certificate check for the SSL connection",
            boolValue = false)
    private static final String DISABLE_CERTIFICATE_CHECK = "disable" +
            ".certificate";

    @Property(label = "username", description = "user name", value = "admin")
    private static final String USERNAME = "sap.username";


    @Property(label = "password", description = "password", value = "admin")
    private static final String PASSWORD = "password";

    private Executor executor;
    private boolean useSSL;
    private String scheme;
    private String gatewayDomain;
    private int gatewayPort;
    private String baseDomain;

    @Activate
    @Modified
    protected void activate(Map<String, Object> config) {
        useSSL = PropertiesUtil.toBoolean(config.get(USE_SSL), false);
        scheme = useSSL ? "https" : "http";
        gatewayDomain = PropertiesUtil
                .toString(config.get(GATEWAY_DOMAIN), "");
        gatewayPort = PropertiesUtil.toInteger(config.get(GATEWAY_PORT), 8050);
        baseDomain = scheme + "://" + gatewayDomain + ":" + gatewayPort;
        synchronized (executor) {
            executor = Executor.newInstance().auth(new HttpHost(gatewayDomain,
                            gatewayPort),
                    PropertiesUtil.toString(config.get(USERNAME), ""),
                    PropertiesUtil
                            .toString(config.get(PASSWORD), ""));
        }
    }

    @Override
    public Executor getExecutor() {
        return executor;
    }

    @Override
    public Request get(String uri) {
        String url = baseDomain + uri;
        return Request.Get(url);
    }

    @Override
    public Request post(String uri) {
        String url = baseDomain + uri;
        return Request.Post(url);
    }

    @Override
    public Request put(String uri) {
        String url = baseDomain + uri;
        return Request.Put(url);
    }

    @Override
    public Request delete(String uri) {
        String url = baseDomain + uri;
        return Request.Delete(url);
    }

    @Override
    public Request options(String uri) {
        String url = baseDomain + uri;
        return Request.Options(url);
    }

}
