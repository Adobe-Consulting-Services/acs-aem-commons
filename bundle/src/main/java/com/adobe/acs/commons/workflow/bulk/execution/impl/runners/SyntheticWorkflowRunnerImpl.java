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
import com.adobe.acs.commons.workflow.bulk.execution.model.PayloadGroup;
import com.adobe.acs.commons.workflow.bulk.execution.model.Status;
import com.adobe.acs.commons.workflow.bulk.execution.model.Workspace;
import com.adobe.acs.commons.workflow.synthetic.SyntheticWorkflowModel;
import com.adobe.acs.commons.workflow.synthetic.SyntheticWorkflowRunner;
import com.day.cq.workflow.WorkflowException;
import org.apache.commons.collections.ListUtils;
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

import javax.jcr.Session;
import java.util.List;

@Component
@Service
public class SyntheticWorkflowRunnerImpl extends AbstractWorkflowRunner implements BulkWorkflowRunner {
    private static final Logger log = LoggerFactory.getLogger(SyntheticWorkflowRunnerImpl.class);

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private Scheduler scheduler;

    @Reference
    private SyntheticWorkflowRunner swr;

    @Reference
    private ThrottledTaskRunner throttledTaskRunner;

    @Override
    public final Runnable getRunnable(final Config config) {
        return new SyntheticWorkflowRunnable(config, scheduler, resourceResolverFactory, swr, throttledTaskRunner);
    }

    @Override
    public ScheduleOptions getOptions(Config config) {
        ScheduleOptions options = scheduler.NOW();
        options.canRunConcurrently(false);
        options.onLeaderOnly(true);
        options.name(config.getWorkspace().getJobName());

        return options;
    }

    @Override
    public void forceTerminate(Workspace workspace, Payload payload) throws PersistenceException {
        workspace.setStatus(Status.FORCE_TERMINATED);
    }

    @Override
    public void complete(Workspace workspace, Payload payload) throws Exception {
        // Remove active payload
        super.complete(workspace, payload);
        payload.setStatus(Status.COMPLETED);
    }

    @Override
    public void run(Workspace workspace, Payload payload) {
        super.run(workspace, payload);
    }


    /** Runner's Runnable **/

    private class SyntheticWorkflowRunnable implements Runnable {
        private final ResourceResolverFactory resourceResolverFactory;
        private final SyntheticWorkflowRunner syntheticWorkflowRunner;
        private final ThrottledTaskRunner throttledTaskRunner;
        private final Scheduler scheduler;
        private String configPath;

        public SyntheticWorkflowRunnable(Config config,
                                         Scheduler scheduler,
                                         ResourceResolverFactory resourceResolverFactory,
                                         SyntheticWorkflowRunner syntheticWorkflowRunner,
                                         ThrottledTaskRunner throttledTaskRunner) {
            this.configPath = config.getPath();
            this.resourceResolverFactory = resourceResolverFactory;
            this.syntheticWorkflowRunner = syntheticWorkflowRunner;
            this.throttledTaskRunner = throttledTaskRunner;
            this.scheduler = scheduler;
        }

        @Override
        @SuppressWarnings({"squid:S3776", "squid:S1141"})
        public void run() {
            Resource configResource;
            long start = System.currentTimeMillis();
            int total = 0;
            boolean stopped = false;

            try (ResourceResolver serviceResourceResolver = resourceResolverFactory.getServiceResourceResolver(AUTH_INFO)){
                configResource = serviceResourceResolver.getResource(configPath);

                final Config config = configResource.adaptTo(Config.class);
                final Workspace workspace = config.getWorkspace();

                if (workspace.isStopped()) {
                    return;
                }

                try {
                    SyntheticWorkflowModel model = syntheticWorkflowRunner.getSyntheticWorkflowModel(serviceResourceResolver, config.getWorkflowModelId(), true);

                    if (config.isUserEventData()) {
                        serviceResourceResolver.adaptTo(Session.class).getWorkspace().getObservationManager().setUserData(config.getUserEventData());
                        log.debug("Set JCR Sessions user-event-data to [ {} ]", config.getUserEventData());
                    }

                    PayloadGroup payloadGroup = null;
                    if (workspace.getActivePayloadGroups().size() > 0) {
                        payloadGroup = workspace.getActivePayloadGroups().get(0);
                    }

                    while (payloadGroup != null) {
                        List<Payload> payloads = workspace.getActivePayloads();

                        if (payloads.size() == 0) {
                            // payloads size is 0, so onboard next payload group
                            payloadGroup = onboardNextPayloadGroup(workspace, payloadGroup);

                            if (payloadGroup != null) {
                                payloads = onboardNextPayloads(workspace, payloadGroup);
                            }
                        }

                        // Safety check; if payloads comes in null then immediately break from loop as there is no work to do
                        if (payloads == null || payloads.size() == 0) {
                            break;
                        }

                        for (Payload payload : payloads) {
                            if (workspace.isStopping() || workspace.isStopped()) {
                                stop(workspace);
                                stopped = true;
                                break;
                            }

                            try {
                                if (config.isAutoThrottle()) {
                                    // Wait before starting more work
                                    throttledTaskRunner.waitForLowCpuAndLowMemory();
                                }

                                long processStart = System.currentTimeMillis();
                                swr.execute(serviceResourceResolver, payload.getPayloadPath(), model, false, false);
                                complete(workspace, payload);
                                log.info("Processed [ {} ] in {} ms", payload.getPayloadPath(), System.currentTimeMillis() - processStart);
                            } catch (WorkflowException e) {
                                fail(workspace, payload);
                                log.warn("Synthetic Workflow could not process [ {} ]", payload.getPath(), e);
                            } catch (Exception e) {
                                // Complete call failed; consider it failed
                                log.warn("Complete call on [ {} ] failed", payload.getPath(), e);
                                fail(workspace, payload);
                            }

                            total++;
                        } // end for

                        workspace.commit();

                        if (stopped) {
                            log.info("Bulk Synthetic Workflow run has been stopped.");
                            break;
                        }
                    } // end while

                    // Stop check in case a STOP request is made that breaks the while loop
                    if (!stopped) {
                        complete(workspace);
                    }

                    log.info("Grand total of [ {} ] payloads saved in {} ms", total, System.currentTimeMillis() - start);
                } catch (Exception e) {
                    log.error("Error processing Bulk Synthetic Workflow execution.", e);
                }
            } catch (Exception e) {
                log.error("Error processing Bulk Synthetic Workflow execution.", e);
            }
        }

        private PayloadGroup onboardNextPayloadGroup(Workspace workspace, PayloadGroup currentPayloadGroup) throws PersistenceException {
            PayloadGroup nextPayloadGroup = currentPayloadGroup.getNextPayloadGroup();
            workspace.removeActivePayloadGroup(currentPayloadGroup);

            if (nextPayloadGroup != null) {
                workspace.addActivePayloadGroup(nextPayloadGroup);
            }

            return nextPayloadGroup;
        }

        private List<Payload> onboardNextPayloads(Workspace workspace, PayloadGroup payloadGroup) throws PersistenceException {
            if (payloadGroup == null) {
                return ListUtils.EMPTY_LIST;
            }

            List<Payload> payloads = payloadGroup.getPayloads();
            if (payloads.size() > 0) {
                workspace.addActivePayloads(payloads);
            }

            return payloads;
        }
    }
}

