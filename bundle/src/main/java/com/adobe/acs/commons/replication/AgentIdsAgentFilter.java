package com.adobe.acs.commons.replication;

import com.day.cq.replication.Agent;
import com.day.cq.replication.AgentFilter;

import java.util.List;

public class AgentIdsAgentFilter implements AgentFilter {

    private final List<String> agentIds;

    public AgentIdsAgentFilter(List<String> agentIds) {
        this.agentIds = agentIds;
    }

    public boolean isIncluded(Agent agent) {
        if (agentIds == null || agentIds.size() == 0) {
            return true;
        } else {
            return agentIds.contains(agent.getId());
        }
    }
}
