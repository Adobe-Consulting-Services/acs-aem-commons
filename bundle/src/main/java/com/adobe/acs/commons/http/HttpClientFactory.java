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
package com.adobe.acs.commons.http;

import java.util.function.Consumer;

import org.apache.http.client.HttpClient;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * Factory for building pre-configured HttpClient Fluent {@link Executor} and {@link Request} objects
 * based on a configured host, port and (optionally) username/password.
 *
 * Each OSGi configuration with factory PID {@code com.adobe.acs.commons.http.impl.HttpClientFactoryImpl} exposes one service of this kind.
 * The individual factories will generally be accessed by service lookup using the {@code factory.name} property.
 * Despite the name each instance of this interface only holds a single {@link Executor} instance (bound to a single {@code HttpClient}).
 */
public interface HttpClientFactory {

    /**
     * Customizes the underlying {@link HttpClientBuilder} which is used to create the singleton http client and executor
     * @param builderCustomizer a {@link Consumer} taking the {@link HttpClientBuilder} initialized with the configured basic options.
     * @throws IllegalStateException in case {@link #getExecutor()} has been called already
     * @since 2.1.0 (Bundle version 6.4.0)
     */
    void customize(Consumer<HttpClientBuilder> builderCustomizer);

    /**
     * Get the singleton {@link Executor} object.
     *
     * @return the executor
     */
    Executor getExecutor();

    /**
     * Create a GET request using the base hostname and port defined in the factory configuration.
     *
     * @param partialUrl the portion of the URL after the port (and slash)
     *
     * @return a fluent Request object
     */
    Request get(String partialUrl);

    /**
     * Create a PUT request using the base hostname and port defined in the factory configuration.
     *
     * @param partialUrl the portion of the URL after the port (and slash)
     *
     * @return a fluent Request object
     */
    Request put(String partialUrl);

    /**
     * Create a POST request using the base hostname and port defined in the factory configuration.
     *
     * @param partialUrl the portion of the URL after the port (and slash)
     *
     * @return a fluent Request object
     */
    Request post(String partialUrl);

    /**
     * Create a DELETE request using the base hostname and port defined in the factory configuration.
     *
     * @param partialUrl the portion of the URL after the port (and slash)
     *
     * @return a fluent Request object
     */
    Request delete(String partialUrl);

    /**
     * Create a OPTIONS request using the base hostname and port defined in the factory configuration.
     *
     * @param partialUrl the portion of the URL after the port (and slash)
     *
     * @return a fluent Request object
     */
    Request options(String partialUrl);
}
