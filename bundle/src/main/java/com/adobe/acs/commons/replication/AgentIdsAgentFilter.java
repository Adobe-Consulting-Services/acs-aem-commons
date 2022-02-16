/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
package com.adobe.acs.commons.replication;

import com.day.cq.replication.Agent;
import com.day.cq.replication.AgentFilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class AgentIdsAgentFilter implements AgentFilter {

    private final List<String> agentIds;

    public AgentIdsAgentFilter(List<String> agentIds) {
        this.agentIds = Optional.ofNullable(agentIds)
                .map(list -> (List<String>) new ArrayList<>(list))
                .orElse(Collections.emptyList());
    }

    public boolean isIncluded(Agent agent) {
        if (agentIds.isEmpty()) {
            return true;
        } else {
            return agentIds.contains(agent.getId());
        }
    }
}
