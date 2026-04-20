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
package com.adobe.acs.commons.replication.dispatcher.impl;

import com.adobe.acs.commons.replication.dispatcher.DispatcherFlushRules;
import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.ReplicationOptions;
import org.apache.sling.discovery.DiscoveryService;
import org.apache.sling.distribution.DistributionRequestType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import static org.apache.sling.distribution.event.DistributionEventProperties.DISTRIBUTION_PATHS;
import static org.apache.sling.distribution.event.DistributionEventProperties.DISTRIBUTION_TYPE;
import static org.apache.sling.distribution.event.DistributionEventTopics.AGENT_PACKAGE_DISTRIBUTED;
import static org.osgi.service.event.EventConstants.EVENT_TOPIC;

@Component(
        immediate = true,
        service = EventHandler.class,
        property = {
                EVENT_TOPIC + "=" + AGENT_PACKAGE_DISTRIBUTED
        }
)
public class CloudDispatcherFlushRulesExecutor implements EventHandler {

    private static final Logger log = LoggerFactory.getLogger(CloudDispatcherFlushRulesExecutor.class);

    @Reference DiscoveryService discoveryService;

    @Reference
    volatile List<DispatcherFlushRules> dispatcherFlushRules;

    @Override
    public void handleEvent(Event event) {
        boolean isLeader = discoveryService.getTopology().getLocalInstance().isLeader();
        // process the OSGi event on the leader author instance
        if (isLeader) {
            final ReplicationActionType actionType;
            Object distributionType = event.getProperty(DISTRIBUTION_TYPE);
            if (distributionType == null) {
                log.warn("Skipping processing because the distribution type is null");
                return;
            } else if (distributionType instanceof String) {
                actionType = getReplicationActionType(distributionType.toString());
            } else if (distributionType instanceof DistributionRequestType) {
                actionType = getReplicationActionType(distributionType.toString());
            } else {
                log.warn("Skipping processing because the distribution type is of unsupported type: {}", distributionType.getClass().getName());
                return;
            }

            String[] distributionPaths = (String[]) event.getProperty(DISTRIBUTION_PATHS);
            if (distributionPaths == null || distributionPaths.length == 0) {
                log.warn("Skipping processing because the distribution paths are empty");
                return;
            }
            if (actionType != null) {
                executeFlushRules(actionType, Arrays.asList(distributionPaths));
            }
        }
    }

    private void executeFlushRules(ReplicationActionType actionType, List<String> distributionPaths) {
        ReplicationAction action = new ReplicationAction(actionType, distributionPaths.toArray(new String[0]), 0L, "", null);
        ReplicationOptions opts = new ReplicationOptions();
        log.debug("Executing dispatcher flush rules for distribution paths {}...", distributionPaths);
        for (DispatcherFlushRules dispatcherFlushRule : dispatcherFlushRules) {
            try {
                dispatcherFlushRule.preprocess(action, opts);
            } catch (ReplicationException e) {
                log.warn("Could not execute dispatcher flush rule for distribution paths [{}]", distributionPaths, e);
            }
        }
        log.info("Executed flush rules for resources [{}]", distributionPaths);
    }


    private ReplicationActionType getReplicationActionType(String distributionType) {
        DistributionRequestType requestType = DistributionRequestType.fromName(distributionType);
        if (DistributionRequestType.ADD.equals(requestType)) {
            return ReplicationActionType.ACTIVATE;
        } else if (DistributionRequestType.DELETE.equals(requestType)) {
            return ReplicationActionType.DEACTIVATE;
        } else if (DistributionRequestType.TEST.equals(requestType)) {
            return ReplicationActionType.TEST;
        }
        log.debug("Distribution request type {} not supported", requestType);
        return null;
    }


}
