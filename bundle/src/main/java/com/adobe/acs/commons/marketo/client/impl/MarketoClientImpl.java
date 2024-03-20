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
package com.adobe.acs.commons.marketo.client.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AUTH;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.util.EntityUtils;
import org.apache.poi.util.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.marketo.MarketoClientConfiguration;
import com.adobe.acs.commons.marketo.client.MarketoApiException;
import com.adobe.acs.commons.marketo.client.MarketoClient;
import com.adobe.acs.commons.marketo.client.MarketoError;
import com.adobe.acs.commons.marketo.client.MarketoField;
import com.adobe.acs.commons.marketo.client.MarketoForm;
import com.adobe.acs.commons.marketo.client.MarketoResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Implementation of the MarketoClient using the REST API.
 */
@Component(service = MarketoClient.class)
public class MarketoClientImpl implements MarketoClient {

    private static final Logger log = LoggerFactory.getLogger(MarketoClientImpl.class);

    private static final int SOCKET_TIMEOUT_MS = 5000;
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int PAGE_SIZE = 200;

    private ObjectMapper mapper = new ObjectMapper();

    private final HttpClientBuilder clientBuilder = HttpClients.custom()
            .setDefaultRequestConfig(RequestConfig.copy(RequestConfig.DEFAULT)
                    .setSocketTimeout(SOCKET_TIMEOUT_MS)
                    .setConnectTimeout(CONNECT_TIMEOUT_MS)
                    .build());

    @Reference
    protected ConfigurationAdmin configAdmin;

