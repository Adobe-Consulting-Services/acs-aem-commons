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
import com.adobe.acs.commons.mcp.ProcessDefinitionFactory;
import com.adobe.acs.commons.mcp.form.FieldComponent;
import com.adobe.acs.commons.mcp.util.AnnotatedFieldDeserializer;
import com.adobe.cq.sightly.WCMUsePojo;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang.StringUtils;
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

    Map<String, ProcessDefinitionFactory> definitions = Collections.emptyMap();
    Map<String, FieldComponent> fieldComponents = Collections.emptyMap();

    @Override
    public void activate() throws Exception {
        SlingScriptHelper sling = getSlingScriptHelper();
        boolean isAdminUser = sling.getRequest().getUserPrincipal().getName().equalsIgnoreCase("admin");
        ProcessDefinitionFactory[] allDefinitionFactories = sling.getServices(ProcessDefinitionFactory.class, null);
        definitions = Stream.of(allDefinitionFactories)
                .filter(o->
                    !(o.getRequiresAdmin()) || isAdminUser
                )
                .collect(Collectors.toMap(ProcessDefinitionFactory::getName, o -> o, (a,b)->a, TreeMap::new));
        String processDefinitionName = get("processDefinition", String.class);
        if (StringUtils.isEmpty(processDefinitionName)) {
            processDefinitionName = getRequest().getParameter("processDefinition");
        }
        if (StringUtils.isNotEmpty(processDefinitionName) && definitions.containsKey(processDefinitionName)) {
            Class clazz = definitions.get(processDefinitionName).createProcessDefinition().getClass();
            fieldComponents = AnnotatedFieldDeserializer.getFormFields(clazz, sling);
        }
    }

    public Map<String, ProcessDefinitionFactory> getDefinitions() {
        return definitions;
    }

    public Map<String, FieldComponent> getFieldComponents() {
        return fieldComponents;
    }
}
