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
import com.adobe.acs.commons.workflow.bulk.execution.model.Status;
import com.adobe.acs.commons.workflow.bulk.execution.model.Config;
import com.adobe.acs.commons.workflow.bulk.execution.model.Payload;
import com.adobe.acs.commons.workflow.bulk.execution.model.PayloadGroup;
import com.adobe.acs.commons.workflow.bulk.execution.model.Workspace;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowService;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.Workflow;
import com.day.cq.workflow.model.WorkflowModel;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
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
public class AEMWorkflowRunnerImpl extends AbstractWorkflowRunner implements BulkWorkflowRunner {
    private static final Logger log = LoggerFactory.getLogger(AEMWorkflowRunnerImpl.class);

    @Reference
    private WorkflowService workflowService;

    @Reference
    private Scheduler scheduler;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private ThrottledTaskRunner throttledTaskRunner;

    /**
     * {@inheritDoc}
     */
    @Override
    public final Runnable getRunnable(final Config config) {
        return new AEMWorkflowRunnable(config, scheduler, resourceResolverFactory, workflowService, throttledTaskRunner);
    }

    @Override
    public ScheduleOptions getOptions(Config config) {
        ScheduleOptions options = scheduler.NOW(-1, config.getInterval());
        options.canRunConcurrently(false);
        options.onLeaderOnly(true);
        options.name(config.getWorkspace().getJobName());

        return options;
    }

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
        ResourceResolver resourceResolver = payload.getResourceResolver();

