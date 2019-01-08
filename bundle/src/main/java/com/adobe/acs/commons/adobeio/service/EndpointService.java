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
package com.adobe.acs.commons.adobeio.service;

import java.util.Map;

import com.google.gson.JsonObject;

import aQute.bnd.annotation.ProviderType;

/**
 * This is the interface for the EndpointService.<br/>
 * Using this service, the calling component can post to and retrieve from the endpoint.<br/>
 * All the plumbing around authentication will be taken care of.<br/>
 *
 * Example how to use the service in your custom code:<br/>
 * {@code @Reference(target="(id=yourEndpointId)")}<br/>
 * {@code private EndpointService endpointService;}
 * </code>
 */
@ProviderType
public interface EndpointService {

    /**
     * The id of the endpoint that is defined via the configuration
     * 
     * @return The ID of the endpoint
     */
    String getId();

    /**
     * The method of the endpoint that is defined via the configuration
     * 
     * @return The method of the endpoint
     */
    String getMethod();

    /**
     * The url of the endpoint that is defined via the configuration
     * 
     * @return The url of this endpoint
     */
    String getUrl();


    /**
     * Performs the action connected to the endpoint.
     * 
     * @return JsonObject containing the result of the action
     */
    JsonObject performIO_Action();

    /**
     * Performs the action connected to the endpoint.
     * With the parameters you can influence all the aspects of the api-call.
     * 
     * @param url the url of the api call, the url can include queryparameter
     * @param method the method of the api call, like GET or POST
     * @param headers headers that need to passed to the api, on top of the authentication headers
     * @param payload an optional payload for the api-call
     * 
     * @return JsonObject containing the result of the action
     */
    JsonObject performIO_Action(String url, String method, String[] headers, JsonObject payload );

    
    /**
     * Performs the action connected to the endpoint, with the specified queryParameters.
     * 
     * @param queryParameters query parameters to pass to the endpoint
     * @return JsonObject containing the result of the action
     */
    JsonObject performIO_Action(Map<String, String> queryParameters);

    /**
     * Performs the action connected to the endpoint with the specified payload.
     * @param payload JsonObject containing the data that is used in the action
     * @return JsonObject containing the result of the action
     */
    JsonObject performIO_Action(JsonObject payload);

    /**
     * This is a test for the connection to the endpoint.
     * The test will be performed using the URL and the GET-method.
     * @return TRUE if connection is successful
     */
    boolean isConnected();
    
    /**
     * Gets the headers that are set via the configuration.
     * 
     * @return an array with the headers, in the format &lt;name:value&gt;
     */
    String[] getConfigServiceSpecificHeaders();
    
}
