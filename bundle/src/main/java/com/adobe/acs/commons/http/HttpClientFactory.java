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
package com.adobe.acs.commons.http;

import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;

/**
 * Factory for building pre-configured HttpClient Fluent Executor and Request objects
 * based a configure host, port and (optionally) username/password.
 *
 * Factories will generally be accessed by service lookup using the factory.name property.
 */
public interface HttpClientFactory {

    /**
     * Get the configured Executor object from this factory.
     *
     * @return an Executor object
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
