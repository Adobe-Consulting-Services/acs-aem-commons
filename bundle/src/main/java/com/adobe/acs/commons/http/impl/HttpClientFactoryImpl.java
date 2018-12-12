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
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import java.io.IOException;
import java.util.Map;

@Component(configurationPolicy = ConfigurationPolicy.REQUIRE,
        service = HttpClientFactory.class)
@Designate(ocd=HttpClientFactoryImpl.Config.class,factory=true)
public class HttpClientFactoryImpl implements HttpClientFactory {

    public static final boolean DEFAULT_USE_SSL = false;

    public static final boolean DEFAULT_DISABLE_CERT_CHECK = false;

    public static final int DEFAULT_CONNECT_TIMEOUT = 30000;

    public static final int DEFAULT_SOCKET_TIMEOUT = 30000;
    
    @ObjectClassDefinition(name = "ACS AEM Commons - Http Components Fluent Executor Factory",
        description = "ACS AEM Commons - Http Components Fluent Executor Factory")
    public @interface Config {
    	
    	@AttributeDefinition(name = "Factory Name", description = "Name of this factory")
    String factory_name();
    	
        @AttributeDefinition(name = "host name", description = "host name")
        String hostname();

        @AttributeDefinition(name = "port", description = "port")
        int port();

        @AttributeDefinition(name = "Use SSL", description = "Select it if only using https connection for calls.", defaultValue = ""+DEFAULT_USE_SSL)
        boolean use_ssl();

        @AttributeDefinition(name = "Disable certificate check", description = "If selected it will disable certificate check for the SSL connection.",
                defaultValue = ""+DEFAULT_DISABLE_CERT_CHECK)
        boolean disable_certificate_check();

        @AttributeDefinition(name = "Username", description = "Username for requests (using basic authentication)")
        String username();

        @AttributeDefinition(name = "Password", description = "Password for requests (using basic authentication)")
        @SuppressWarnings("squid:S2068")
        String password();

        @AttributeDefinition(name = "Socket Timeout", description = "Socket timeout in milliseconds", defaultValue = ""+DEFAULT_SOCKET_TIMEOUT)
        int so_timeout();

        @AttributeDefinition(name = "Connect Timeout", description = "Connect timeout in milliseconds", defaultValue = ""+DEFAULT_CONNECT_TIMEOUT)
        int conn_timeout();
    	
    }

    private static final String PROP_HOST_DOMAIN = "hostname";

    private static final String PROP_GATEWAY_PORT = "port";

    private static final String PROP_USE_SSL = "use.ssl";

    private static final String PROP_DISABLE_CERT_CHECK = "disable.certificate.check";

    private static final String PROP_USERNAME = "username";

    @SuppressWarnings("squid:S2068")
    private static final String PROP_PASSWORD = "password";

    private static final String PROP_SO_TIMEOUT = "so.timeout";

    private static final String PROP_CONNECT_TIMEOUT = "conn.timeout";

    @Reference
    private HttpClientBuilderFactory httpClientBuilderFactory;

    private Executor executor;
    private String baseUrl;
    private CloseableHttpClient httpClient;

    @Activate
    protected void activate(Map<String, Object> config) throws Exception {
        boolean useSSL = PropertiesUtil.toBoolean(config.get(PROP_USE_SSL), DEFAULT_USE_SSL);

        String scheme = useSSL ? "https" : "http";
        String hostname = PropertiesUtil.toString(config.get(PROP_HOST_DOMAIN), null);
        int port = PropertiesUtil.toInteger(config.get(PROP_GATEWAY_PORT), 0);

        if (hostname == null || port == 0) {
            throw new IllegalArgumentException("Configuration not valid. Both host and port must be provided.");
        }

        baseUrl = String.format("%s://%s:%s", scheme, hostname, port);

        int connectTimeout = PropertiesUtil.toInteger(config.get(PROP_CONNECT_TIMEOUT), DEFAULT_CONNECT_TIMEOUT);
        int soTimeout = PropertiesUtil.toInteger(config.get(PROP_SO_TIMEOUT), DEFAULT_SOCKET_TIMEOUT);

        HttpClientBuilder builder = httpClientBuilderFactory.newBuilder();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(connectTimeout)
                .setSocketTimeout(soTimeout)
                .build();
        builder.setDefaultRequestConfig(requestConfig);

        boolean disableCertCheck = PropertiesUtil.toBoolean(config.get(PROP_DISABLE_CERT_CHECK), DEFAULT_DISABLE_CERT_CHECK);

        if (useSSL && disableCertCheck) {
            // Disable hostname verification and allow self-signed certificates
            SSLContextBuilder sslbuilder = new SSLContextBuilder();
            sslbuilder.loadTrustMaterial(new TrustSelfSignedStrategy());
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                    sslbuilder.build(), NoopHostnameVerifier.INSTANCE);
            builder.setSSLSocketFactory(sslsf);
        }
        httpClient = builder.build();
        executor = Executor.newInstance(httpClient);

        String username = PropertiesUtil.toString(config.get(PROP_USERNAME), null);
        String password = PropertiesUtil.toString(config.get(PROP_PASSWORD), null);
        if (username != null && password != null) {
            HttpHost httpHost = new HttpHost(hostname, port, useSSL ? "https" : "http");
            executor.auth(httpHost, username, password).authPreemptive(httpHost);
        }
    }

    @Deactivate
    protected void deactivate() {
        if (httpClient != null) {
            try {
                httpClient.close();
            } catch (final IOException e) {
                // do nothing
            }
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
