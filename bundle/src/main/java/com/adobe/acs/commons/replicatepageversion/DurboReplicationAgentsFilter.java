/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2014 Adobe
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
package com.adobe.acs.commons.replicatepageversion;

import org.apache.commons.lang.StringUtils;

import com.day.cq.replication.Agent;
import com.day.cq.replication.AgentFilter;

public class DurboReplicationAgentsFilter implements AgentFilter {
    private static final String SERIALIZATION_TYPE = "durbo";
    private static final String REVERSE_REPLICATION = "true";
    private static final String TRANSPORT_URI = "repo";


    /**
     * Checks if the agent has a "default" serialization type.
     *
     * @param agent Agent to check
     * @return true if the Agent's serialization type is "default"
     */
    public final boolean isIncluded(Agent agent) {

        return isDurbo(agent) && isNotReverseReplicationAgent(agent)
                && isEnabled(agent) && isNotLocal(agent);
    }

    private boolean isDurbo(Agent agent) {
        return StringUtils.equals(SERIALIZATION_TYPE, agent.getConfiguration()
                .getSerializationType());
    }

    private boolean isNotReverseReplicationAgent(Agent agent) {
        return !REVERSE_REPLICATION.equals(agent.getConfiguration()
                .getProperties().get("reverseReplication", String.class));
    }

    private boolean isEnabled(Agent agent) {
        return agent.isEnabled() && agent.isValid();
    }

    private boolean isNotLocal(Agent agent) {
        return !agent.getConfiguration().getTransportURI()
                .startsWith(TRANSPORT_URI);
    }
}
