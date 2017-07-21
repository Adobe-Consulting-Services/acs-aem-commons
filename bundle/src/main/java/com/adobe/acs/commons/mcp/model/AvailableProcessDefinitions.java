/*
 * Copyright 2017 Adobe.
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
package com.adobe.acs.commons.mcp.model;

import aQute.bnd.annotation.ProviderType;
import com.adobe.acs.commons.mcp.HiddenProcessDefinition;
import com.adobe.acs.commons.mcp.form.FieldComponent;
import com.adobe.acs.commons.mcp.form.FormField;
import com.adobe.acs.commons.mcp.ProcessDefinition;
import com.adobe.cq.sightly.WCMUsePojo;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Produce a list of available process definitions that can be started by the
 * user.
 */
@ProviderType
public class AvailableProcessDefinitions extends WCMUsePojo {
    private static final Logger LOG = LoggerFactory.getLogger(AvailableProcessDefinitions.class);

    Map<String, ProcessDefinition> definitions = Collections.EMPTY_MAP;
    Map<String, FieldComponent> fieldComponents = Collections.EMPTY_MAP;

    @Override
    public void activate() throws Exception {
        SlingScriptHelper sling = getSlingScriptHelper();
        boolean isAdminUser = sling.getRequest().getUserPrincipal().getName().equalsIgnoreCase("admin");
        ProcessDefinition[] allDefinitions = sling.getServices(ProcessDefinition.class, null);
        definitions = Stream.of(allDefinitions)
                .filter(o->
                    !(o instanceof HiddenProcessDefinition) || isAdminUser
                )
                .collect(Collectors.toMap(o -> o.getClass().getName(), o -> o, (a,b)->a, TreeMap::new));
        String processDefinitionName = get("processDefinition", String.class);
        if (StringUtils.isEmpty(processDefinitionName)) {
            processDefinitionName = getRequest().getParameter("processDefinition");
        }
        if (StringUtils.isNotEmpty(processDefinitionName) && definitions.containsKey(processDefinitionName)) {
            fieldComponents = FieldUtils.getFieldsListWithAnnotation(definitions.get(processDefinitionName).getClass(), FormField.class)
                    .stream()
                    .collect(Collectors.toMap(Field::getName, f -> {
                        FormField fieldDefinition = f.getAnnotation(FormField.class);
                        FieldComponent component;
                        try {
                            component = fieldDefinition.component().newInstance();
                            component.setup(f.getName(), f, fieldDefinition, sling);
                            return component;
                        } catch (InstantiationException | IllegalAccessException ex) {
                            LOG.error("Unable to instantiate field component for "+f.getName(), ex);
                        }
                        return null;
                    }, (a,b)->a, LinkedHashMap::new));
        }
    }

    public Map<String, ProcessDefinition> getDefinitions() {
        return definitions;
    }

    public Map<String, FieldComponent> getFieldComponents() {
        return fieldComponents;
    }
}
