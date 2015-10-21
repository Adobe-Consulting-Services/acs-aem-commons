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

package com.adobe.acs.commons.replication.impl;

import com.adobe.acs.commons.replication.AgentHosts;
import com.day.cq.replication.Agent;
import com.day.cq.replication.AgentFilter;
import com.day.cq.replication.AgentManager;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Service
public class AgentHostsImpl implements AgentHosts {
    private static final Logger log = LoggerFactory.getLogger(AgentHostsImpl.class);

    private static final String DEFAULT_SCHEME = "http";
    
    @Reference
    private AgentManager agentManager;

    @Override
    public final List<String> getHosts(final AgentFilter agentFilter) {
        final List<String> hosts = new ArrayList<String>();
        final Map<String, Agent> agents = agentManager.getAgents();

        for (final Agent agent : agents.values()) {
            if (!agentFilter.isIncluded(agent)) {
                continue;
            }

            try {
                final URI uri = new URI(agent.getConfiguration().getTransportURI());

                String tmp = StringUtils.defaultIfEmpty(uri.getScheme(), DEFAULT_SCHEME) + "://" + uri.getHost();
                if (uri.getPort() > 0) {
                    tmp += ":" + uri.getPort();
                }

                hosts.add(tmp);
            } catch (URISyntaxException e) {
                log.error("Unable to extract a scheme/host/port from Agent transport URI [ {} ]",
                        agent.getConfiguration().getTransportURI());
            }
        }

        return hosts;
    }
}
