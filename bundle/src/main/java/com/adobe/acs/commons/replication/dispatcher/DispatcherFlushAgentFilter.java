
/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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

package com.adobe.acs.commons.replication.dispatcher;

import com.day.cq.replication.Agent;
import com.day.cq.replication.AgentFilter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Replication Agent Filter used to identify Flush agents.
 */
public class DispatcherFlushAgentFilter implements AgentFilter {
    public static final String SERIALIZATION_TYPE = "flush";

    private static final Logger log = LoggerFactory.getLogger(DispatcherFlushAgentFilter.class);

    /**
     * Checks if the @agent is considered an active Flush agent (Serialization Type ~> Flush and is enabled).
     *
     * @param agent the agent to test test
     * @return true is is considered an enabled Flush agent
     */
    @Override
    public final boolean isIncluded(final Agent agent) {
        if (!StringUtils.equals(SERIALIZATION_TYPE, agent.getConfiguration().getSerializationType())) {
            // Is not a Flush Agent
            return false;
        } else if (!agent.isEnabled()) {
            // Is not enabled
            log.info("Ignoring Dispatcher Flush agent [ {} ] because it is disabled.", agent.getId());
            return false;
        } else {
            // Is a flush agent and is enabled
            return true;
        }
    }
}