        if (workflow != null) {
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

    /**
     * Operations
     **/
    public Payload onboardNextPayload(Workspace workspace) {
        long start = System.currentTimeMillis();

        for (PayloadGroup payloadGroup : workspace.getActivePayloadGroups()) {
            Payload payload = payloadGroup.getNextPayload();

            if (payload != null && !payload.isOnboarded()) {
                // Onboard this payload as it hasnt been onboarded yet
                workspace.addActivePayload(payload);

                if (log.isDebugEnabled()) {
                    log.debug("Took {} ms to onboard next payload", System.currentTimeMillis() - start);
                }
                return payload;
            }
        }

        // No payloads in the active payload groups are eligible for onboarding


        PayloadGroup nextPayloadGroup = null;
        for (PayloadGroup payloadGroup : workspace.getActivePayloadGroups()) {
            nextPayloadGroup = onboardNextPayloadGroup(workspace, payloadGroup);

            if (nextPayloadGroup != null) {
                Payload payload = nextPayloadGroup.getNextPayload();
                if (payload == null) {
                    continue;
                    // all done! empty group
                } else {
                    workspace.addActivePayload(payload);
                }

                if (log.isDebugEnabled()) {
                    log.debug("Took {} ms to onboard next payload", System.currentTimeMillis() - start);
                }

                return payload;
            }
        }

        return null;
    }

    private void cleanupActivePayloadGroups(Workspace workspace) {
        for (PayloadGroup payloadGroup : workspace.getActivePayloadGroups()) {
            boolean removeActivePayloadGroup = true;
            for (Payload payload : workspace.getActivePayloads()) {
                if (StringUtils.startsWith(payload.getPath(), payloadGroup.getPath() + "/")) {
                    removeActivePayloadGroup = false;
                    break;
                }
            }

            if (removeActivePayloadGroup) {
                workspace.removeActivePayloadGroup(payloadGroup);
            }
        }
    }

    private PayloadGroup onboardNextPayloadGroup(Workspace workspace, PayloadGroup payloadGroup) {
        // Assumes a next group should be onboarded
        // This method is not responsible for removing items from the activePayloadGroups
        if (payloadGroup == null) {
            return null;
        }

        PayloadGroup candidatePayloadGroup = payloadGroup.getNextPayloadGroup();

        if (candidatePayloadGroup == null) {
            // payloadGroup is the last! nothing to do!
            return null;
        } else if (workspace.isActive(candidatePayloadGroup) || candidatePayloadGroup.getNextPayload() == null) {
            // Already processing the next group, use *that* group's next group
            // OR there is nothing left in that group to process...

            // recursive call..
            return onboardNextPayloadGroup(workspace, candidatePayloadGroup);
        } else {
            // Found a good payload group! has atleast 1 payload that can be onboarded
            workspace.addActivePayloadGroup(payloadGroup);
            return candidatePayloadGroup;
        }
    }


    private boolean isTransient(ResourceResolver resourceResolver, String workflowModelId) {
        Resource resource = resourceResolver.getResource(workflowModelId).getParent();
        return resource.getValueMap().get("transient", false);
    }


    /** Runner's Runnable **/

    private class AEMWorkflowRunnable implements Runnable {
        private final ResourceResolverFactory resourceResolverFactory;
        private final ThrottledTaskRunner throttledTaskRunner;
        private final WorkflowService workflowService;
        private final Scheduler scheduler;
        private String configPath ;
        private String jobName;

        public AEMWorkflowRunnable(Config config,
                                   Scheduler scheduler,
                                   ResourceResolverFactory resourceResolverFactory,
                                   WorkflowService workflowService,
                                   ThrottledTaskRunner throttledTaskRunner) {
            this.configPath = config.getPath();
            this.jobName = config.getWorkspace().getJobName();
            this.resourceResolverFactory = resourceResolverFactory;
            this.workflowService = workflowService;
            this.throttledTaskRunner = throttledTaskRunner;
            this.scheduler = scheduler;
        }

        public void run() {
            log.debug("Running Bulk AEM Workflow job [ {} ]", jobName);

            ResourceResolver adminResourceResolver = null;
            Resource configResource = null;
            Config config = null;
            Workspace workspace = null;

            try {
                adminResourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
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
                        unscheduleJob(configResource, workspace);
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
                                if (!isTransient(adminResourceResolver, config.getWorkflowModelId())) {
                                    // Something bad happened; Workflow is missing.
                                    // This could be a result of a purge.
                                    // Dont know what the status is so mark as Force Terminated
                                    forceTerminate(workspace, payload);
                                    log.warn("Force terminated payload [ {} ] when running under a non-transient Workflow, as workflow is null.", payload.getPath());
                                } else {
                                    // Transient WF.. very possible this WF instance has gone away
                                    complete(workspace, payload);
                                }
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
                    boolean isTransient = isTransient(adminResourceResolver, workflowModel.getId());
                    boolean dirty = false;
                    while (capacity > 0) {
                        // Bring new payloads into the active workspace
                        Payload payload = onboardNextPayload(workspace);
                        if (payload != null) {

                            log.debug("Onboarding payload [ {} ~> {} ]", payload.getPath(), payload.getPayloadPath());

                            Workflow workflow = workflowSession.startWorkflow(workflowModel,
                                    workflowSession.newWorkflowData("JCR_PATH", payload.getPayloadPath()));

                            if((workflow == null || workflow.getId() == null) && isTransient) {
                                // Null and transient, then mark as complete as this is a race condition where WF Is faster than this check.
                                log.debug("Payload [ {} ~> {} ] marked as complete as the Workflow is transient and can no longer be obtained", payload.getPath(), payload.getPayloadPath());
                                complete(workspace, payload);
                                dirty = true;
                            } else if ((workflow != null && workflow.getId() != null)) {
                                // If the workflow and workflowId are not null, then there is something to update the payload w so do that.
                                payload.updateWith(workflow);
                                currentActivePayloads.add(payload);
                                capacity--;
                                dirty = true;
                            } else {
                                log.warn("The WF is not transient and the WF is null, so something strange happened to it.");
                                forceTerminate(workspace, payload);
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
                        unscheduleJob(configResource, workspace);
                        log.info("Completed Bulk Workflow execution for [ {} ]", config.getPath());
                    }

                    workspace.commit();
                }
            } catch (Exception e) {
                log.error("Error processing periodic execution: {}", e);
                unscheduleJob(configResource, workspace);
            } finally {
                if (adminResourceResolver != null) {
                    adminResourceResolver.close();
                }
            }
        }

        private void unscheduleJob(Resource configResource, Workspace workspace) {
            try {
                if (configResource != null) {
                    this.scheduler.unschedule(jobName);
                } else {
                    this.scheduler.unschedule(jobName);
                    stopWithError(workspace);
                    log.error("Removed scheduled job [ {} ] due to errors content resource [ {} ] could not "
                            + "be found.", jobName, configPath);
                }
            } catch (Exception e1) {
                if (this.scheduler != null) {
                    this.scheduler.unschedule(jobName);
                    log.error("Removed scheduled job [ {} ] due to errors and could not stop normally.", jobName, e1);
                } else {
                    log.error("Scheduler is null. Could not unschedule Job: [ {} ] ", jobName);
                }
            }
        }
    }
}
