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

import com.adobe.acs.commons.replication.dispatcher.DispatcherFlushAgentFilter;
import com.adobe.acs.commons.replication.dispatcher.DispatcherFlusher;
import com.day.cq.replication.Agent;
import com.day.cq.replication.AgentConfig;
import com.day.cq.replication.AgentFilter;
import com.day.cq.replication.AgentManager;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationOptions;
import com.day.cq.replication.Replicator;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.jcr.Session;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class DispatcherFlusherImplTest {
    @Mock
    private Replicator replicator;

    @Mock
    private AgentManager agentManager;

    final Agent agent1 = mock(Agent.class);
    final Agent agent2 = mock(Agent.class);

    final AgentConfig agentConfig1 = mock(AgentConfig.class);
    final AgentConfig agentConfig2 = mock(AgentConfig.class);

    @InjectMocks
    private DispatcherFlusher dispatcherFlusher = new DispatcherFlusherImpl();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(agent1.getId()).thenReturn("Agent 1");
        when(agent1.isEnabled()).thenReturn(true);
        when(agent1.getConfiguration()).thenReturn(agentConfig1);

        when(agent2.getId()).thenReturn("Agent 2");
        when(agent2.isEnabled()).thenReturn(true);
        when(agent2.getConfiguration()).thenReturn(agentConfig2);

        when(agent1.getId()).thenReturn("Agent 1");
        when(agent1.isEnabled()).thenReturn(true);
        when(agent1.getConfiguration()).thenReturn(agentConfig1);

        when(agent2.getId()).thenReturn("Agent 2");
        when(agent2.isEnabled()).thenReturn(true);
        when(agent2.getConfiguration()).thenReturn(agentConfig2);

        when(agentConfig1.getSerializationType()).thenReturn("flush");
        when(agentConfig2.getSerializationType()).thenReturn("notflush");

        when(agentConfig1.getTransportURI()).thenReturn("http://localhost/dispatcher/invalidate.cache");
        when(agentConfig2.getTransportURI()).thenReturn("ftp://localhost/dispatcher/invalidate.cache");

        Map<String, Object> tmp = new HashMap<String, Object>();
        tmp.put(AgentConfig.PROTOCOL_HTTP_HEADERS, new String[] {"CQ-Action:{action}", "CQ-Handle:{path}",
                "CQ-Path: {path}"});

        when(agentConfig1.getProperties()).thenReturn(new ValueMapDecorator(tmp));
        when(agentConfig2.getProperties()).thenReturn(new ValueMapDecorator(tmp));
    }

    @After
    public void tearDown() throws Exception {
        reset(replicator);
        reset(agentManager);
        reset(agent1, agent2);
        reset(agentConfig1, agentConfig2);
    }

    @Test
    public void testName() throws Exception {

    }

    @Test
    public void testFlush_replicatorAgent() throws Exception {
        final ResourceResolver resourceResolver = mock(ResourceResolver.class);
        final Session session = mock(Session.class);
        when(resourceResolver.adaptTo(Session.class)).thenReturn(session);

        final String path1 = "/content/foo";
        final String path2 = "/content/bar";

       dispatcherFlusher.flush(resourceResolver, path1, path2);

        verify(replicator, times(1)).replicate(eq(session), eq(ReplicationActionType.ACTIVATE), eq(path1),
                any(ReplicationOptions.class));

        verify(replicator, times(1)).replicate(eq(session), eq(ReplicationActionType.ACTIVATE), eq(path2),
                any(ReplicationOptions.class));

        verifyNoMoreInteractions(replicator);
    }


    @Test
    public void testFlush_replicatorAgent2() throws Exception {
        final AgentFilter agentFilter = new DispatcherFlushAgentFilter();

        final ResourceResolver resourceResolver = mock(ResourceResolver.class);
        final Session session = mock(Session.class);
        when(resourceResolver.adaptTo(Session.class)).thenReturn(session);

        final ReplicationActionType actionType = ReplicationActionType.DELETE;
        final boolean synchronous = false;

        final String path1 = "/content/foo";
        final String path2 = "/content/bar";

        final ArgumentCaptor<ReplicationOptions> argument = ArgumentCaptor.forClass(ReplicationOptions.class);

        dispatcherFlusher.flush(resourceResolver, actionType, synchronous, path1, path2);

        verify(replicator, times(1)).replicate(eq(session), eq(actionType), eq(path1),
                argument.capture());

        assertEquals(DispatcherFlushAgentFilter.class, argument.getValue().getFilter().getClass());

        verify(replicator, times(1)).replicate(eq(session), eq(actionType), eq(path2),
                argument.capture());

        assertEquals(DispatcherFlushAgentFilter.class, argument.getValue().getFilter().getClass());

        verifyNoMoreInteractions(replicator);
    }

    @Test
    public void testFlush_replicatorAgent3() throws Exception {
        final AgentFilter agentFilter = AgentFilter.DEFAULT;
        final ResourceResolver resourceResolver = mock(ResourceResolver.class);
        final Session session = mock(Session.class);
        when(resourceResolver.adaptTo(Session.class)).thenReturn(session);

        final ReplicationActionType actionType = ReplicationActionType.DELETE;
        final boolean synchronous = false;

        final String path1 = "/content/foo";
        final String path2 = "/content/bar";

        final ArgumentCaptor<ReplicationOptions> argument = ArgumentCaptor.forClass(ReplicationOptions.class);

        dispatcherFlusher.flush(resourceResolver, actionType, synchronous, agentFilter, path1,
                path2);

        verify(replicator, times(1)).replicate(eq(session), eq(actionType), eq(path1),
                argument.capture());

        assertEquals(agentFilter, argument.getValue().getFilter());

        verify(replicator, times(1)).replicate(eq(session), eq(actionType), eq(path2),
                any(ReplicationOptions.class));

        assertEquals(agentFilter, argument.getValue().getFilter());

        verifyNoMoreInteractions(replicator);
    }


    @Test
    public void testGetFlushAgents() throws Exception {
        final Map<String, Agent> agents = mock(Map.class);
        final Collection<Agent> agentValues = Arrays.asList(new Agent[]{ agent1, agent2 });

        when(agentManager.getAgents()).thenReturn(agents);

        when(agents.values()).thenReturn(agentValues);

        final Agent[] actual = dispatcherFlusher.getFlushAgents();

        assertEquals(1, actual.length);

        assertEquals("Agent 1", actual[0].getId());
    }


    @Test
    public void testGetAgents() throws Exception {
        final Map<String, Agent> agents = mock(Map.class);
        final Collection<Agent> agentValues = Arrays.asList(new Agent[]{ agent1, agent2 });

        when(agentManager.getAgents()).thenReturn(agents);

        when(agents.values()).thenReturn(agentValues);


        final Agent[] actual = dispatcherFlusher.getAgents(new DispatcherFlushAgentFilter());

        assertEquals(1, actual.length);

        assertEquals("Agent 1", actual[0].getId());
    }
}
