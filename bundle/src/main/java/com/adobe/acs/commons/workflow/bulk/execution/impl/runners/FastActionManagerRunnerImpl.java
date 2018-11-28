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

package com.adobe.acs.commons.workflow.bulk.execution.impl.runners;

import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.fam.ActionManagerFactory;
import com.adobe.acs.commons.fam.ThrottledTaskRunner;
import com.adobe.acs.commons.fam.actions.Actions;
import com.adobe.acs.commons.util.QueryHelper;
import com.adobe.acs.commons.workflow.bulk.execution.BulkWorkflowRunner;
import com.adobe.acs.commons.workflow.bulk.execution.model.Config;
import com.adobe.acs.commons.workflow.bulk.execution.model.Payload;
import com.adobe.acs.commons.workflow.bulk.execution.model.SubStatus;
import com.adobe.acs.commons.workflow.bulk.execution.model.Workspace;
import com.adobe.acs.commons.workflow.synthetic.SyntheticWorkflowModel;
import com.adobe.acs.commons.workflow.synthetic.SyntheticWorkflowRunner;
import com.day.cq.workflow.WorkflowException;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.sling.api.resource.LoginException;

@Component
@Service
public class FastActionManagerRunnerImpl extends AbstractWorkflowRunner implements BulkWorkflowRunner {
    private static final Logger log = LoggerFactory.getLogger(FastActionManagerRunnerImpl.class);

    @Reference
    private ThrottledTaskRunner throttledTaskRunner;

    @Reference
    private ResourceResolverFactory resourceResolverFactoryRef;

    @Reference
    private QueryHelper queryHelperRef;

    @Reference
    private ActionManagerFactory actionManagerFactoryRef;

    @Reference
    private SyntheticWorkflowRunner syntheticWorkflowRunnerRef;

    /**
     * {@inheritDoc}
     */
    @Override
    public final Runnable getRunnable(final Config config) {
        return new FastActionManagerRunnable(config, resourceResolverFactoryRef, queryHelperRef, actionManagerFactoryRef, syntheticWorkflowRunnerRef);
    }

    @Override
    public ScheduleOptions getOptions(Config config) {
        return null;
    }

    @Override
    public void initialize(QueryHelper queryHelper, Config config) throws PersistenceException, RepositoryException {
        Workspace workspace = config.getWorkspace();
        initialize(workspace, 0);
        workspace.commit();
    }

    @Override
    public void start(Workspace workspace) throws PersistenceException {
        if (!throttledTaskRunner.isRunning()) {
            throttledTaskRunner.resumeExecution();
        }
        super.start(workspace);
    }

    @Override
    public void stopping(Workspace workspace) throws PersistenceException {
        stop(workspace);
    }

    @Override
    public void stop(Workspace workspace) throws PersistenceException {
        throttledTaskRunner.pauseExecution();
        super.stop(workspace);
    }

    @Override
    public void stop(Workspace workspace, SubStatus subStatus) throws PersistenceException {
        throttledTaskRunner.pauseExecution();
        super.stop(workspace, subStatus);
    }

    @Override
    public void stopWithError(Workspace workspace) throws PersistenceException {
        throttledTaskRunner.pauseExecution();
        super.stopWithError(workspace);
    }

    public void complete(ResourceResolver resourceResolver, String workspacePath, ActionManager manager, int success) throws PersistenceException, RepositoryException {
        Workspace workspace = resourceResolver.getResource(workspacePath).adaptTo(Workspace.class);

        workspace.setCompleteCount(success);
        for (com.adobe.acs.commons.fam.Failure f : manager.getFailureList()) {
            workspace.addFailure(f.getNodePath(), null, f.getTime());
            workspace.incrementFailCount();
        }

        super.complete(workspace);

        manager.closeAllResolvers();
        if (actionManagerFactoryRef != null) {
            actionManagerFactoryRef.purgeCompletedTasks();
        } else {
            log.warn("Action Manager Factory reference is null. Please purge completed tasks via the JMX console.");
        }
    }

    @Override
    public void complete(Workspace workspace, Payload payload) throws Exception {
        throw new UnsupportedOperationException("FAM payloads cannot be completed as they are not tracked");
    }

    @Override
    public void run(Workspace workspace, Payload payload) {
        if (!throttledTaskRunner.isRunning()) {
            throttledTaskRunner.resumeExecution();
        }
    }

