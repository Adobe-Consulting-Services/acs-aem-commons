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

    @AttributeDefinition(
            name = "Enable resource cache",
            description = "If enabled, will cache I18n maps for resource based retrieval"
                    + " so if another call is made on the same resource, it won't need to retrieve it a second time."
                    + " Helps if there are Sling Models with alot of I18n keys on the same model resource.",
            defaultValue = "false")
    boolean useResourceCache();
}
