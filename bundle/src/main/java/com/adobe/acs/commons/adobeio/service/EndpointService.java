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

import com.adobe.acs.commons.adobeio.types.Filter;
import com.google.gson.JsonObject;

import aQute.bnd.annotation.ProviderType;

/**
 * This is the interface for the EndpointService
 * Using this service, the calling component can post to and retrieve from Service.
 * All the plumbing around authentication will be taken care of.
 *
 * Example how to use in the calling component:
 *   @Reference(target = "(id=///put your id here///)")
 *   private EndpointService endpointService;
 */
@ProviderType
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
     * @return The url of this endpoint
     */
    String getUrl();
    
    /**
     * @return Specific headers used for this endpoint
     */
    Map<String, String> getSpecificServiceHeader();


    /**
     * Performs the GET-action connected to the endpoint
     * @return JsonObject containing the result of the action
     */
    JsonObject performIO_Action();

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
    
}
