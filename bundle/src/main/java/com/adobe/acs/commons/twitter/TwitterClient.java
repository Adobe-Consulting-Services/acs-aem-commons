/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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
package com.adobe.acs.commons.twitter;

import twitter4j.Twitter;
import aQute.bnd.annotation.ProviderType;

import com.day.cq.wcm.webservicesupport.Configuration;

/**
 * Service interface which wraps the Twitter4j API to expose the originating
 * Cloud Service Configuration.
 * 
 * To obtain an instance of this class, adapt either a Page or a Configuration object.
 * 
 * Note that these clients always use only Application authentication.
 */
@ProviderType
public interface TwitterClient {

    /**
     * Get the Cloud Service Configuration from which this client was created.
     * 
     * @return the service configuration
     */
    Configuration getServiceConfiguration();

    /**
     * Get the Twitter4j client.
     * 
     * @return the client
     */
    Twitter getTwitter();

}
