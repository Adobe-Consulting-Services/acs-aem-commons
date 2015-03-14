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

import aQute.bnd.annotation.ProviderType;

import com.day.cq.replication.Agent;
import com.day.cq.replication.AgentFilter;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.ReplicationResult;

import org.apache.sling.api.resource.ResourceResolver;

import java.util.Map;

/**
 * Service used to issue Dispatcher Flush requests.
 */
@ProviderType
public interface DispatcherFlusher {

    /**
     * Issue flush replication request.
     *
     * @param resourceResolver access into repository; Must have access to the resources to flush
     * @param paths list of resources to flush
     * @return a map of the targeted flush agents and the result of the replication request
     * @throws ReplicationException
     */
    Map<Agent, ReplicationResult> flush(ResourceResolver resourceResolver, String... paths)
            throws ReplicationException;

    /**
     * Issue flush replication request.
     *
     * @param resourceResolver access into repository; Must have access to the resources to flush
     * @param actionType specifies the Replication Type that will be associated with the flush requests
     *                   (ex. Activate, Deactivate, Delete)
     * @param synchronous specifies if the Replication Request should be synchronous or asynchronous
     * @param paths list of resources to flush
     * @return a map of the targeted flush agents and the result of the replication request
     * @throws ReplicationException
     */
    Map<Agent, ReplicationResult> flush(ResourceResolver resourceResolver, ReplicationActionType actionType,
                                               boolean synchronous, String... paths) throws ReplicationException;

    /**
     * Issue flush replication request.
     *
     * @param resourceResolver access into repository; Must have access to the resources to flush
     * @param actionType specifies the Replication Type that will be associated with the flush requests
     *                   (ex. Activate, Deactivate, Delete)
     * @param synchronous specifies if the Replication Request should be synchronous or asynchronous
     * @param agentFilter filter used to specify agents to flush
     * @param paths list of resources to flush
     * @return a map of the targeted flush agents and the result of the replication request
     * @throws ReplicationException
     */
    Map<Agent, ReplicationResult> flush(ResourceResolver resourceResolver, ReplicationActionType actionType,
                                        boolean synchronous, AgentFilter agentFilter, String... paths) throws
            ReplicationException;

    /**
     * Get Replication Agents targeted by this service.
     *
     * @return a list of Replication Agents that will be targeted by this service
     */
    Agent[] getFlushAgents();

    /**
     * Get Replication Agents targeted by the provided AgentFilter.
     *
     * @param agentFilter filter used to specify agents to flush
     * @return a list of Replication Agents that will be targeted provided AgentFilter
     */
    Agent[] getAgents(AgentFilter agentFilter);
}
