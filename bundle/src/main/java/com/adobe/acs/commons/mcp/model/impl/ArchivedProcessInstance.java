/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 - Adobe
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
package com.adobe.acs.commons.mcp.model.impl;

import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.fam.ActionManagerFactory;
import com.adobe.acs.commons.functions.CheckedConsumer;
import com.adobe.acs.commons.mcp.ProcessInstance;
import com.adobe.acs.commons.mcp.model.ManagedProcess;
import com.adobe.acs.commons.mcp.util.DeserializeException;
import java.io.Serializable;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.jcr.RepositoryException;
import javax.management.openmbean.CompositeData;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Via;

/**
 *
 */
@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class ArchivedProcessInstance implements ProcessInstance, Serializable {
    private static final long serialVersionUID = 7526472295622776155L;
    private static final String MSG_ARCHIVED_PROCESSES_HAVE_NO_ACTIONS = "Archived processes have no actions";

    @Inject
    private transient Resource resource;

    @Inject
    @Via("path")
    String path;

    @Inject
    @Via("name")
    String id;

    @Inject
    @Named("jcr:content")
    public ManagedProcess infoBean;

    @Override
    public String getName() {
        return getInfo().getName();
    }

    @Override
    public ManagedProcess getInfo() {
        return infoBean;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getPath() {
        return path;
    }

    @PostConstruct
    protected void markNotRunning() {
        if (getInfo().isIsRunning()) {
            getInfo().setIsRunning(false);
            getInfo().setStatus("Halted abnormally");
        }
    }
    
    @PostConstruct
    protected void setValues() {
        id = resource.getName();
        path = resource.getPath();
    }

    @Override
    public void init(ResourceResolver resourceResolver, Map<String, Object> parameterMap) throws DeserializeException, RepositoryException {
        throw new UnsupportedOperationException(MSG_ARCHIVED_PROCESSES_HAVE_NO_ACTIONS);
    }

    @Override
    public ActionManagerFactory getActionManagerFactory() {
        throw new UnsupportedOperationException(MSG_ARCHIVED_PROCESSES_HAVE_NO_ACTIONS);
    }

    @Override
    public ActionManager defineCriticalAction(String name, ResourceResolver rr, CheckedConsumer<ActionManager> builder) throws LoginException {
        throw new UnsupportedOperationException(MSG_ARCHIVED_PROCESSES_HAVE_NO_ACTIONS);
    }

    @Override
    public ActionManager defineAction(String name, ResourceResolver rr, CheckedConsumer<ActionManager> builder) throws LoginException {
        throw new UnsupportedOperationException(MSG_ARCHIVED_PROCESSES_HAVE_NO_ACTIONS);
    }

    @Override
    public double updateProgress() {
        throw new UnsupportedOperationException(MSG_ARCHIVED_PROCESSES_HAVE_NO_ACTIONS);
    }

    @Override
    public void run(ResourceResolver rr) {
        throw new UnsupportedOperationException(MSG_ARCHIVED_PROCESSES_HAVE_NO_ACTIONS);
    }

    @Override
    public void halt() {
        throw new UnsupportedOperationException(MSG_ARCHIVED_PROCESSES_HAVE_NO_ACTIONS);
    }

    @Override
    public CompositeData getStatistics() {
        throw new UnsupportedOperationException(MSG_ARCHIVED_PROCESSES_HAVE_NO_ACTIONS);
    }
}
