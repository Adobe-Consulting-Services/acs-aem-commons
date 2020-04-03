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

import com.adobe.acs.commons.fam.ThrottledTaskRunner;
import com.adobe.acs.commons.workflow.bulk.execution.BulkWorkflowRunner;
import com.adobe.acs.commons.workflow.bulk.execution.model.Config;
import com.adobe.acs.commons.workflow.bulk.execution.model.Payload;
import com.adobe.acs.commons.workflow.bulk.execution.model.Status;
import com.adobe.acs.commons.workflow.bulk.execution.model.Workspace;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowService;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.Workflow;
import com.day.cq.workflow.model.WorkflowModel;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.event.jobs.JobManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@Component
@Service
public class AEMWorkflowRunnerImpl extends AbstractAEMWorkflowRunner implements BulkWorkflowRunner {
    private static final Logger log = LoggerFactory.getLogger(AEMWorkflowRunnerImpl.class);

    @Reference
    private WorkflowService workflowService;

    @Reference
    private Scheduler scheduler;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private JobManager jobManager;

    /**
     * {@inheritDoc}
     */
    @Override
    public Runnable getRunnable(final Config config) {
        return new AEMWorkflowRunnable(config, scheduler, resourceResolverFactory, workflowService);
    }

    @Override
    public ScheduleOptions getOptions(Config config) {
        ScheduleOptions options = scheduler.NOW(-1, config.getInterval());
        options.canRunConcurrently(false);
        options.onLeaderOnly(true);
        options.name(config.getWorkspace().getJobName());

        return options;
    }

    @SuppressWarnings("squid:S00112")
    @Override
    public void complete(Workspace workspace, Payload payload) throws Exception {
        super.complete(workspace, payload);

        if (workspace.getConfig().isPurgeWorkflow()) {
            try {
                purge(payload);
            } catch (WorkflowException e) {
                throw new Exception(e);
            }
        }
    }

    @SuppressWarnings("squid:S00112")
    @Override
    public void forceTerminate(Workspace workspace, Payload payload) throws Exception {
        final WorkflowSession workflowSession =
                workflowService.getWorkflowSession(payload.getResourceResolver().adaptTo(Session.class));

        Workflow workflow = null;
        fail(workspace, payload);
        try {
            workflow = payload.getWorkflow();

            if (workflow != null) {
                if (workflow.isActive()) {
                    workflowSession.terminateWorkflow(workflow);

                    log.info("Force Terminated workflow [ {} ]", workflow.getId());

                    payload.setStatus(Status.FORCE_TERMINATED);

                    if (workspace.getConfig().isPurgeWorkflow()) {
                        purge(payload);
                    }
                } else {
                    log.warn("Trying to force terminate an inactive workflow [ {} ]", workflow.getId());
                }
            } else {
                payload.setStatus(Status.FORCE_TERMINATED);
            }
        } catch (WorkflowException e) {
            throw new Exception(e);
        }
    }

    private void purge(Payload payload) throws PersistenceException, WorkflowException {
        Workflow workflow = payload.getWorkflow();

        if (workflow != null) {
            ResourceResolver resourceResolver = payload.getResourceResolver();
            final Resource resource = resourceResolver.getResource(workflow.getId());

            if (resource != null) {
                try {
                    String path = resource.getPath();
                    resource.adaptTo(Node.class).remove();
                    log.info("Purging working instance [ {} ]", path);
                } catch (RepositoryException e) {
                    throw new PersistenceException("Unable to purge workflow instance node.", e);
                }
            } else {
                log.warn("Could not find workflow instance at [ {} ] to purge.", workflow.getId());
            }
        }
    }



    /** Runner's Runnable **/

    protected class AEMWorkflowRunnable implements Runnable {
        private final ResourceResolverFactory resourceResolverFactory;
        private final WorkflowService workflowService;
        private final Scheduler scheduler;
        private String configPath ;
        private String jobName;

        public AEMWorkflowRunnable(Config config,
                                   Scheduler scheduler,
                                   ResourceResolverFactory resourceResolverFactory,
                                   WorkflowService workflowService) {
            this.configPath = config.getPath();
            this.jobName = config.getWorkspace().getJobName();
            this.resourceResolverFactory = resourceResolverFactory;
            this.workflowService = workflowService;
            this.scheduler = scheduler;
        }

