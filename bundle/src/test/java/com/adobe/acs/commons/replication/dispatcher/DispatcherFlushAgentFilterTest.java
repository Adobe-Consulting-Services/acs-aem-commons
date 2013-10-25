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
import com.day.cq.replication.AgentConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DispatcherFlushAgentFilterTest {
    private Agent agent;
    private AgentConfig agentConfig;

    @Before
    public void setUp() throws Exception {
        agent = mock(Agent.class);
        agentConfig = mock(AgentConfig.class);

        when(agent.getId()).thenReturn("mock-agent");
        when(agent.getConfiguration()).thenReturn(agentConfig);
    }

    @Test
    public void testIsIncluded_enabled_flush() throws Exception {
        final DispatcherFlushAgentFilter filter = new DispatcherFlushAgentFilter();

        when(agent.isEnabled()).thenReturn(true);
        when(agentConfig.getSerializationType()).thenReturn("flush");

        final boolean expected = true;
        final boolean actual = filter.isIncluded(agent);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testIsIncluded_disabled_flush() throws Exception {
        final DispatcherFlushAgentFilter filter = new DispatcherFlushAgentFilter();

        when(agent.isEnabled()).thenReturn(false);
        when(agentConfig.getSerializationType()).thenReturn("flush");

        final boolean expected = false;
        final boolean actual = filter.isIncluded(agent);

        Assert.assertEquals(expected, actual);
    }


    @Test
    public void testIsIncluded_enabled_notflush() throws Exception {
        final DispatcherFlushAgentFilter filter = new DispatcherFlushAgentFilter();

        when(agent.isEnabled()).thenReturn(true);
        when(agentConfig.getSerializationType()).thenReturn("notflush");

        final boolean expected = false;
        final boolean actual = filter.isIncluded(agent);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testIsIncluded_disabled_notflush() throws Exception {
        final DispatcherFlushAgentFilter filter = new DispatcherFlushAgentFilter();

        when(agent.isEnabled()).thenReturn(false);
        when(agentConfig.getSerializationType()).thenReturn("notflush");

        final boolean expected = false;
        final boolean actual = filter.isIncluded(agent);

        Assert.assertEquals(expected, actual);
    }
}
