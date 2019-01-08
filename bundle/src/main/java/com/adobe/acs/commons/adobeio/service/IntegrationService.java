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

import aQute.bnd.annotation.ProviderType;

/**
 * Service to communicate to Adobe I/O with regards to authentication. 
 * <br/>
 * Use the following command the generate the public/private keyfile:<br/>
 * 
 * openssl req -x509 -sha256 -nodes -days 365 -newkey rsa:2048 -keyout private.key -out certificate_pub.crt
 *
 */
@ProviderType
public interface IntegrationService {
   
   /**
    * Get the api-key, can be used as the X-Api-Key header
    * @return the configured api-key
    */
   String getApiKey();
   
   /**
    * Get the access-token used as the Authorization header.
    * This is fetched once per hour via a scheduler.
    * @return the access token
    */
   String getAccessToken();
   
   /**
    * Timeout in milliseconds that is used in the various http-calls
    * 
    * @return timeout in milliseconds
    */
   int getTimeoutinMilliSeconds();
}
