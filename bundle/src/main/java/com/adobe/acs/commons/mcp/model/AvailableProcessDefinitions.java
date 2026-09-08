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
package com.adobe.acs.commons.mcp.model;

import com.adobe.acs.commons.mcp.ControlledProcessManager;
import com.adobe.acs.commons.mcp.ProcessDefinitionFactory;
import com.adobe.acs.commons.mcp.form.FieldComponent;
import com.adobe.cq.sightly.WCMUsePojo;
import java.util.Collections;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Produce a list of available process definitions that can be started by the
 * user.
 */
@ProviderType
public class AvailableProcessDefinitions extends WCMUsePojo {

    Map<String, ProcessDefinitionFactory> definitions = Collections.emptyMap();
    Map<String, FieldComponent> fieldComponents = Collections.emptyMap();

    @Override
    @SuppressWarnings("checkstyle:parametername")
    public void activate() throws Exception {
        SlingScriptHelper sling = getSlingScriptHelper();
        User user = sling.getRequest().getResourceResolver().adaptTo(User.class);
        ControlledProcessManager cpm = sling.getService(ControlledProcessManager.class);
        definitions = cpm.getAllProcessDefinitionsForUser(user);
        String processDefinitionName = get("processDefinition", String.class);
        if (StringUtils.isEmpty(processDefinitionName)) {
            processDefinitionName = getRequest().getParameter("processDefinition");
        }
        fieldComponents = cpm.getComponentsForProcessDefinition(processDefinitionName, sling);
    }

    public Map<String, ProcessDefinitionFactory> getDefinitions() {
        return definitions;
    }

    public Map<String, FieldComponent> getFieldComponents() {
        return fieldComponents;
    }
}
