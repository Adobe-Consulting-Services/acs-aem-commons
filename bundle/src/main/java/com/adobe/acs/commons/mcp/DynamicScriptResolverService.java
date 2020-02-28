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

 package com.adobe.acs.commons.mcp;

import com.adobe.acs.commons.mcp.form.FieldComponent;
import java.util.Map;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * Provides hooks for injecting additional MCP process definitions.
 * For example, definitions that are not written in Java as OSGi services.
 * Identifier is up to the resolver to determine, examples might be a unique name or a path.
 * It is recommended to use something as specific as JCR path if at all possible for identifier.
 */
@ConsumerType
public interface DynamicScriptResolverService {
    Map<String, ProcessDefinitionFactory> getDetectedProcesDefinitionFactories(ResourceResolver rr);

    default ProcessDefinitionFactory getScriptByIdentifier(ResourceResolver rr, String identifier) {
        return getDetectedProcesDefinitionFactories(rr).get(identifier);
    }

    Map<String, FieldComponent> geFieldComponentsForProcessDefinition(String identifier, SlingScriptHelper sling) throws ReflectiveOperationException;
}
