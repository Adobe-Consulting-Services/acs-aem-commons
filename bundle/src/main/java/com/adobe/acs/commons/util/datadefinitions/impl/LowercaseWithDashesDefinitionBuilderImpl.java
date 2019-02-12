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
package com.adobe.acs.commons.util.datadefinitions.impl;

import com.adobe.acs.commons.util.datadefinitions.ResourceDefinitionBuilder;
import com.adobe.acs.commons.util.datadefinitions.ResourceDefinition;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;

@Component
@Properties({
        @Property(
                name = ResourceDefinitionBuilder.PROP_NAME,
                value = LowercaseWithDashesDefinitionBuilderImpl.NAME,
                propertyPrivate = true
        )
})
@Service
public class LowercaseWithDashesDefinitionBuilderImpl implements ResourceDefinitionBuilder {
    public static final String NAME = "LOWERCASE_WITH_DASHES";

    @Override
    public final ResourceDefinition convert(final String data) {
        final String title = data;

        String name = data;
        name = StringUtils.stripToEmpty(name);
        name = StringUtils.lowerCase(name);
        name = StringUtils.replace(name, "&", " and ");
        name = StringUtils.replace(name, "/", " or ");
        name = StringUtils.replace(name, "%", " percent ");
        name = name.replaceAll("[^a-z0-9-]+", "-");
        name = StringUtils.stripEnd(name, "-");
        name = StringUtils.stripStart(name, "-");

        final BasicResourceDefinition dataDefinition = new BasicResourceDefinition(name);

        dataDefinition.setTitle(title);

        return dataDefinition;
    }

    @Override
    public boolean accepts(String data) {
        // Accepts any formats
        return true;
    }
}
