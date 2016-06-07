/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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
import org.apache.http.client.HttpClient;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.sling.commons.osgi.PropertiesUtil;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;


@Component(
        label = "Http Components Fluent Executor Factory",
        description = "Http Components Fluent Executor Factory", immediate = false,
        configurationFactory = true, policy = ConfigurationPolicy.REQUIRE)
@Service
@Property(label = "Factory Name", description = "Name of this factory", name = "factory.name")
public class HttpClientFactoryImpl implements HttpClientFactory {
    public static final boolean DEFAULT_USE_SSL = false;
    public static final boolean DEFAULT_DISABLE_CERT_CHECK = false;

    @Property(label = "host name", description = "host name")
    private static final String PROP_HOST_DOMAIN = "hostname";


    @Property(label = "port", description = "port")
    private static final String PROP_GATEWAY_PORT = "port";

    @Property(label = "Use SSL", description = "Select it if only using https connection for calls.", boolValue = DEFAULT_USE_SSL)
    private static final String PROP_USE_SSL = "use.ssl";

    @Property(label = "Disable certificate check", description = "If selected it will disable certificate check for the SSL connection.",
            boolValue = DEFAULT_DISABLE_CERT_CHECK)
    private static final String PROP_DISABLE_CERT_CHECK = "disable.certificate.check";

    @Property(label = "username", description = "username")
    private static final String PROP_USERNAME = "username";


    @Property(label = "password", description = "password")
    private static final String PROP_PASSWORD = "password";

    private Executor executor;
    private String baseUrl;

    @Activate
    protected void activate(Map<String, Object> config) throws Exception {
        boolean useSSL = PropertiesUtil.toBoolean(config.get(PROP_USE_SSL), DEFAULT_USE_SSL);
        boolean disableCertCheck = PropertiesUtil.toBoolean(config.get(PROP_DISABLE_CERT_CHECK), DEFAULT_DISABLE_CERT_CHECK);

        String scheme = useSSL ? "https" : "http";
        String hostname = PropertiesUtil.toString(config.get(PROP_HOST_DOMAIN), null);
        int port = PropertiesUtil.toInteger(config.get(PROP_GATEWAY_PORT), 0);

        if (hostname == null || port == 0) {
            throw new IllegalArgumentException("Configuration not valid. Both host and port must be provided.");
        }

        baseUrl = String.format("%s://%s:%s", scheme, hostname, port);

        if (useSSL && disableCertCheck) {
            HttpClient client = HttpClients.custom().
                    setHostnameVerifier(new AllowAllHostnameVerifier()).
                    setSslcontext(new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
                        public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                            return true;
                        }
                    }).build()).build();
            executor = Executor.newInstance();
        } else {
            executor = Executor.newInstance();
        }

        String username = PropertiesUtil.toString(config.get(PROP_USERNAME), null);
        String password = PropertiesUtil.toString(config.get(PROP_PASSWORD), null);
        if (username != null && password != null) {
            HttpHost httpHost = new HttpHost(hostname, port);
            executor.auth(httpHost, username, password).authPreemptive(httpHost);
        }
    }

    @Override
    public Executor getExecutor() {
        return executor;
    }

    @Override
    public Request get(String partialUrl) {
        String url = baseUrl + partialUrl;
        return Request.Get(url);
    }

    @Override
    public Request post(String partialUrl) {
        String url = baseUrl + partialUrl;
        return Request.Post(url);
    }

    @Override
    public Request put(String partialUrl) {
        String url = baseUrl + partialUrl;
        return Request.Put(url);
    }

    @Override
    public Request delete(String partialUrl) {
        String url = baseUrl + partialUrl;
        return Request.Delete(url);
    }

    @Override
    public Request options(String partialUrl) {
        String url = baseUrl + partialUrl;
        return Request.Options(url);
    }

}
