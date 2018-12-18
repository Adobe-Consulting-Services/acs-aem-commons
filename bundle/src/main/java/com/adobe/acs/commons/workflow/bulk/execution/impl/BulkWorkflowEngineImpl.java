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

package com.adobe.acs.commons.workflow.bulk.execution.impl;

import com.adobe.acs.commons.util.QueryHelper;
import com.adobe.acs.commons.workflow.bulk.execution.BulkWorkflowEngine;
import com.adobe.acs.commons.workflow.bulk.execution.model.Config;
import com.adobe.acs.commons.workflow.bulk.execution.model.Status;
import com.adobe.acs.commons.workflow.bulk.execution.model.SubStatus;
import com.adobe.acs.commons.workflow.bulk.execution.model.Workspace;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@Service
public class BulkWorkflowEngineImpl implements BulkWorkflowEngine {
    private static final Logger log = LoggerFactory.getLogger(BulkWorkflowEngineImpl.class);

    private static final String BULK_WORKFLOW_MANAGER_PAGE_FOLDER_PATH = "/etc/acs-commons/bulk-workflow-manager";

    private static final String SERVICE_NAME = "bulk-workflow";
    private static final Map<String, Object> AUTH_INFO;

    static {
        AUTH_INFO = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, (Object) SERVICE_NAME);
    }

    @Reference
    private QueryHelper queryHelper;

    @Reference
    private Scheduler scheduler;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Override
    public void initialize(Config config) throws PersistenceException, RepositoryException {
        Workspace workspace = config.getWorkspace();

        if (workspace.isInitialized()) {
            log.warn("Refusing to re-initialize an already initialized Bulk Workflow Manager.");
        } else {
            workspace.getRunner().initialize(queryHelper, config);
        }
    }

    @Override
    public final void start(Config config) throws PersistenceException {
        Workspace workspace = config.getWorkspace();

        workspace.getRunner().start(workspace);

        Runnable job = workspace.getRunner().getRunnable(config);
        ScheduleOptions options = workspace.getRunner().getOptions(config);

        if (options != null) {
            scheduler.schedule(job, options);
        } else {
            job.run();
        }

        workspace.commit();
    }

    @Override
    public void stopping(Config config) throws PersistenceException {
        Workspace workspace = config.getWorkspace();
        workspace.getRunner().stopping(workspace);
        workspace.commit();
    }

    @Override
    public void stop(Config config) throws PersistenceException {
        Workspace workspace = config.getWorkspace();

        scheduler.unschedule(workspace.getJobName());
        workspace.getRunner().stop(workspace);
        workspace.commit();
    }

    @Override
    public void resume(Config config) throws PersistenceException {
        start(config);
    }

    public void complete(Config config) throws PersistenceException {
        Workspace workspace = config.getWorkspace();

        scheduler.unschedule(workspace.getJobName());
        workspace.getRunner().complete(workspace);
        workspace.commit();
    }

    @Deactivate
    protected final void deactivate(final Map<String, String> args) {
        ResourceResolver adminResourceResolver = null;
        try {
            adminResourceResolver = resourceResolverFactory.getServiceResourceResolver(AUTH_INFO);
            final Resource root = adminResourceResolver.getResource(BULK_WORKFLOW_MANAGER_PAGE_FOLDER_PATH);

            if (root == null) {
                return;
            }

            final ConfigResourceVisitor visitor = new ConfigResourceVisitor();
            visitor.accept(root);

            final List<Resource> configs = visitor.getConfigs();
            for (Resource config : configs) {
                ModifiableValueMap properties = config.getChild(Workspace.NN_WORKSPACE).adaptTo(ModifiableValueMap.class);

                if (StringUtils.equals(Status.RUNNING.name(), properties.get(Workspace.PN_STATUS, String.class))) {
                    properties.put(Workspace.PN_STATUS, Status.STOPPED.name());
                    properties.put(Workspace.PN_SUB_STATUS, SubStatus.DEACTIVATED.name());
                }
            }

            if (root.getResourceResolver().hasChanges()) {
                root.getResourceResolver().commit();
            }
        } catch (LoginException e) {
            log.error("Could not obtain resource resolver for finding stopped Bulk Workflow jobs", e);
        } catch (PersistenceException e) {
            log.error("Could not resume bulk workflow manager configuration", e);
        } finally {
            if (adminResourceResolver != null) {
                adminResourceResolver.close();
            }
        }
    }
}
