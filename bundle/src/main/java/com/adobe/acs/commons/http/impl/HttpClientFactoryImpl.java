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
package com.adobe.acs.commons.http.impl;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.function.Consumer;

import org.apache.http.HttpHost;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
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
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.adobe.acs.commons.http.HttpClientFactory;

@Component()
public class HttpClientFactoryImpl implements HttpClientFactory {

    @ObjectClassDefinition(factoryPid = "com.adobe.acs.commons.http.impl.HttpClientFactoryImpl",
            name="ACS AEM Commons - Http Components Fluent Executor Factory",
            description = "ACS AEM Commons - Http Components Fluent Executor Factory"
            )
    @interface Config {
        @AttributeDefinition(name = "Factory Name", description = "Name of this factory")
        String factory_name();

        @AttributeDefinition(name = "host name", description="Mandatory")
        String hostname();

        @AttributeDefinition(name = "port", description="Mandatory")
        int port();

        @AttributeDefinition(name = "Use SSL", description = "Select it if only using https connection for calls.")
        boolean use_ssl() default false;

        @AttributeDefinition(name = "Disable certificate ceck", description = "If selected it will disable certificate check for the SSL connection.")
        boolean disable_certificate_check() default false;

        @AttributeDefinition(name = "Username", description = "Username for requests (using basic authentication)")
        String username();

        @AttributeDefinition(name = "Password", description = "Password for requests (using basic authentication)", type = AttributeType.PASSWORD)
        @SuppressWarnings("squid:S2068")
        String password();

        @AttributeDefinition(name = "Socket Timeout", description = "Socket timeout in milliseconds")
        int so_timeout() default 30000;

        @AttributeDefinition(name = "Connect Timeout", description = "Connect timeout in milliseconds")
        int conn_timeout() default 30000;
        
        // Internal Name hint for web console.
        String webconsole_configurationFactory_nameHint() default "Factory Name: {factory.name}";
    }
    
    private final HttpClientBuilder builder;
    private final HttpHost httpHost;
    private final Credentials credentials;
    private final String baseUrl;

    private Executor executor;
    private CloseableHttpClient httpClient;

    @Activate
    public HttpClientFactoryImpl(HttpClientFactoryImpl.Config config, @Reference HttpClientBuilderFactory httpClientBuilderFactory) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        this(config.use_ssl(), config.hostname(), config.port(), config.so_timeout(),
                config.conn_timeout(), config.disable_certificate_check(), config.username(), config.password(),
                httpClientBuilderFactory);
    }

    HttpClientFactoryImpl(boolean useSSL, String hostname, int port, int soTimeout, 
            int connectTimeout, boolean disableCertCheck, String username, String password,
            HttpClientBuilderFactory httpClientBuilderFactory) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        String scheme = useSSL ? "https" : "http";

        if (hostname == null || port == 0) {
            throw new IllegalArgumentException("Configuration not valid. Both host and port must be provided.");
        }

        baseUrl = String.format("%s://%s:%s", scheme, hostname, port);

        builder = httpClientBuilderFactory.newBuilder();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(connectTimeout)
                .setSocketTimeout(soTimeout)
                .build();
        builder.setDefaultRequestConfig(requestConfig);


        if (useSSL && disableCertCheck) {
            // Disable hostname verification and allow self-signed certificates
            SSLContextBuilder sslbuilder = new SSLContextBuilder();
            sslbuilder.loadTrustMaterial(new TrustSelfSignedStrategy());
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                    sslbuilder.build(), NoopHostnameVerifier.INSTANCE);
            builder.setSSLSocketFactory(sslsf);
        }
        httpHost = new HttpHost(hostname, port, useSSL ? "https" : "http");
        if (username != null && password != null) {
            credentials = new UsernamePasswordCredentials(username, password);
        } else {
            credentials = null;
        }
    }

    synchronized Executor createExecutor() {
        httpClient = builder.build();
        executor = Executor.newInstance(httpClient);

        if (credentials != null) {
            executor.auth(httpHost, credentials).authPreemptive(httpHost);
        }
        return executor;
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
    public void customize(Consumer<HttpClientBuilder> builderCustomizer) {
        if (httpClient != null) {
            throw new IllegalStateException("The underlying http client has already been created through a call of getExecutor() and can no longer be customized");
        }
        builderCustomizer.accept(builder);
    }

    @Override
    public Executor getExecutor() {
        if (executor == null) {
            executor = createExecutor();
        }
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
