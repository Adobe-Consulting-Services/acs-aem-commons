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
package com.adobe.acs.commons.mcp.impl;

import com.adobe.acs.commons.fam.ActionManagerFactory;
import com.adobe.acs.commons.mcp.ControlledProcessManager;
import com.adobe.acs.commons.mcp.ProcessDefinition;
import com.adobe.acs.commons.mcp.ProcessDefinitionFactory;
import com.adobe.acs.commons.mcp.ProcessInstance;
import com.adobe.acs.commons.mcp.model.impl.ArchivedProcessInstance;
import com.adobe.acs.commons.util.visitors.TreeFilteringResourceVisitor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.TabularDataSupport;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.slf4j.LoggerFactory;

/**
 * Implementation of ControlProcessManager service
 */
@Component
@Service(ControlledProcessManager.class)
@Property(name = "jmx.objectname", value = "com.adobe.acs.commons:type=Manage Controlled Processes")
public class ControlledProcessManagerImpl implements ControlledProcessManager {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(ControlledProcessManagerImpl.class);

    private static final String SERVICE_NAME = "manage-controlled-processes";
    private static final Map<String, Object> AUTH_INFO;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_MULTIPLE, bind = "bindDefinitionFactory", unbind = "unbindDefinitionFactory", referenceInterface = ProcessDefinitionFactory.class, policy = ReferencePolicy.DYNAMIC)
    private final List<ProcessDefinitionFactory> processDefinitionFactories = new CopyOnWriteArrayList<>();

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

    protected void bindDefinitionFactory(ProcessDefinitionFactory fac) {
        processDefinitionFactories.add(fac);
    }

    protected void unbindDefinitionFactory(ProcessDefinitionFactory fac) {
        processDefinitionFactories.remove(fac);
    }

    @Override
    public ProcessInstance getManagedProcessInstanceByPath(String path) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ProcessInstance getManagedProcessInstanceByIdentifier(String id) {
        ProcessInstance process = activeProcesses.get(id);
        if (process != null) {
            process.updateProgress();
        }
        return process;
    }

    @Override
    public ProcessInstance createManagedProcessInstance(ProcessDefinition definition, String description) {
        ProcessInstance instance = new ProcessInstanceImpl(this, definition, description);
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
        activeProcesses.values().removeIf(proc -> !proc.getInfo().isIsRunning());
    }

    @Override
    public ResourceResolver getServiceResourceResolver() throws LoginException {
        return resourceResolverFactory.getServiceResourceResolver(AUTH_INFO);
    }

    @Override
    public ProcessDefinition findDefinitionByNameOrPath(String nameOrPath) throws ReflectiveOperationException {
        if (nameOrPath.startsWith("/")) {
            return findDefinitionByPath(nameOrPath);
        } else {
            return findDefinitionByName(nameOrPath);
        }
    }

    private ProcessDefinition findDefinitionByName(String name) throws ReflectiveOperationException {
        ProcessDefinitionFactory factory = processDefinitionFactories.stream()
                .filter(f -> name.equals(f.getName())).findFirst()
                .orElseThrow(()->new IllegalArgumentException("Unable to find process " + name));

        return factory.createProcessDefinition();
    }

    @SuppressWarnings("squid:S1172")
    private ProcessDefinition findDefinitionByPath(String path) throws ReflectiveOperationException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public TabularDataSupport getStatistics() throws OpenDataException {
        TabularDataSupport stats = new TabularDataSupport(ProcessInstanceImpl.getStaticsTableType());
        activeProcesses.values().stream().map(ProcessInstance::getStatistics).forEach(stats::put);
        return stats;
    }

    @Override
    public void haltProcessById(String id) {
        getManagedProcessInstanceByIdentifier(id).halt();
    }

    @Override
    public void haltProcessByPath(String path) {
        getManagedProcessInstanceByPath(path).halt();
    }

    @Override
    public Collection<ProcessInstance> getActiveProcesses() {
        activeProcesses.forEach((id, process) -> process.updateProgress());
        return activeProcesses.values();
    }
    
    @Override
    public Collection<ProcessInstance> getInactiveProcesses() {
        ArrayList<ProcessInstance> processes = new ArrayList();
        try (ResourceResolver rr = getServiceResourceResolver()) {
            Resource tree = rr.getResource(ProcessInstanceImpl.BASE_PATH);
            TreeFilteringResourceVisitor visitor = new TreeFilteringResourceVisitor();
            visitor.setLeafVisitor((r,l)->{
                if (!activeProcesses.containsKey(r.getName())) {
                    processes.add(r.adaptTo(ArchivedProcessInstance.class));
                }
            });
            visitor.accept(tree);
        } catch (Exception ex) {
            LOG.error("Error getting inactive process list", ex);
        }
        return processes;
    }
}
