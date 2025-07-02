/*-
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2022 Adobe
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
package com.adobe.acs.commons.contentsync;

import com.adobe.granite.auth.oauth.AccessTokenProvider;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingScriptHelper;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.adobe.acs.commons.contentsync.ConfigurationUtils.CONNECT_TIMEOUT_KEY;
import static com.adobe.acs.commons.contentsync.ConfigurationUtils.DISABLE_CERT_CHECK_KEY;
import static com.adobe.acs.commons.contentsync.ConfigurationUtils.SO_TIMEOUT_STRATEGY_KEY;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;

/**
 * HTTP connection to a remote AEM instance + some sugar methods to fetch data
 */
public class RemoteInstance implements Closeable {
    static final int CONNECT_TIMEOUT = 5000;
    static final int SOCKET_TIMEOUT = 300000;

    private final CloseableHttpClient httpClient;
    private final SyncHostConfiguration hostConfiguration;
    private AccessTokenProvider tokenProvider;

    public RemoteInstance(SyncHostConfiguration hostConfiguration, ValueMap generalSettings) throws GeneralSecurityException, IOException {
        this.hostConfiguration = hostConfiguration;
        this.httpClient = createHttpClient(hostConfiguration, generalSettings, null);
    }
    public RemoteInstance(SyncHostConfiguration hostConfiguration, ValueMap generalSettings, SlingScriptHelper sling, ResourceResolver resourceResolver) throws GeneralSecurityException, IOException {
        if(hostConfiguration.isOAuthEnabled()){
            String providerName = hostConfiguration.getAccessTokenProviderName();
            AccessTokenProvider[] tokenProviders = sling.getServices(AccessTokenProvider.class, "(name="+providerName+")");
            if(tokenProviders != null && tokenProviders.length > 0) tokenProvider = tokenProviders[0];
        }
        this.hostConfiguration = hostConfiguration;
        this.httpClient = createHttpClient(hostConfiguration, generalSettings,  resourceResolver);
    }

    private CloseableHttpClient createHttpClient(SyncHostConfiguration hostConfiguration, ValueMap generalSettings, ResourceResolver resourceResolver)
            throws GeneralSecurityException, IOException {
        BasicCredentialsProvider provider = new BasicCredentialsProvider();
        HttpClientBuilder builder = HttpClients.custom();
        if (hostConfiguration.isOAuthEnabled()) {
            // If OAuth is enabled, use the AccessTokenProvider to get the token
            String accessToken = tokenProvider.getAccessToken(resourceResolver, resourceResolver.getUserID(), null);
            builder.addInterceptorFirst((HttpRequestInterceptor) (request, context) -> {
                request.addHeader("Authorization", "Bearer " + accessToken);
            });
        } else {
            // If username and password are set, use them for basic authentication
            provider.setCredentials(
                    AuthScope.ANY,
                    new UsernamePasswordCredentials(hostConfiguration.getUsername(), hostConfiguration.getPassword()));
        }
        provider.setCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials(hostConfiguration.getUsername(), hostConfiguration.getPassword()));
        int soTimeout = generalSettings.get(SO_TIMEOUT_STRATEGY_KEY, SOCKET_TIMEOUT);
        int connTimeout = generalSettings.get(CONNECT_TIMEOUT_KEY, CONNECT_TIMEOUT);
        boolean disableCertCheck = generalSettings.get(DISABLE_CERT_CHECK_KEY, false);
        RequestConfig requestConfig = RequestConfig
                .custom()
                .setConnectTimeout(connTimeout)
                .setSocketTimeout(soTimeout)
                .setCookieSpec(CookieSpecs.STANDARD).build();
        builder.setDefaultRequestConfig(requestConfig)
                .setDefaultCredentialsProvider(provider);
        if (disableCertCheck) {
            // Disable hostname verification and allow self-signed certificates
            SSLContextBuilder sslbuilder = new SSLContextBuilder();
            sslbuilder.loadTrustMaterial(new TrustSelfSignedStrategy());
            SSLConnectionSocketFactory sslsf = null;
            sslsf = new SSLConnectionSocketFactory(
                    sslbuilder.build(), NoopHostnameVerifier.INSTANCE);
            builder.setSSLSocketFactory(sslsf);
        }
        return builder.build();
    }

    public CloseableHttpClient getHttpClient() {
        return httpClient;
    }

    public InputStream getStream(String path) throws IOException, URISyntaxException {
        URI uri = toURI(path);

        return getStream(uri);
    }

    public InputStream getStream(URI uri ) throws IOException {
        HttpGet request = new HttpGet(uri);
        CloseableHttpResponse response = httpClient.execute(request);
        String msg;
        switch (response.getStatusLine().getStatusCode()){
            case HttpStatus.SC_OK:
                return response.getEntity().getContent();
            case HttpStatus.SC_MULTIPLE_CHOICES:
                msg = formatError(uri.toString(), response.getStatusLine().getStatusCode(),
                        "It seems that the \"Json Max Results\" in Sling Get Servlet is too low. Increase it to a higher value, e.g. 1000.");
                throw new IOException(msg);
            default:
                msg = formatError(uri.toString(), response.getStatusLine().getStatusCode(), "Response: " + EntityUtils.toString(response.getEntity()));
                throw new IOException(msg);
        }
    }

    public String getString(URI uri) throws IOException {
        HttpGet request = new HttpGet(uri);
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String str = EntityUtils.toString(response.getEntity());
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                return str;
            } else {
                String msg = formatError(uri.toString(), response.getStatusLine().getStatusCode(), "Response: " + str);
                throw new IOException(msg);
            }
        }
    }

    private String formatError(String uri, int statusCode, String message) {
        return String.format("Failed to fetch data from %s, HTTP [%d]%n%s", uri, statusCode, message);
    }

    public String getPrimaryType(String path) throws IOException, URISyntaxException {
        URI uri = toURI(path + "/" + JCR_PRIMARYTYPE);
        String str = getString(uri);
        if (str.isEmpty()) {
            throw new IllegalStateException("It appears " + hostConfiguration.getUsername()
                    + " user does not have permissions to read " + uri);
        }
        return str;
    }

    public List<String> listChildren(String path) throws IOException, URISyntaxException {
        List<String> children;
        try (InputStream is = getStream(path + ".1.json"); JsonReader reader = Json.createReader(is)) {
            children = reader
                    .readObject()
                    .entrySet()
                    .stream()
                    .filter(entry -> entry.getValue().getValueType() == JsonValue.ValueType.OBJECT)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        }
        return children;
    }

    public JsonObject getJson(String path, String... parameters) throws IOException, URISyntaxException {
        URI uri = toURI(path, parameters);
        return getJson(uri);
    }

    public JsonObject getJson(URI uri) throws IOException {
        try (InputStream is = getStream(uri); JsonReader reader = Json.createReader(is)) {
            return reader.readObject();
        }
    }

    URI toURI(String path, String ... parameters) throws URISyntaxException {
        URIBuilder ub = new URIBuilder(hostConfiguration.getHost())
                .setPath(path);
        if(parameters != null) {
            if (parameters.length % 2 != 0) {
                throw new IllegalArgumentException("query string parameters must be an even number of name/values:" + Arrays.asList(parameters));
            }
            for (int i = 0; i < parameters.length; i += 2) {
                ub.addParameter(parameters[i], parameters[i + 1]);
            }
        }
        return ub.build();
    }

    @Override
    public void close() throws IOException {
        httpClient.close();
    }
}
