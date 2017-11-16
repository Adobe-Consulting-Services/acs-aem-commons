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

import com.adobe.acs.commons.workflow.bulk.execution.model.Payload;
import com.adobe.acs.commons.workflow.bulk.execution.model.PayloadGroup;
import com.adobe.acs.commons.workflow.bulk.execution.model.Workspace;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.scheduler.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractAEMWorkflowRunner extends AbstractWorkflowRunner {
    private static final Logger log = LoggerFactory.getLogger(AbstractAEMWorkflowRunner.class);

    @SuppressWarnings("squid:S3776")
    protected Payload onboardNextPayload(Workspace workspace) {
        long start = System.currentTimeMillis();

        for (PayloadGroup payloadGroup : workspace.getActivePayloadGroups()) {
            Payload payload = payloadGroup.getNextPayload();

            if (payload != null && !payload.isOnboarded()) {
                // Onboard this payload as it hasnt been onboarded yet
                workspace.addActivePayload(payload);
                if (log.isTraceEnabled()) {
                    log.trace("Took {} ms to onboard next payload", System.currentTimeMillis() - start);
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

                if (log.isTraceEnabled()) {
                    log.trace("Onboarded [ {} ] in {} ms",
                            payload.getPayloadPath(),
                            System.currentTimeMillis() - start);
                }

                return payload;
            }
        }

        return null;
    }

    protected void cleanupActivePayloadGroups(Workspace workspace) {
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

    protected PayloadGroup onboardNextPayloadGroup(Workspace workspace, PayloadGroup payloadGroup) {
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

    @SuppressWarnings("squid:S2583")
    protected void unscheduleJob(Scheduler scheduler, String jobName, Resource configResource, Workspace workspace) {
        try {
            if (configResource != null) {
                scheduler.unschedule(jobName);
            } else {
                scheduler.unschedule(jobName);
                stopWithError(workspace);
                log.error("Removed scheduled job [ {} ] due to errors content resource could not "
                        + "be found.", jobName);
            }
        } catch (Exception e1) {
            if (scheduler != null) {
                scheduler.unschedule(jobName);
                log.error("Removed scheduled job [ {} ] due to errors and could not stop normally.", jobName, e1);
            } else {
                log.error("Scheduler is null. Could not un-schedule Job: [ {} ] ", jobName);
            }
        }
    }
}