        @Override
        @SuppressWarnings({"squid:S3776", "squid:S1141"})
        public void run() {
            log.debug("Running Bulk AEM Workflow job [ {} ]", jobName);

            Resource configResource = null;
            Config config = null;
            Workspace workspace = null;

            try (ResourceResolver adminResourceResolver = resourceResolverFactory.getServiceResourceResolver(AUTH_INFO)) {
                configResource = adminResourceResolver.getResource(configPath);

                if (configResource != null) {
                    config = configResource.adaptTo(Config.class);
                }

                if (config == null) {
                    log.error("Bulk workflow process resource [ {} ] could not be found. Removing periodic job.", configPath);
                    scheduler.unschedule(jobName);
                } else {
                    workspace = config.getWorkspace();

                    if (workspace.isStopped() || workspace.isStopping()) {
                        unscheduleJob(scheduler, jobName, configResource, workspace);
                        stop(workspace);
                        return;
                    }

                    final List<Payload> priorActivePayloads = workspace.getActivePayloads();
                    final List<Payload> currentActivePayloads = new ArrayList<Payload>();

                    for (Payload payload : priorActivePayloads) {
                        log.debug("Checking status of payload [ {} ~> {} ]", payload.getPath(), payload.getPayloadPath());
                        Workflow workflow;
                        try {
                            workflow = payload.getWorkflow();

                            // First check if workflow is complete (aka not active)
                            if (workflow == null) {
                                forceTerminate(workspace, payload);
                                log.warn("Force terminated payload [ {} ] when running under a non-transient Workflow, as workflow is null.", payload.getPath());
                            } else {
                                if (!workflow.isActive()) {
                                    // Workflow has ended, so mark payload as complete
                                    payload.updateWith(workflow);
                                    complete(workspace, payload);
                                } else {
                                    // If active, check that the workflow has not expired
                                    Calendar expiresAt = Calendar.getInstance();
                                    expiresAt.setTime(workflow.getTimeStarted());
                                    expiresAt.add(Calendar.SECOND, config.getTimeout());

                                    if (!Calendar.getInstance().before(expiresAt)) {
                                        payload.updateWith(workflow);
                                        forceTerminate(workspace, payload);
                                        log.warn("Force terminated payload [ {} ~> {} ] as processing time has expired.", payload.getPath(), payload.getPayloadPath());
                                    } else {
                                        // Finally, if active and not expired, update status and let the workflow continue
                                        payload.updateWith(workflow);
                                        currentActivePayloads.add(payload);
                                    }
                                }
                            }
                        } catch (WorkflowException e) {
                            // Logged in Payload class
                            forceTerminate(workspace, payload);
                        } catch (Exception e) {
                            log.error("Error while processing payload [ {} ~> {} ]", payload.getPath(), payload.getPayloadPath());
                            forceTerminate(workspace, payload);
                        }
                    }

                    int capacity = config.getBatchSize() - currentActivePayloads.size();

                    WorkflowSession workflowSession =
                            workflowService.getWorkflowSession(adminResourceResolver.adaptTo(Session.class));

                    WorkflowModel workflowModel = workflowSession.getModel(config.getWorkflowModelId());
                    boolean dirty = false;
                    while (capacity > 0) {
                        // Bring new payloads into the active workspace
                        Payload payload = onboardNextPayload(workspace);
                        if (payload != null) {
                            log.trace("Onboarding payload [ {} ~> {} ]", payload.getPath(), payload.getPayloadPath());

                            Workflow workflow = workflowSession.startWorkflow(workflowModel,
                                    workflowSession.newWorkflowData("JCR_PATH", payload.getPayloadPath()));

                            if (workflow != null && workflow.getId() != null) {
                                // If the workflow and workflowId are not null, then there is something to update the payload w so do that.
                                payload.updateWith(workflow);
                                currentActivePayloads.add(payload);
                                capacity--;
                                dirty = true;
                            } else {
                                log.warn("The workflow or workflow ID is null, so something strange happened to it.");
                                fail(workspace, payload);
                                dirty = true;
                            }
                        } else {
                            // This means there is nothing
                            break;
                        }
                    }

                    cleanupActivePayloadGroups(workspace);

                    if (!dirty && currentActivePayloads.size() == 0) {
                        // Check if we are in a completed state for the entire workspace.
                        // We are done! Everything is processed and nothing left to onboard.
                        log.debug("No more payloads found to process. No more work to be done.");
                        complete(workspace);
                        unscheduleJob(scheduler, jobName, configResource, workspace);
                        log.info("Completed Bulk Workflow execution for [ {} ]", config.getPath());
                    }

                    if (workspace != null) {
                        workspace.commit();
                    }
                }
            } catch (Exception e) {
                String workspacePath = workspace != null ? workspace.getPath() : "unknown";
                log.error("Error processing periodic execution for job [ {} ] for workspace [ {} ]", new String[]{ jobName, workspacePath }, e);
                unscheduleJob(scheduler, jobName, configResource, workspace);
                try {
                    stop(workspace);
                } catch (PersistenceException e1) {
                    log.error("Unable to mark this workspace [ {} ] as stopped.", workspacePath, e1);
                }
            }
        }
    }
}