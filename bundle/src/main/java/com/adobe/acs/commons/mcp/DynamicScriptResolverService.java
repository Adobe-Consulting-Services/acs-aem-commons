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
    /**
     * Prepare a list of custom process definition factory instances
     * @param rr Resource resolver to use (note this will usually be the MCP service user, so make sure that doesn't cause ACL issues)
     * @return Map of String (Path or identifier) -&gt; ProessDefinitionFactory instances
     */
    Map<String, ProcessDefinitionFactory> getDetectedProcesDefinitionFactories(ResourceResolver rr);

    /**
     * Given an identifier, look up the process definition factory.  The default definition should be sufficient for most uses but can be overridden if needed.
     * @param rr Resource resolver to use (note this will usually be the MCP service user, so make sure that doesn't cause ACL issues)
     * @param identifier JCR path or other applicable script identifier
     * @return PrccessDefinitionFactory if available, null otherwise.
     */
    default ProcessDefinitionFactory getScriptByIdentifier(ResourceResolver rr, String identifier) {
        return getDetectedProcesDefinitionFactories(rr).get(identifier);
    }

    /**
     * Generate list of components needed to build the start dialog of a process
     * @param identifier JCR path or other applicable script identifier
     * @param sling Sling helper
     * @return Map of String (variable name) -&gt; FieldComponent for process if it exists (can be an empty map), null if process definition doesn't exist.
     * @throws ReflectiveOperationException If needed because reflection is used to generate this list
     */
    Map<String, FieldComponent> geFieldComponentsForProcessDefinition(String identifier, SlingScriptHelper sling) throws ReflectiveOperationException;
}
