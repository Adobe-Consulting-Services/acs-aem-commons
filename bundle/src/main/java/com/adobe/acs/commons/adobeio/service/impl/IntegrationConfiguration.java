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
package com.adobe.acs.commons.adobeio.service.impl;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "ACS AEM Commons - Adobe I/O Integration Configuration",
                     description = "Configuration of Adobe.IO access")
public @interface IntegrationConfiguration {

   @AttributeDefinition(name = "Endpoint", description = "Endpoint for the JWT-check", defaultValue = "https://ims-na1.adobelogin.com/ims/exchange/jwt")
   String endpoint();

   @AttributeDefinition(name = "Login Endpoint", description = "Login Endpoint for the JWT-check", defaultValue = "https://ims-na1.adobelogin.com/c/")
   String loginEndpoint();

   @AttributeDefinition(name = "PrivateKey", description = "Contents of the private.key file")
   String privateKey();

   @AttributeDefinition(name = "ClientId", description = "Client Id")
   String clientId();
 
   @AttributeDefinition(name = "ClientSecret", description = "Client Secret")
   String clientSecret();
    
   @AttributeDefinition(name = "OrgId", description = "Organization id")
   String amcOrgId();
    
   @AttributeDefinition(name = "TechAccountId", description = "Technical Account Id")
   String techAccountId();
        
   @AttributeDefinition(name = "LoginClaim", description = "Login claims")
   String[] adobeLoginClaimKey();
   
   @AttributeDefinition(name = "ExpirationTime", description = "Expiration time of the access token in seconds",
                      defaultValue="7200",type= AttributeType.INTEGER)
   int expirationTimeInSeconds();
   
   @AttributeDefinition(name = "Timeout", description = "Timeout in milliseconds, used in the various http-calls",
           defaultValue="60000",type= AttributeType.INTEGER)
   int timeoutInMilliSeocnds();
}
