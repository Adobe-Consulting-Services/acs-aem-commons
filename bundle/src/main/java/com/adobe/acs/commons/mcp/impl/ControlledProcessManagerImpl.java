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
package com.adobe.acs.commons.mcp.impl;

import com.adobe.acs.commons.fam.ActionManagerFactory;
import com.adobe.acs.commons.mcp.ControlledProcessManager;
import com.adobe.acs.commons.mcp.ProcessDefinition;
import com.adobe.acs.commons.mcp.ProcessInstance;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;

/**
 * Implementation of ControlProcessManager service
 */
@Component
@Service(ControlledProcessManager.class)
@Property(name = "jmx.objectname", value = "com.adobe.acs.commons:type=Manage Controlled Processes")
public class ControlledProcessManagerImpl implements ControlledProcessManager {
    private static final String SERVICE_NAME = "mcp";
    private static final Map<String, Object> AUTH_INFO;
    static {
        AUTH_INFO = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, (Object) SERVICE_NAME);
    }
    Map<String, ProcessInstance> activeProcesses = Collections.synchronizedMap(new LinkedHashMap<>());
    
    @Reference
    ResourceResolverFactory resourceResolverFactory;
    
    @Reference
    ActionManagerFactory amf;
    
    @Override
    public ActionManagerFactory getActionManagerFactory() {
        return amf;
    }

    @Override
    public ProcessInstance getManagedProcessInstanceByPath(String path) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ProcessInstance getManagedProcessInstanceByIdentifier(String id) {
        return activeProcesses.get(id);
    }

    @Override
    public ProcessInstance createManagedProcessInstance(ProcessDefinition definition, String description) {
        ProcessInstance instance = new ProcessInstanceImpl(definition, description);
        instance.init(this);
        activeProcesses.put(instance.getId(), instance);
        return instance;
    }

    @Override
    public void haltActiveProcesses() {
        Set<ProcessInstance> instances = new HashSet<>(activeProcesses.values());
        activeProcesses.clear();
        instances.forEach(ProcessInstance::halt);
    }

    @Override
    public void purgeCompletedProcesses() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ResourceResolver getServiceResourceResolver() throws LoginException {
        return resourceResolverFactory.getServiceResourceResolver(AUTH_INFO);
    }
    
}
