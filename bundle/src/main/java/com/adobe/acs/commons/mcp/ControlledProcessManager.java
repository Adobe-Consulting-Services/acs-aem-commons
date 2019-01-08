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
package com.adobe.acs.commons.mcp;

import aQute.bnd.annotation.ProviderType;
import com.adobe.acs.commons.fam.ActionManagerFactory;
import com.adobe.acs.commons.mcp.mbean.CPMBean;
import java.util.Collection;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Core management container for managing controlled processes.
 */
@ProviderType
public interface ControlledProcessManager extends CPMBean {

    ActionManagerFactory getActionManagerFactory();
    
    ProcessInstance getManagedProcessInstanceByPath(String path);
    
    ProcessInstance getManagedProcessInstanceByIdentifier(String id);
    
    ProcessInstance createManagedProcessInstance(ProcessDefinition definition, String description);
    
    ResourceResolver getServiceResourceResolver() throws LoginException;

    ProcessDefinition findDefinitionByNameOrPath(String nameOrPath) throws ReflectiveOperationException;

    Collection<ProcessInstance> getActiveProcesses();
    
    Collection<ProcessInstance> getInactiveProcesses();    
}
