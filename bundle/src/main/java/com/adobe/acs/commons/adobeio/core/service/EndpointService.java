/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2018 Adobe
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
package com.adobe.acs.commons.adobeio.core.service;

import java.util.Map;

import org.apache.http.impl.client.CloseableHttpClient;

import com.adobe.acs.commons.adobeio.core.types.Filter;
import com.adobe.acs.commons.adobeio.core.types.PKey;
import com.google.gson.JsonObject;

/**
 * This is the interface for the EndPointService
 * Using this service, the calling component can post to and retrieve from Service.
 *
 * Example how to use in the calling component:
 *   @Reference(target = "(getId=///put your id here///)")
 *   private EndpointService endpointService;
 */
public interface EndpointService {

    /**
     * @return The ID of the endpoint
     */
    String getId();

    /**
     * @return The method of the endpoint
     */
    String getMethod();

    /**
     * @return The endpoint that can be used in the url
     */
    String getEndpoint();
    
    /**
     * @return Specific Header used for this service
     */
    Map<String, String> getSpecificServiceHeader();

    /**
     * @param pKey The provided pKey
     * @return The full url of the call.
     */
    String getUrl(PKey pKey);

    /**
     * Performs the GET-action connected to the endpoint
     * @return JsonObject containing the result of the action
     */
    JsonObject performIO_Action();

    /**
     * Performs the GET-action connected to the endpoint
     * @param pkey The provided pkey
     * @return JsonObject containing the result of the action
     */
    JsonObject performIO_Action(PKey pkey);

    /**
     * Performs the GET-action connected to the endpoint
     * @param filter Filter that will be applied
     * @return JsonObject containing the result of the action
     */
    JsonObject performIO_Action(Filter filter);

    /**
     * Performs the action connected to the endpoint
     * @param payload JsonObject containing the data that is used in the action
     * @return JsonObject containing the result of the action
     */
    JsonObject performIO_Action(JsonObject payload);

    /**
     * Performs the action connected to the endpoint
     * @param pkey The provided pkey
     * @param payload JsonObject containing the data that is used in the action
     * @return JsonObject containing the result of the action
     */
    JsonObject performIO_Action(PKey pkey, JsonObject payload);

    /**
     * Performs the GET-action connected to the endpoint
     * @param pkey The provided pkey
     * @param classOfT Type of object that will be returned
     * @return Object containing the result of the action
     */
    <T> T performIO_Action(PKey pkey, Class<T> classOfT);

    /**
     * Performs the action connected to the endpoint
     * @param pkey The provided pkey
     * @param payload JsonObject containing the data that is used in the action
     * @param classOfT Type of object that will be returned
     * @return Object containing the result of the action
     */
    <T> T performIO_Action(PKey pkey, JsonObject payload, Class<T> classOfT);

    /**
     * Straight post-action to the provided url,
     * using the payload as the body of the message.
     * @param url Url of the endpoint
     * @param payload Body of the POST-call
     * @return JsonObject with the result of the call
     */
    JsonObject postIO_Action(String url, JsonObject payload);

    /**
     * This is a test for the connection to the endpoint.
     * The test will be performed using the URL and the GET-method.
     * @return TRUE if connection is successful
     */
    boolean isConnected();
    
    /**
     * @return HTTP Client
     */
    CloseableHttpClient getHttpClient();
}