    @Activate
    public void activate() {
        Configuration[] configs;
        try {
            configs = configAdmin.listConfigurations("(service.factoryPid=org.apache.http.proxyconfigurator)");

            if (configs != null) {
                for (Configuration config : configs) {
                    log.info("Evaluating proxy configuration: {}", config.getPid());

                    Dictionary<String, Object> properties = config.getProperties();
                    if (isEnabled(properties)) {
                        String host = String.class.cast(properties.get("proxy.host"));
                        int port = Integer.class.cast(properties.get("proxy.port"));

                        log.debug("Using proxy host: {}", host);
                        HttpHost proxyhost = new HttpHost(host, port);
                        HttpRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxyhost);
                        clientBuilder.setRoutePlanner(routePlanner);

                        String user = String.class.cast(properties.get("proxy.user"));
                        String password = String.class.cast(properties.get("proxy.password"));
                        if (StringUtils.isNotBlank(user)) {
                            log.debug("Using proxy authentication with user: {}", user);
                            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                            credentialsProvider.setCredentials(new AuthScope(host, port),
                                    new UsernamePasswordCredentials(user, password));
                            clientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                        }

                    } else {
                        log.debug("Proxy configuration not enabled");
                    }
                }
            }
        } catch (IOException | InvalidSyntaxException e) {
            log.error("Failed to load proxy configuration", e);
        }

    }

    private boolean isEnabled(Dictionary<String, Object> properties) {
        if (properties == null) {
            return false;
        }

        Object value = properties.get("proxy.enabled");
        if (!(value instanceof Boolean) || !(Boolean.class.cast(value))) {
            return false;
        }

        String host = String.class.cast(properties.get("proxy.host"));
        if (StringUtils.isBlank(host)) {
            return false;
        }
        return true;
    }

    private boolean isSuccessStatus(HttpResponse response) {
        return response.getStatusLine().getStatusCode() < 300 && response.getStatusLine().getStatusCode() >= 200;
    }

    @Override
    public CloseableHttpClient getHttpClient() {
        return clientBuilder.build();
    }

    protected <T> @NotNull T getApiResponse(@NotNull String url, String bearerToken,
            BiFunction<HttpGet, HttpResponse, ParsedResponse<T>> callback)
            throws MarketoApiException {
        CloseableHttpClient client = null;
        HttpGet httpGet = null;
        CloseableHttpResponse response = null;
        try {
            client = getHttpClient();
            log.debug("Sending request to: {}", url);
            httpGet = new HttpGet(url);
            if (StringUtils.isNotBlank(bearerToken)) {
                httpGet.setHeader(AUTH.WWW_AUTH_RESP, "Bearer " + bearerToken);
            }
            response = client.execute(httpGet);
            if (!isSuccessStatus(response)) {
                throw new MarketoApiException("Unexpected API response", httpGet, response);
            }
            ParsedResponse<T> parsed = callback.apply(httpGet, response);
            if (parsed.isSuccess()) {
                return parsed.getResult();
            } else {
                throw parsed.getException();
            }
        } catch (MarketoApiException mae) {
            throw mae;
        } catch (IOException ioe) {
            throw new MarketoApiException("Unexpected I/O Exception calling Marketo API", httpGet, response);
        } finally {
            IOUtils.closeQuietly(client);
            IOUtils.closeQuietly(response);
        }
    }

    public @NotNull String getApiToken(@NotNull MarketoClientConfiguration config) throws MarketoApiException {
        log.trace("getApiToken");
        String url = String.format(
                "https://%s/identity/oauth/token?grant_type=client_credentials&client_id=%s&client_secret=%s",
                config.getEndpointHost(), config.getClientId(), config.getClientSecret());
        return getApiResponse(url, null, (req, res) -> {
            String body = null;
            try {
                body = EntityUtils.toString(res.getEntity());
                Map<?, ?> responseData = mapper.readValue(body, Map.class);
                String token = (String) responseData.get("access_token");
                return new ParsedResponse<String>(token);
            } catch (IOException e) {
                return new ParsedResponse<String>(new MarketoApiException("Failed to get API Token", req, res, body));
            }
        });
    }

    @Override
    public List<MarketoField> getFields(MarketoClientConfiguration config) throws MarketoApiException {
        String apiToken = getApiToken(config);
        List<MarketoField> fields = new ArrayList<>();

        String base = String.format("https://%s/rest/asset/v1/form/fields.json?", config.getEndpointHost());

        for (int i = 0; true; i++) {
            MarketoField[] page = getApiPage(base, apiToken, i, MarketoFieldResponse.class);
            if (page == null || page.length == 0) {
                break;
            } else {
                Arrays.stream(page).forEach(fields::add);
            }
        }
        return fields;
    }

    private @Nullable <T, R extends MarketoResponse<T>> T[] getApiPage(@NotNull String urlBase, @NotNull String token,
            int page, Class<R> responseType) throws MarketoApiException {
        log.trace("getApiPage({})", page);
        int offset = PAGE_SIZE * page;

        String url = String.format("%smaxReturn=%s&offset=%s", urlBase, PAGE_SIZE, offset);

        return getApiResponse(url, token, (req, res) -> {
            String body = null;
            try {
                body = EntityUtils.toString(res.getEntity());
                MarketoResponse<T> response = mapper.readValue(body, responseType);
                if (response.getErrors() != null && response.getErrors().length > 0) {
                    throw new IOException("Retrieved errors in response: "
                            + Arrays.stream(response.getErrors()).map(MarketoError::getMessage)
                                    .collect(Collectors.joining(", ")));
                }
                if (!response.isSuccess()) {
                    throw new IOException("Retrieved non-success response");
                }
                return new ParsedResponse<T[]>(response.getResult());
            } catch (IOException ioe) {
                return new ParsedResponse<T[]>(
                        new MarketoApiException("Failed to retrieve Marketo API Page", req, res, body));
            }
        });

    }

    @Override
    public List<MarketoForm> getForms(@NotNull MarketoClientConfiguration config) throws MarketoApiException {
        String apiToken = getApiToken(config);
        List<MarketoForm> forms = new ArrayList<>();
        String base = String.format("https://%s/rest/asset/v1/forms.json?status=approved&", config.getEndpointHost());
        for (int i = 0; true; i++) {

            MarketoForm[] page = getApiPage(base, apiToken, i, MarketoFormResponse.class);
            if (page == null || page.length == 0) {
                break;
            } else {
                Arrays.stream(page).forEach(forms::add);
            }
        }
        return forms;
    }

    class ParsedResponse<T> {
        private final boolean success;
        private final MarketoApiException exception;
        private final T result;

        public ParsedResponse(T result) {
            this.success = true;
            this.result = result;
            this.exception = null;
        }

        public ParsedResponse(MarketoApiException exception) {
            this.success = false;
            this.result = null;
            this.exception = exception;
        }

        /**
         * @return the success
         */
        public boolean isSuccess() {
            return success;
        }

        /**
         * @return the exception
         */
        public MarketoApiException getException() {
            return exception;
        }

        /**
         * @return the result
         */
        public T getResult() {
            return result;
        }

    }

}
