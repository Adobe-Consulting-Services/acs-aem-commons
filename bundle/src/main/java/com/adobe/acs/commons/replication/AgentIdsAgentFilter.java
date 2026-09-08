/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.replication;

import com.day.cq.replication.Agent;
import com.day.cq.replication.AgentFilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An AEM {@link AgentFilter} implementation that filters replication agents 
 * based on a specified list of Agent IDs.
 * <p>
 * <strong>Note on Empty Lists:</strong> If the provided list of agent IDs is 
 * null or empty, {@link #isIncluded(Agent)} will return {@code true} for all agents. 
 * This fallback behavior prevents silent replication failures by defaulting to 
 * standard AEM replication (targeting all active agents) when no specific IDs are defined.
 */
public class AgentIdsAgentFilter implements AgentFilter {

    private static final Logger log = LoggerFactory.getLogger(AgentIdsAgentFilter.class);
    private final List<String> agentIds;

    public AgentIdsAgentFilter(List<String> agentIds) {
        this.agentIds = Optional.ofNullable(agentIds)
                .map(list -> (List<String>) new ArrayList<>(list))
                .orElse(Collections.emptyList());
        
        if (this.agentIds.isEmpty()) {
            log.debug("Initialized with an empty agent list. Default AEM behavior (allow all agents) will apply to this replication event.");
        }
    }

    public boolean isIncluded(Agent agent) {
        if (agentIds.isEmpty()) {
            return true;
        } else {
            return agentIds.contains(agent.getId());
        }
    }
}
