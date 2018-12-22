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
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(name = "ACS AEM Commons - Adobe I/O Endpoint Factory Configuration",
        description = "Configuration of Adobe.io endpoints")
public @interface EndpointConfiguration {

    @AttributeDefinition(name = "ID", description = "Id of the endpoint", required = true)
    String id();

    @AttributeDefinition(
       name = "Method",
       description = "Used method for the endpoint",
       options = {
                @Option(label = "GET", value = "GET"),
                @Option(label = "POST", value = "POST"),
                @Option(label = "PUT", value = "PUT"),
                @Option(label = "PATCH", value = "PATCH"),
                @Option(label = "DELETE", value = "DELETE"),
        },
       required = true
    )
    String method();

    @AttributeDefinition(name = "URL Endpoint", description = "Full Endpoint URL including domain", required = true)
    String endpoint();
    
    @AttributeDefinition(name = "Service specific Header", description = "name:value E.g.: x-product:app-name")
    String[] specificServiceHeaders();

    String webconsole_configurationFactory_nameHint() default "Endpoint <b>{id}</b><br/> {endpoint}";

}
