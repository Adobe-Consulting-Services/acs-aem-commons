/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2020 Adobe
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
package com.adobe.acs.commons.properties.impl;

import com.adobe.acs.commons.properties.PropertyConfigService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component(service = PropertyConfigService.class)
@Designate(ocd = PropertyConfigServiceImpl.Config.class)
public class PropertyConfigServiceImpl implements PropertyConfigService {

    private List<Pattern> exclusionList;

    @Override
    public boolean isNotExcluded(final String propertyName) {
        for (Pattern pattern : exclusionList) {
            if (pattern.matcher(propertyName).matches()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isAllowedType(Object object) {
        return String.class.equals(object.getClass()) || Long.class.equals(object.getClass());
    }

    @Activate
    protected void activate(Config config) {
        List<Pattern> excludeList = new ArrayList<>();
        for (String exclude : config.exclude_list()) {
            excludeList.add(Pattern.compile(exclude));
        }
        this.exclusionList = excludeList;
    }

    @ObjectClassDefinition(
            name = "Property Aggregator Service Configuration"
    )
    @interface Config {

        /**
         * The list of patterns or strings to exclude from the property aggregation.
         *
         * @return The list of exclusions
         */
        @AttributeDefinition(
                name = "Exclude List",
                description = "List of properties to exclude, accepts regex.",
                type = AttributeType.STRING
        )
        String[] exclude_list() default {"cq:(.*)"};
    }
}
