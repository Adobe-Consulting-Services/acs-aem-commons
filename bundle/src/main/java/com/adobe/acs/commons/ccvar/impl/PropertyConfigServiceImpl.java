/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.ccvar.impl;

import com.adobe.acs.commons.ccvar.PropertyConfigService;
import com.adobe.acs.commons.ccvar.TransformAction;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component(service = PropertyConfigService.class)
@Designate(ocd = PropertyConfigServiceImpl.Config.class)
public class PropertyConfigServiceImpl implements PropertyConfigService {
    public static final String PARSER_SEPARATOR = "!";

    private static final Logger LOG = LoggerFactory.getLogger(PropertyConfigServiceImpl.class);

    private List<Pattern> exclusionList;

    @Reference(policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
    private List<TransformAction> actions;

    @Override
    public boolean isAllowed(final String propertyName) {
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

    @Override
    public TransformAction getAction(String key) {
        if (StringUtils.contains(key, PARSER_SEPARATOR)) {
            String actionName = StringUtils.substringAfter(key, PARSER_SEPARATOR);
            for (TransformAction action : actions) {
                if (StringUtils.equals(action.getName(), actionName)) {
                    return action;
                }
            }
            LOG.warn("Action specified with name [{}] was not found in map.", actionName);
        }
        return null;
    }

    @Activate
    protected void activate(Config config) {
        List<Pattern> excludeList = new ArrayList<>();
        for (String exclude : config.exclude_list()) {
            if (StringUtils.isNotBlank(exclude)) {
                excludeList.add(Pattern.compile(exclude));
            }
        }
        this.exclusionList = excludeList;
    }

    @ObjectClassDefinition(
            name = "ACS AEM Commons - Contextual Content Variable Property Aggregator Service Configuration"
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
        String[] exclude_list() default {"cq:.*"};
    }
}
