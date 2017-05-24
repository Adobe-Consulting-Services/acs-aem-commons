/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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

import aQute.bnd.annotation.ProviderType;
import com.day.cq.replication.Agent;
import com.day.cq.replication.AgentConfig;
import com.day.cq.replication.AgentFilter;
import org.apache.commons.lang3.StringUtils;

/**
 * Replication Agent Filter used to identify AEM Publish Replication agents.
 */
@ProviderType
public class AemPublishAgentFilter implements AgentFilter {

    public static final AemPublishAgentFilter AEM_PUBLISH_AGENT_FILTER = new AemPublishAgentFilter();

    private static final String SERIALIZATION_TYPE = "durbo";
    private static final String TRANSPORT_PATH = "/bin/receive";

    private AemPublishAgentFilter() {
        
    }
    
    /**
     * Checks if the @agent is considered an active AEM Publish Agent (Serialization Type ~> Default and is enabled).
     *
     * @param agent the agent to test
     * @return true is is considered an enabled AEM Publish agent
     */
    @Override
    public final boolean isIncluded(final Agent agent) {
        final AgentConfig agentConfig = agent.getConfiguration();

        return agentConfig.isEnabled()
                && !agentConfig.usedForReverseReplication()
                && SERIALIZATION_TYPE.equalsIgnoreCase(agentConfig.getSerializationType())
                && StringUtils.contains(agentConfig.getTransportURI(), TRANSPORT_PATH);
    }
}
