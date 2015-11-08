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

package com.adobe.acs.commons.replication.dispatcher.impl;

import com.adobe.acs.commons.replication.dispatcher.DispatcherFlushFilter;
import com.adobe.acs.commons.replication.dispatcher.DispatcherFlusher;
import com.day.cq.replication.Agent;
import com.day.cq.replication.AgentFilter;
import com.day.cq.replication.AgentManager;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.ReplicationOptions;
import com.day.cq.replication.ReplicationResult;
import com.day.cq.replication.Replicator;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ACS AEM Commons - Dispatcher Flusher
 * Service used to issue flush requests to enabled Dispatcher Flush Agents.
 */
@Component
@Service
public class DispatcherFlusherImpl implements DispatcherFlusher {
    private static final Logger log = LoggerFactory.getLogger(DispatcherFlusherImpl.class);

    @Reference
    private Replicator replicator;

    @Reference
    private AgentManager agentManager;

    /**
     * {@inheritDoc}
     */
    @Override
    public final Map<Agent, ReplicationResult> flush(final ResourceResolver resourceResolver, final String... paths)
            throws ReplicationException {
        return this.flush(resourceResolver, ReplicationActionType.ACTIVATE, false, paths);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Map<Agent, ReplicationResult> flush(final ResourceResolver resourceResolver,
                                                     final ReplicationActionType actionType,
                                                     final boolean synchronous,
                                                     final String... paths) throws ReplicationException {
        return this.flush(resourceResolver, actionType, synchronous, DispatcherFlushFilter.HIERARCHICAL, paths);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Map<Agent, ReplicationResult> flush(final ResourceResolver resourceResolver,
                                                     final ReplicationActionType actionType,
                                                     final boolean synchronous,
                                                     final AgentFilter agentFilter,
                                                     final String... paths) throws ReplicationException {
        final ReplicationOptions options = new ReplicationOptions();
        final ReplicationResultListener listener = new ReplicationResultListener();

        options.setFilter(agentFilter);
        options.setSynchronous(synchronous);
        options.setSuppressStatusUpdate(true);
        options.setSuppressVersions(true);
        options.setListener(listener);

        for (final String path : paths) {
            if (log.isDebugEnabled()) {
                log.debug("--------------------------------------------------------------------------------");
                log.debug("Issuing Dispatcher Flush (via AEM Replication API) request for: {}", path);
                log.debug(" > Synchronous: {}", options.isSynchronous());
                log.debug(" > Replication Action Type: {}", actionType.name());
            }

            replicator.replicate(resourceResolver.adaptTo(Session.class),
                    actionType, path, options);
        }

        return listener.getResults();
    }

    /**
     * {@inheritDoc}
     */
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



