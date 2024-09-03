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
package com.adobe.acs.commons.http;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** 
 * Encapsulates a single Java {@link HttpClient}. Its lifetime and basic configuration is managed via OSGi (Config Admin and Declarative
 * Services).
 * @since 2.2.0 (Bundle Version 6.5.0)
 * @see HttpClientFactory HttpClientFactory, for a similar service for the Apache Http Client
 */
public interface OsgiManagedJavaHttpClient {

    /** Returns the configured HTTP client.
     * 
     * @return the HTTP client 
     */
    @NotNull HttpClient getClient();

    /**
     * Similar to {@link #getClient()} but customizes the underlying {@link HttpClient.Builder} which is used to create the singleton HTTP
     * client
     * 
     * @param builderCustomizer a {@link Consumer} taking the {@link HttpClient.Builder} initialized with the configured basic options.
     * 
     * @throws IllegalStateException in case {@link #getClient()} has been called already
     */
    @NotNull HttpClient getClient(@Nullable Consumer<HttpClient.Builder> builderCustomizer);

    /** Creates a new configured HTTP request.
     * 
     * @param uri the URI to target
     * @return the new request
     */
    @NotNull HttpRequest createRequest(@NotNull URI uri);

    /** Creates a new configured HTTP request.
     * 
     * @param uri the URI to target
     * @return the new request
     */
    @NotNull HttpRequest createRequest(@NotNull URI uri, @Nullable Consumer<HttpRequest.Builder> builderCustomizer);
}
