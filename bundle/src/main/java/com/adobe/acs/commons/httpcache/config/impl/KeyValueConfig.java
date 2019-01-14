/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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
package com.adobe.acs.commons.httpcache.config.impl;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;


@ObjectClassDefinition(name = "ACS AEM Commons - HTTP Cache - Key / Value extension",
        description = "Defined key / values that will be allowed for this extension.")
public @interface KeyValueConfig {
    @AttributeDefinition(
            name = "Allowed keys",
            description = "ValueMap keys that will used to generate a cache key."
    )
    String[] allowedKeys() default {};

    @AttributeDefinition(
            name = "AllowedValues",
            description = "If set, narrows down specified keys to specified values only."
    )
    String[] allowedValues() default {};

    @AttributeDefinition(
            name = "Empty is allowed",
            description = "Allows no value match to be a cache entry."
    )
    boolean emptyAllowed() default false;

    @AttributeDefinition(name = "Config Name")
    String configName() default StringUtils.EMPTY;
}
