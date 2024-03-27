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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.function.Consumer;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.http.OsgiManagedJavaHttpClient;

@Component
@Designate(factory = true, ocd = OsgiManagedJavaHttpClientImpl.Config.class)
public class OsgiManagedJavaHttpClientImpl implements OsgiManagedJavaHttpClient {

    @ObjectClassDefinition(
            name="ACS AEM Commons - OSGi Managed Java Http Clients",
            description = "Allows to manage multiple Java clients with basic settings"
            )
    @interface Config {
        @AttributeDefinition(name = "Client Name", description = "Unique name of this client (used to reference a particular config in code)")
        String name();

        @AttributeDefinition(name = "Base URL", description = "The absolute base URL including protocol and optionally the port. Considered for relative URLs.")
        String baseUrl();

        @AttributeDefinition(name = "Disable certificate path check", description = "If selected it will disable certificate path check for the TLS connection, i.e. the certificate doesn't need to be issued by a trusted CA (only relevant for protocol HTTPS).")
        boolean disableCertificatePathCheck() default false;

        @AttributeDefinition(name = "Use trust store from AEM", description = "If selected it will use the global trust store from AEM in addition to the standard JRE one, otherwise only the default one from the JRE is used (only relevant if disableCertificatePathCheck is not set to true).")
        boolean useAemTrustStore() default false;

        @AttributeDefinition(name = "User name", description = "User name for requests (using basic authentication)", required=false)
        String userName();

        @AttributeDefinition(name = "Password", description = "Password for requests (using basic authentication)", type = AttributeType.PASSWORD, required=false)
        String password() default "";

        @AttributeDefinition(name = "User ID and Alias For Client Certificate", description = "User ID and alias to use for client certificate authentication. The certificate is extracted from the given user's keystore leveraging the given alias. Must have the format <UserId>:<Alias>")
        String userIdAndAliasForClientCertificate();

        @AttributeDefinition(name = "Request Timeout", description = "Request timeout in milliseconds")
        int requestTimeout() default 30000;

        @AttributeDefinition(name = "Connect Timeout", description = "Connect timeout in milliseconds")
        int connectTimeout() default 30000;
        
        // Internal Name hint for web console.
        String webconsole_configurationFactory_nameHint() default "Name: {name}";
    }

    private static final Logger LOG = LoggerFactory.getLogger(OsgiManagedJavaHttpClientImpl.class);
    
    private final Config config;
    private final HttpClient.Builder builder;
    private final URI baseUrl;
    private HttpClient client;

    @Activate
    public OsgiManagedJavaHttpClientImpl(Config config, @Reference ProxySelector proxySelector, 
            @Reference Authenticator proxyAuthenticator, @Reference AemKeyStoreFactory aemKeyStoreFactory) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, FileNotFoundException, CertificateException, IOException, org.apache.sling.api.resource.LoginException, UnrecoverableKeyException {
        this.config = config;
        this.builder = HttpClient.newBuilder();
        this.builder.proxy(proxySelector);
        this.builder.authenticator(new Authenticator() {
            @Override
            public PasswordAuthentication requestPasswordAuthenticationInstance(String host, InetAddress addr, int port, String protocol,
                    String prompt, String scheme, URL url, RequestorType reqType) {
                if (reqType == RequestorType.PROXY) {
                    return proxyAuthenticator.requestPasswordAuthenticationInstance(host, addr, port, protocol, prompt, scheme, url, reqType);
                } else {
                    return super.requestPasswordAuthenticationInstance(host, addr, port, protocol, prompt, scheme, url, reqType);
                }
            }

            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                
                // basic authentication credentials
                if (config.userName() != null && !config.userName().isEmpty()) {
                    int expectedPort = baseUrl.getPort();
                    if (expectedPort <= 0) {
                        try {
                            expectedPort = baseUrl.toURL().getDefaultPort();
                        } catch (MalformedURLException e) {
                            throw new IllegalArgumentException("Given baseUrl " + baseUrl + " is no valid URL", e);
                        }
                    }
                    if (getRequestingHost().equalsIgnoreCase(baseUrl.getHost()) && getRequestingPort() == expectedPort) {
                        return new PasswordAuthentication(config.userName(), config.password().toCharArray());
                    } else {
                        LOG.warn("Don't use credentials from configuration as request targets another host {} and/or port {}", getRequestingHost(), getRequestingPort());
                    }
                }
                return null;
            }
        });
        final SSLContext sslContext = SSLContext.getInstance("TLS");
        // configure trust manager
        final TrustManager trustManager;
        if (config.disableCertificatePathCheck()) {
            trustManager = TrustManagerUtils.createAlwaysTrusted();
        } else if (config.useAemTrustStore()) {
            trustManager = TrustManagerUtils.createComposition(aemKeyStoreFactory.getTrustManager());
        } else {
            trustManager = null;
        }
        final KeyManager keyManager;
        // configure key manager (for client cert authentication)
        if (config.userIdAndAliasForClientCertificate() != null && !config.userIdAndAliasForClientCertificate().isBlank()) {
            String[] parts = config.userIdAndAliasForClientCertificate().split(":", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Configuration parameter 'userIdAndAliasForClientCertificate' must have one separator ':' but is " + config.userIdAndAliasForClientCertificate());
            }
            String userId = parts[0];
            String alias = parts[1];
            KeyStore keyStore = aemKeyStoreFactory.getKeyStore(userId);
            
            keyManager = KeyManagerUtils.createSingleClientSideCertificateKeyManager(keyStore, aemKeyStoreFactory.getKeyStorePassword(userId), alias);
        } else {
            keyManager = null;
        }
        TrustManager[] tms = trustManager != null ? new TrustManager[]{ trustManager } : null;
        KeyManager[] kms = keyManager != null ? new KeyManager[]{ keyManager } : null;
        sslContext.init(kms, tms, null);
        this.builder.sslContext(sslContext);
        this.builder.connectTimeout(Duration.ofMillis(config.connectTimeout()));
        baseUrl = URI.create(config.baseUrl());
    }

    @Deactivate()
    public void deactivate() {
        if (client != null) {
            try {
                // call httpClient.close() with JRE21
                Method method = HttpClient.class.getMethod("close");
                method.invoke(client);
            } catch (NoSuchMethodException e) {
                LOG.debug("Couldn't found close() method on HTTP Client, probably JRE < 21", e);
            } catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                LOG.warn("Could not close HTTP client", e);
            }
        }
    }

    @Override
    public HttpClient getClient() {
        return getClient(null);
    }

    @Override
    public HttpClient getClient(Consumer<HttpClient.Builder> builderCustomizer) {
        if (builderCustomizer != null) {
            if (client == null) {
                builderCustomizer.accept(builder);
                client = builder.build();
            } else {
                throw new IllegalStateException("The underlying HTTP client has already been created through a call of getClient(...) and can no longer be customized");
            }
        }
        return client;
    }

    @Override
    public HttpRequest createRequest(URI uri) {
        return createRequest(uri, null);
    }

    @Override
    public HttpRequest createRequest(URI uri, Consumer<HttpRequest.Builder> requestBuilderCustomizer) {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(baseUrl.resolve(uri)) // combine with base
                .timeout(Duration.ofMillis(config.requestTimeout()));
        if (requestBuilderCustomizer != null) {
            requestBuilderCustomizer.accept(requestBuilder);
        }
        return requestBuilder.build();
    }

}
