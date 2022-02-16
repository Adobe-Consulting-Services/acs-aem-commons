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
import com.day.cq.workflow.WorkflowService;
import com.day.cq.workflow.WorkflowSession;
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
import org.apache.sling.event.jobs.Queue;
import org.apache.sling.event.jobs.Statistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import java.util.ArrayList;
import java.util.List;

@Component
@Service(value = BulkWorkflowRunner.class)
public class AEMTransientWorkflowRunnerImpl extends AbstractAEMWorkflowRunner implements BulkWorkflowRunner {
    private static final Logger log = LoggerFactory.getLogger(AEMTransientWorkflowRunnerImpl.class);

    private static final String JOB_QUEUE_NAME = "Granite Workflow Queue";

    @Reference
    private WorkflowService workflowService;

    @Reference
    private Scheduler scheduler;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private ThrottledTaskRunner throttledTaskRunner;

    @Reference
    private JobManager jobManager;

    /**
     * {@inheritDoc}
     */
    @Override
    public Runnable getRunnable(final Config config) {
        return new AEMTransientWorkflowRunnable(config, scheduler, resourceResolverFactory, workflowService, throttledTaskRunner, jobManager);
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
    }

    @Override
    public void forceTerminate(Workspace workspace, Payload payload) throws Exception {
        log.info("Cannot force terminate Transient Workflow for [ {} ]", payload.getPayloadPath());
    }

    /** Runner's Runnable **/

    private class AEMTransientWorkflowRunnable implements Runnable {
        private final ResourceResolverFactory resourceResolverFactory;
        private final ThrottledTaskRunner throttledTaskRunner;
        private final WorkflowService workflowService;
        private final Scheduler scheduler;
        private final JobManager jobManager;
        private String configPath ;
        private String jobName;

        public AEMTransientWorkflowRunnable(Config config,
                                            Scheduler scheduler,
                                            ResourceResolverFactory resourceResolverFactory,
                                            WorkflowService workflowService,
                                            ThrottledTaskRunner throttledTaskRunner,
                                            JobManager jobManager) {
            this.configPath = config.getPath();
            this.jobName = config.getWorkspace().getJobName();
            this.resourceResolverFactory = resourceResolverFactory;
            this.workflowService = workflowService;
            this.throttledTaskRunner = throttledTaskRunner;
            this.scheduler = scheduler;
            this.jobManager = jobManager;
        }

        @SuppressWarnings("squid:S3776")
        public void run() {
            log.debug("Running Bulk AEM Transient Workflow job [ {} ]", jobName);

            Resource configResource = null;
            Config config = null;
            Workspace workspace = null;

            try (ResourceResolver serviceResourceResolver = resourceResolverFactory.getServiceResourceResolver(AUTH_INFO)){
                configResource = serviceResourceResolver.getResource(configPath);

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

                    long capacity = getCapacity(config);
                    log.debug("Transient workflow capacity of [ {} ] for batch size [ {} ]", capacity, config.getBatchSize());

                    int complete = 0;
                    for (Payload payload : priorActivePayloads) {
                        if (complete++ < capacity) {
                            // Mark the #capacity complete, so we can onboard #capacity mode
                            // Because this is Transient we cant check on actual jobs
                            log.trace("Marked [ {} ] as complete", payload.getPayloadPath());
                            complete(workspace, payload);
                        } else {
                            break;
                        }
                    }

                    WorkflowSession workflowSession =
                            workflowService.getWorkflowSession(serviceResourceResolver.adaptTo(Session.class));
                    WorkflowModel workflowModel = workflowSession.getModel(config.getWorkflowModelId());

                    boolean dirty = false;
                    while (capacity > 0) {

                        if (config.isAutoThrottle()) {
                            throttledTaskRunner.waitForLowCpuAndLowMemory();
                        }

                        // Bring new payloads into the active workspace
                        Payload payload = onboardNextPayload(workspace);
                        if (payload != null) {
                            log.debug("Onboarding payload w/ Transient WF [ {} ~> {} ]", payload.getPath(), payload.getPayloadPath());

                            workflowSession.startWorkflow(workflowModel,
                                    workflowSession.newWorkflowData("JCR_PATH", payload.getPayloadPath()));

                            payload.setStatus(Status.RUNNING);
                            currentActivePayloads.add(payload);
                            capacity--;
                            dirty = true;
                        } else {
                            // This means there is nothing left to onboard
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

                    workspace.commit();
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

        private long getCapacity(Config config) {
            final Queue queue = jobManager.getQueue(JOB_QUEUE_NAME);

            if (queue != null) {
                final Statistics statistics = queue.getStatistics();
                return config.getBatchSize() - statistics.getNumberOfJobs();
            } else {
                log.warn("Could not locate Job Queue named [ {} ] - this often happens on first run when no jobs have been added to the queue.", JOB_QUEUE_NAME);
                return config.getBatchSize();
            }

        }
    }
}