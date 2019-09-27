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
package com.adobe.acs.commons.i18n.impl;


import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "I18NProviderImpl",
        description = "Provides I18n translations translations / maps from a resource or locale"
)
public @interface Config {

    String PN_MAX_SIZE_IN_MB = "maxSizeCount";
    String PN_TTL = "ttl";

    long DEFAULT_MAX_SIZE_IN_MB = 10L;
    long DEFAULT_TTL = -1L;

    @AttributeDefinition(name = "CacheSize (count)", description = "This determines the cache size of caching I18n maps to resources.")
    long maxSizeCount() default DEFAULT_MAX_SIZE_IN_MB;

    @AttributeDefinition(name = "Cache expiry", description = "This determines the cache expiry time in seconds of caching I18n maps to resources.")
    long getTtl() default  DEFAULT_TTL;
}