    @Override
    public void forceTerminate(Workspace workspace, Payload payload) throws Exception {
        throw new UnsupportedOperationException("FAM jobs cannot be force terminated");
    }

    /** Runner's Runnable **/

    private class FastActionManagerRunnable implements Runnable {
        private final String configPath;
        private final ResourceResolverFactory resourceResolverFactory;
        private final QueryHelper queryHelper;
        private final ActionManagerFactory actionManagerFactory;
        private final SyntheticWorkflowRunner syntheticWorkflowRunner;

        public FastActionManagerRunnable(Config config,
                                         ResourceResolverFactory resourceResolverFactory,
                                         QueryHelper queryHelper,
                                         ActionManagerFactory actionManagerFactory,
                                         SyntheticWorkflowRunner syntheticWorkflowRunner) {

            this.configPath = config.getPath();
            this.resourceResolverFactory = resourceResolverFactory;
            this.queryHelper = queryHelper;
            this.actionManagerFactory = actionManagerFactory;
            this.syntheticWorkflowRunner = syntheticWorkflowRunner;
        }

        @Override
        @SuppressWarnings({"squid:S3776", "squid:S1141", "squid:S1854"})
        public void run() {
            Resource configResource;

            try (ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(AUTH_INFO)){

                configResource = resourceResolver.getResource(configPath);

                final Config config = configResource.adaptTo(Config.class);
                final Workspace workspace = config.getWorkspace();

                if (StringUtils.isNotBlank(workspace.getActionManagerName())
                        && actionManagerFactory.hasActionManager(workspace.getActionManagerName())) {
                    log.warn("Action Manager already exists for [ {} ]", workspace.getActionManagerName());
                    return;
                }

                if (config.isUserEventData()) {
                    resourceResolver.adaptTo(Session.class).getWorkspace().getObservationManager().setUserData(config.getUserEventData());
                }

                /** Collect and initialize the workspace **/

                final List<Resource> resources;
                resources = queryHelper.findResources(resourceResolver,
                        config.getQueryType(),
                        config.getQueryStatement(),
                        config.getRelativePath());

                // Reset FAM tracking
                actionManagerFactory.purgeCompletedTasks();

                final ActionManager manager = actionManagerFactory.createTaskManager(
                        "Bulk Workflow Manager @ " + config.getPath(),
                        resourceResolver,
                        config.getBatchSize());

                final SyntheticWorkflowModel model = syntheticWorkflowRunner.getSyntheticWorkflowModel(
                        resourceResolver,
                        config.getWorkflowModelId(),
                        true);

                workspace.setTotalCount(resources.size());
                workspace.setActionManagerName(manager.getName());
                workspace.commit();

                /** Begin the work of processing the results **/

                final String workspacePath = workspace.getPath();
                final int retryCount = config.getRetryCount();
                final int retryPause = config.getInterval();
                final AtomicInteger processed = new AtomicInteger(0);
                final int total = resources.size();
                final AtomicInteger success = new AtomicInteger(0);

                resources.stream().map((resource) -> resource.getPath()).forEach((path) -> {
                    manager.deferredWithResolver((ResourceResolver r) -> {
                        try {
                            manager.setCurrentItem(path);

                            if (retryCount > 0) {
                                try {
                                    Actions.retryAll(retryCount, retryPause, Actions.startSyntheticWorkflows(model, syntheticWorkflowRunner)).accept(r, path);
                                    success.incrementAndGet();
                                } catch (Exception e) {
                                    log.warn("Could not process [ {} ] with [ " + retryCount + " ] retries", path, e);
                                    // Must throw the exception so defferedWithResolver and pick up the failure
                                    throw e;
                                }
                            } else {
                                try {
                                    Actions.startSyntheticWorkflows(model, syntheticWorkflowRunner).accept(r, path);
                                    success.incrementAndGet();
                                } catch (Exception e) {
                                    log.warn("Could not process [ {} ]", path, e);
                                    // Must throw the exception so defferedWithResolver and pick up the failure
                                    throw e;
                                }
                            }
                        } finally {
                            if (processed.incrementAndGet() == total) {
                                complete(r, workspacePath, manager, success.get());
                            }
                        }
                    });
                });
            } catch (WorkflowException | RepositoryException | LoginException | PersistenceException e) {
                log.error("Error occurred while processing Fast Action Manager Synthetic Workflow via Bulk Workflow Manager", e);
            }
        }

    }
}