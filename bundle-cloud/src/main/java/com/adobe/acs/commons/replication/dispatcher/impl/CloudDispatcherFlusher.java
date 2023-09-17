package com.adobe.acs.commons.replication.dispatcher.impl;

import com.adobe.acs.commons.replication.dispatcher.DispatcherFlushFilter;
import com.adobe.acs.commons.replication.dispatcher.DispatcherFlusher;
import com.day.cq.replication.*;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.*;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceRanking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Component
@ServiceRanking(-5000)
public class CloudDispatcherFlusher implements DispatcherFlusher {

    private static final Logger log = LoggerFactory.getLogger(CloudDispatcherFlusher.class);

    // Default agent name
    private static final String PUBLISH_AGENT_NAME = "publish";

    @Reference
    private Distributor distributor;

    @Reference
    private AgentManager agentManager;

    @Override
    public Map<Agent, ReplicationResult> flush(ResourceResolver resourceResolver, String... paths) {
        DistributionRequest distributionRequest = new SimpleDistributionRequest(DistributionRequestType.INVALIDATE, false, paths);
        DistributionResponse distributionResponse = distributor.distribute(PUBLISH_AGENT_NAME, resourceResolver, distributionRequest);
        Map<Agent, ReplicationResult> result = new HashMap<>();
        Agent agent = agentManager.getAgents().get(PUBLISH_AGENT_NAME);
        if (agent != null) {
            result.put(agent, toReplicationResult(distributionResponse));
        }
        log.debug("Executed dispatcher flush for paths {}", Arrays.asList(paths));
        return result;
    }

    private ReplicationResult toReplicationResult(DistributionResponse distributionResponse) {
        if (distributionResponse.isSuccessful()) {
            return ReplicationResult.OK;
        }
        return new ReplicationResult(false, 500, distributionResponse.getMessage());
    }

    @Override
    public Map<Agent, ReplicationResult> flush(ResourceResolver resourceResolver, ReplicationActionType actionType, boolean synchronous, String... paths) throws ReplicationException {
        // see https://experienceleague.adobe.com/docs/experience-manager-cloud-service/content/implementing/content-delivery/caching.html?lang=en#sling-distribution
        log.warn(
                "Dispatcher flusher in cloud should use INVALIDATE distribution types from author, no custom action type and synchronous should be set, " +
                        "refactor your code to use the DispatcherFlushRules.flush(resourceResolver, paths) method"
        );
        return flush(resourceResolver, paths);
    }

    @Override
    public Map<Agent, ReplicationResult> flush(ResourceResolver resourceResolver, ReplicationActionType actionType, boolean synchronous, AgentFilter agentFilter, String... paths) throws ReplicationException {
        // see https://experienceleague.adobe.com/docs/experience-manager-cloud-service/content/implementing/content-delivery/caching.html?lang=en#sling-distribution
        log.warn(
                "Dispatcher flusher in cloud should use INVALIDATE distribution types from author, no custom action type and synchronous should be set, " +
                        "refactor your code to use the DispatcherFlushRules.flush(resourceResolver, paths) method"
        );
        return flush(resourceResolver, paths);
    }

    public final Agent[] getFlushAgents() {
        return this.getAgents(new DispatcherFlushFilter());
    }

    /**
     * {@inheritDoc}
     */
    public final Agent[] getAgents(final AgentFilter agentFilter) {
        final List<Agent> flushAgents = new ArrayList<Agent>();

        for (final Agent agent : agentManager.getAgents().values()) {
            if (agentFilter.isIncluded(agent)) {
                flushAgents.add(agent);
            }
        }
        return flushAgents.toArray(new Agent[flushAgents.size()]);
    }
}
