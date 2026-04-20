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
package com.adobe.acs.commons.marketo.client;

import java.io.IOException;
import java.util.List;

import org.apache.http.impl.client.CloseableHttpClient;
import org.jetbrains.annotations.NotNull;

import com.adobe.acs.commons.marketo.MarketoClientConfiguration;

/**
 * A client for interacting with Marketo's API.
 */
public interface MarketoClient {

    /**
     * Retrieve an API token used for interacting with the Marketo API.
     * 
     * @param config the configuration to use to retrieve the token
     * @return a valid Marketo API Token
     * @throws IOException an error occurs retrieving the token
     */
    public @NotNull String getApiToken(@NotNull MarketoClientConfiguration config) throws MarketoApiException;

    /**
     * Retrieve a HttpClient for interacting with the Marketo API
     * 
     * @return the httpclient
     */
    public @NotNull CloseableHttpClient getHttpClient();

    /**
     * Retrieve all of the available forms from the current organization in Marketo.
     * 
     * @param config the configuration for this request
     * @return the full list of available forms
     * @throws IOException an exception occurs interacting with the API
     */
    public @NotNull List<MarketoForm> getForms(@NotNull MarketoClientConfiguration config) throws MarketoApiException;

    /**
     * Retrieve all of the available forms from the current organization in Marketo.
     * 
     * @param config the configuration for this request
     * @return the full list of available forms
     * @throws IOException an exception occurs interacting with the API
     */
    public @NotNull List<MarketoField> getFields(@NotNull MarketoClientConfiguration config) throws MarketoApiException;

}
