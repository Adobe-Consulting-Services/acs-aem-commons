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
package com.adobe.acs.commons.mcp.model.impl;

import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.fam.ActionManagerFactory;
import com.adobe.acs.commons.functions.CheckedConsumer;
import com.adobe.acs.commons.mcp.ProcessInstance;
import com.adobe.acs.commons.mcp.model.ManagedProcess;
import com.adobe.acs.commons.mcp.util.DeserializeException;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Via;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.jcr.RepositoryException;
import javax.management.openmbean.CompositeData;
import java.io.Serializable;
import java.util.Map;

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
        return StringUtils.defaultIfBlank(getInfo().getName(), getId());
    }

    @Override
    public ManagedProcess getInfo() {
        // Handle cases where archived processes are invalid/null such that they display in the UI and do not break its rendering.
        if (infoBean.getName() == null) {
            infoBean.setName("Invalid (" + getId() + ")");
        }

        if (infoBean.getDescription() == null) {
            infoBean.setDescription("Archived process at " + getPath() + " is null");
        }

        if (infoBean.getStartTime() == null) {
            infoBean.setStartTime(-1L);
        }

        if (infoBean.getStopTime() == null) {
            infoBean.setStopTime(-1L);
        }

        return infoBean;
    }

    @Override
    public String getId() {
        return StringUtils.defaultString(id, resource.getName());
    }

    @Override
    public String getPath() {
        return StringUtils.defaultString(path, resource.getPath());
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
