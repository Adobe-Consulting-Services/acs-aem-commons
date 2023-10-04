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

import com.adobe.acs.commons.replication.dispatcher.DispatcherFlushFilter;
import com.adobe.acs.commons.replication.dispatcher.DispatcherFlusher;
import com.day.cq.replication.Agent;
import com.day.cq.replication.AgentFilter;
import com.day.cq.replication.AgentManager;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.ReplicationResult;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.DistributionResponse;
import org.apache.sling.distribution.Distributor;
import org.apache.sling.distribution.SimpleDistributionRequest;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceRanking;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ServiceRanking(-5000)
@Designate(ocd = CloudDispatcherFlusher.Config.class)
public class CloudDispatcherFlusher implements DispatcherFlusher {

    private static final Logger log = LoggerFactory.getLogger(CloudDispatcherFlusher.class);

    @ObjectClassDefinition
    @interface Config {
        @AttributeDefinition(description = "Agent names to trigger when executing a distribute invalidate (ex. publish, preview)")
        String[] agent_names() default {"publish"};
    }

    @Reference
    private Distributor distributor;

    @Reference
    private AgentManager agentManager;

    private String[] agentNames;

    @Activate
    protected void activate(Config config) {
        this.agentNames = config.agent_names();
    }

    @Override
    public Map<Agent, ReplicationResult> flush(ResourceResolver resourceResolver, String... paths) {
        Map<Agent, ReplicationResult> result = new HashMap<>();
        DistributionRequest distributionRequest = new SimpleDistributionRequest(DistributionRequestType.INVALIDATE, false, paths);
        for (String agentName : agentNames) {
            DistributionResponse distributionResponse = distributor.distribute(agentName, resourceResolver, distributionRequest);
            Agent agent = agentManager.getAgents().get(agentName);
            if (agent != null) {
                result.put(agent, toReplicationResult(distributionResponse));
            }
        }
        log.debug("Executed dispatcher flush for paths {}", Arrays.asList(paths));
        return result;
    }

    @Override
    public Map<Agent, ReplicationResult> flush(ResourceResolver resourceResolver, ReplicationActionType actionType, boolean synchronous, String... paths) throws ReplicationException {
        // see https://experienceleague.adobe.com/docs/experience-manager-cloud-service/content/implementing/content-delivery/caching.html?lang=en#sling-distribution
        log.warn(
                "Dispatcher flusher in cloud should use INVALIDATE distribution types from author, no custom action type and synchronous should be set, "
                        + "refactor your code to use the DispatcherFlushRules.flush(resourceResolver, paths) method"
        );
        return flush(resourceResolver, paths);
    }

    @Override
    public Map<Agent, ReplicationResult> flush(ResourceResolver resourceResolver, ReplicationActionType actionType, boolean synchronous, AgentFilter agentFilter, String... paths) throws ReplicationException {
        // see https://experienceleague.adobe.com/docs/experience-manager-cloud-service/content/implementing/content-delivery/caching.html?lang=en#sling-distribution
        log.warn(
                "Dispatcher flusher in cloud should use INVALIDATE distribution types from author, no custom action type and synchronous should be set, "
                        + "refactor your code to use the DispatcherFlushRules.flush(resourceResolver, paths) method"
        );
        return flush(resourceResolver, paths);
    }

    @Override
    public final Agent[] getFlushAgents() {
        return this.getAgents(new DispatcherFlushFilter());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Agent[] getAgents(final AgentFilter agentFilter) {
        final List<Agent> flushAgents = new ArrayList<Agent>();

        for (final Agent agent : agentManager.getAgents().values()) {
            if (agentFilter.isIncluded(agent)) {
                flushAgents.add(agent);
            }
        }
        return flushAgents.toArray(new Agent[flushAgents.size()]);
    }

    private ReplicationResult toReplicationResult(DistributionResponse distributionResponse) {
        if (distributionResponse.isSuccessful()) {
            return ReplicationResult.OK;
        }
        return new ReplicationResult(false, 500, distributionResponse.getMessage());
    }
}
