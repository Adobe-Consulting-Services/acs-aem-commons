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
package com.adobe.acs.commons.mcp.form;

import com.adobe.acs.commons.mcp.util.AccessibleObjectUtil;
import com.adobe.acs.commons.mcp.util.StringUtil;
import com.day.cq.commons.jcr.JcrUtil;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.osgi.annotation.versioning.ProviderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Radio button selector component
 */
@ProviderType
public abstract class RadioComponent extends FieldComponent {
    private static final String DESCRIPTION_DELIMITER = "::";

    public static class EnumerationSelector extends RadioComponent {

        private static final Logger LOG = LoggerFactory.getLogger(EnumerationSelector.class);

        @Override
        public Map<String, String> getOptions() {
            return Stream.of((Enum[]) AccessibleObjectUtil.getType(getAccessibleObject()).getEnumConstants())
                    .collect(Collectors.toMap(Enum::name,
                                              this::getName,
                                              (k, v)-> { throw new IllegalArgumentException("cannot merge"); },
                                              LinkedHashMap::new));
        }

        private String getName(Enum e) {
            String name = StringUtil.getFriendlyName(e.name());
            try {
                Description desc = e.getClass().getField(e.name()).getAnnotation(Description.class);
                if (desc != null) {
                    name = name + DESCRIPTION_DELIMITER + desc.value();
                }
            } catch (NoSuchFieldException | SecurityException ex) {
                LOG.error("Unable to lookup '{}' on class", name, ex);
            }
            return name;
        }
    }

    @Override
    public void init() {
        setResourceType("granite/ui/components/foundation/form/radiogroup");
        getProperties().put("vertical", hasOption("vertical"));
        getProperties().put("text", getFieldDefinition().name());
        getProperties().remove("fieldLabel");
        getProperties().remove("fieldDescription");
    }

    @Override
    public Resource buildComponentResource() {
        AbstractResourceImpl component = (AbstractResourceImpl) super.buildComponentResource();
        AbstractResourceImpl options = new AbstractResourceImpl("items", null, null, new HashMap<>());
        component.addChild(options);

        String defaultValue = getOption("default").orElse(null);

        getOptions().forEach((value, name) -> {
            final HashMap<String, Object> properties = new HashMap<>();
            final String nodeName = JcrUtil.escapeIllegalJcrChars(value);

            if (value.equals(defaultValue)) {
                properties.put("checked", true);
            }
            properties.put("name", getName());
            properties.put("value", value);
            if (name.contains("::")) {
                String description = StringUtils.substringAfter(name, DESCRIPTION_DELIMITER);
                properties.put("title", description);
                name = StringUtils.substringBefore(name, DESCRIPTION_DELIMITER);
            }
            properties.put("text", name);
            AbstractResourceImpl option = new AbstractResourceImpl("option_" + nodeName, "granite/ui/components/foundation/form/radio", "granite/ui/components/foundation/form/field", properties);
            options.addChild(option);
        });
        return component;
    }

    public abstract Map<String, String> getOptions();
}
