/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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
package com.adobe.acs.commons.fam.impl;

import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.fam.ActionManagerConstants;
import com.adobe.acs.commons.fam.ActionManagerFactory;
import com.adobe.acs.commons.fam.ThrottledTaskRunner;
import com.adobe.acs.commons.fam.mbean.ActionManagerMBean;
import com.adobe.granite.jmx.annotation.AnnotatedStandardMBean;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.management.NotCompliantMBeanException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.TabularDataSupport;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;

@Component
@Service(ActionManagerFactory.class)
@Property(name = "jmx.objectname", value = "com.adobe.acs.commons:type=Action Manager")
public class ActionManagerFactoryImpl extends AnnotatedStandardMBean implements ActionManagerFactory {


    @Reference
    ThrottledTaskRunner taskRunner;
    
    private final Map<String, ActionManager> tasks;
    
    public ActionManagerFactoryImpl() throws NotCompliantMBeanException {
        super(ActionManagerMBean.class);
        tasks = Collections.synchronizedMap(new LinkedHashMap<>());
    }

    @Override
    public ActionManager createTaskManager(String name, ResourceResolver resourceResolver, int saveInterval) throws LoginException {
        return this.createTaskManager(name, resourceResolver, saveInterval, ActionManagerConstants.DEFAULT_ACTION_PRIORITY);
    }

    @Override
    public ActionManager createTaskManager(String name, ResourceResolver resourceResolver, int saveInterval, int priority) throws LoginException {
        String fullName = String.format("%s (%s)", name, UUID.randomUUID().toString());
        
        ActionManagerImpl manager = new ActionManagerImpl(fullName, taskRunner, resourceResolver, saveInterval, priority);
        tasks.put(fullName, manager);
        return manager;
    }

    @Override
    public ActionManager getActionManager(String name) {
        if (name == null) {
            return null;
        }

        return this.tasks.get(name);
    }

    @Override
    public boolean hasActionManager(String name) {
        return this.tasks.get(name) != null;
    }

    @Override
    public TabularDataSupport getStatistics() throws OpenDataException {
        TabularDataSupport stats = new TabularDataSupport(ActionManagerImpl.getStaticsTableType());
        for (ActionManager task : tasks.values()) {
            stats.put(task.getStatistics());
        }
        return stats;
    }
    
    @Override
    public TabularDataSupport getFailures() throws OpenDataException {
        TabularDataSupport stats = new TabularDataSupport(ActionManagerImpl.getFailuresTableType());
        for (ActionManager task : tasks.values()) {
            stats.putAll(task.getFailures().toArray(new CompositeData[0]));
        }
        return stats;
    }    
    
    @Override
    public void purgeCompletedTasks() {
        for (Iterator<ActionManager> taskIterator = tasks.values().iterator(); taskIterator.hasNext();) {
            ActionManager task = taskIterator.next();
            if (task.isComplete() || taskRunner.getActiveCount() == 0) {
                task.closeAllResolvers();
                taskIterator.remove();
            }
        }
    }

    @Override
    public void purge(ActionManager manager) {
        if (!manager.isComplete()) {
            manager.cancel(true);
        }
        manager.closeAllResolvers();
        tasks.values().remove(manager);
    }
}
