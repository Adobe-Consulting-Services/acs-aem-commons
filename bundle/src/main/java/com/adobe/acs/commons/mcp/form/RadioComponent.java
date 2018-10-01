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

import aQute.bnd.annotation.ProviderType;
import com.adobe.acs.commons.mcp.impl.AbstractResourceImpl;
import com.adobe.acs.commons.mcp.util.StringUtil;
import com.day.cq.commons.jcr.JcrUtil;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;

/**
 * Radio button selector component
 */
@ProviderType
public abstract class RadioComponent extends FieldComponent {
    private static final String DESCRIPTION_DELIMITER = "::";
        
    public static class EnumerationSelector extends RadioComponent {

        @Override
        public Map<String, String> getOptions() {
            return Stream.of((Enum[]) getField().getType().getEnumConstants())
                    .collect(Collectors.toMap(Enum::name, this::getName));
        }
        
        private String getName(Enum e) {
            String name = StringUtil.getFriendlyName(e.name());
            try {
                Description desc = e.getClass().getField(e.name()).getAnnotation(Description.class);
                if (desc != null) {
                    name = name + DESCRIPTION_DELIMITER + desc.value();
                }
            } catch (NoSuchFieldException | SecurityException ex) {
                Logger.getLogger(RadioComponent.class.getName()).log(Level.SEVERE, null, ex);
            }
            return name;
        }
    }
    
    @Override
    public void init() {
        setResourceType("granite/ui/components/foundation/form/radiogroup");
        getComponentMetadata().put("vertical", hasOption("vertical"));
        getComponentMetadata().put("text", getFieldDefinition().name());
        getComponentMetadata().remove("fieldLabel");
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
                meta.put("checked", true);
            }
            meta.put("name", getName());
            meta.put("value", value);
            if (name.contains("::")) {
                String description = StringUtils.substringAfter(name, DESCRIPTION_DELIMITER);
                meta.put("title", description);
                name = StringUtils.substringBefore(name, DESCRIPTION_DELIMITER);
            }
            meta.put("text", name);
            AbstractResourceImpl option = new AbstractResourceImpl("option_"+nodeName, "granite/ui/components/foundation/form/radio", "granite/ui/components/foundation/form/field", meta);
            options.addChild(option);
        });
        return component;
    }
    public abstract Map<String, String> getOptions();
}
