/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
package com.adobe.acs.commons.mcp.form;

import com.adobe.acs.commons.mcp.util.AccessibleObjectUtil;
import com.adobe.acs.commons.mcp.util.StringUtil;
import com.day.cq.commons.jcr.JcrUtil;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Select (drop-down) selector component
 */
public abstract class SelectComponent extends FieldComponent {
    @ProviderType
    public static class EnumerationSelector extends SelectComponent {
        @Override
        public Map<String, String> getOptions() {
            return Stream.of((Enum[]) AccessibleObjectUtil.getType(getAccessibleObject()).getEnumConstants())
                    .collect(Collectors.toMap(Enum::name,
                                              e -> StringUtil.getFriendlyName(e.name()),
                                              (k, v)-> { throw new IllegalArgumentException("cannot merge"); },
                                              LinkedHashMap::new));
        }
    }

    @Override
    public void init() {
        setResourceType("granite/ui/components/coral/foundation/form/select");
        getComponentMetadata().put("text", getFieldDefinition().name());
    }

    @Override
    public Resource buildComponentResource() {
        AbstractResourceImpl component = (AbstractResourceImpl) super.buildComponentResource();
        AbstractResourceImpl options = new AbstractResourceImpl("items", null, null, new ResourceMetadata());
        component.addChild(options);

        String defaultValue = getOption("default").orElse(null);

        getOptions().forEach((value, name)->{
            final ResourceMetadata meta = new ResourceMetadata();
            final String nodeName = JcrUtil.escapeIllegalJcrChars(value);

            if (value.equals(defaultValue)) {
                meta.put("selected", true);
            }
            meta.put("value", value);
            meta.put("text", name);
            AbstractResourceImpl option = new AbstractResourceImpl(
                    "option_" + nodeName,
                    null,
                    null,
                    meta);
            options.addChild(option);
        });
        return component;
    }

    public abstract Map<String, String> getOptions();
}
