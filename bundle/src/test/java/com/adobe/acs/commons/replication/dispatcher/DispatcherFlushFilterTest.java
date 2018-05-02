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
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DispatcherFlushFilterTest {
    private Agent agent;
    private AgentConfig agentConfig;
    private ValueMap invalidProperties;
    private ValueMap hierarchicalProperties;
    private ValueMap resourceOnlyProperties;

    @Before
    public void setUp() throws Exception {
        agent = mock(Agent.class);
        agentConfig = mock(AgentConfig.class);

        when(agent.getId()).thenReturn("mock-agent");
        when(agent.getConfiguration()).thenReturn(agentConfig);

        Map<String, Object> tmp = new HashMap<String, Object>();
        tmp.put(AgentConfig.PROTOCOL_HTTP_HEADERS, new String[] {"CQ-Action:{action}", "CQ-Handle:{path}",
                "CQ-Path: {path}"});
        hierarchicalProperties = new ValueMapDecorator(tmp);

        tmp = new HashMap<String, Object>();
        tmp.put(AgentConfig.PROTOCOL_HTTP_HEADERS, new String[] {"CQ-Action:{action}", "CQ-Handle:{path}",
                "CQ-Path: {path}", "CQ-Action-Scope: ResourceOnly"});
        resourceOnlyProperties = new ValueMapDecorator(tmp);

        tmp = new HashMap<String, Object>();
        tmp.put(AgentConfig.PROTOCOL_HTTP_HEADERS, new String[] {"Foo-Action:{action}", "Foo-Handle:{path}",
                "Foo-Path: {path}"});
        invalidProperties = new ValueMapDecorator(tmp);
    }

    @Test
    public void testIsIncluded_isAgent() throws Exception {
        final DispatcherFlushFilter filter = new DispatcherFlushFilter();

        when(agent.isEnabled()).thenReturn(true);
        when(agentConfig.getTransportURI()).thenReturn("http://localhost:80/dispatcher/invalidate.cache");
        when(agentConfig.getProperties()).thenReturn(hierarchicalProperties);
        when(agentConfig.getSerializationType()).thenReturn("flush");

        final boolean expected = true;
        final boolean actual = filter.isIncluded(agent);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testIsIncluded_disabled_flush() throws Exception {
        final DispatcherFlushFilter filter = new DispatcherFlushFilter();

        when(agent.isEnabled()).thenReturn(false);
        when(agentConfig.getTransportURI()).thenReturn("https://localhost:80/dispatcher/invalidate.cache");
        when(agentConfig.getProperties()).thenReturn(hierarchicalProperties);
        when(agentConfig.getSerializationType()).thenReturn("flush");

        final boolean expected = false;
        final boolean actual = filter.isIncluded(agent);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testIsIncluded_notflush() throws Exception {
        final DispatcherFlushFilter filter = new DispatcherFlushFilter();

        when(agent.isEnabled()).thenReturn(true);
        when(agentConfig.getTransportURI()).thenReturn("http://localhost:80/dispatcher/invalidate.cache");
        when(agentConfig.getProperties()).thenReturn(hierarchicalProperties);
        when(agentConfig.getSerializationType()).thenReturn("notflush");

        final boolean expected = false;
        final boolean actual = filter.isIncluded(agent);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testIsIncluded_enabled_invalidTransportURI() throws Exception {
        final DispatcherFlushFilter filter = new DispatcherFlushFilter();

        when(agent.isEnabled()).thenReturn(true);
        when(agentConfig.getTransportURI()).thenReturn("ftp://localhost:80/not/dispatcher");
        when(agentConfig.getProperties()).thenReturn(hierarchicalProperties);
        when(agentConfig.getSerializationType()).thenReturn("flush");

        final boolean expected = false;
        final boolean actual = filter.isIncluded(agent);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testIsIncluded_enabled_invalidHttpHeaders() throws Exception {
        final DispatcherFlushFilter filter = new DispatcherFlushFilter();

        when(agent.isEnabled()).thenReturn(true);
        when(agentConfig.getTransportURI()).thenReturn("http://localhost:80/dispatcher/invalidate.cache");
        when(agentConfig.getProperties()).thenReturn(invalidProperties);
        when(agentConfig.getSerializationType()).thenReturn("flush");

        final boolean expected = false;
        final boolean actual = filter.isIncluded(agent);

        Assert.assertEquals(expected, actual);
    }



    @Test
    public void testIsIncluded_all_hierarchicalIncluded() throws Exception {
        final DispatcherFlushFilter filter = new DispatcherFlushFilter();

        when(agent.isEnabled()).thenReturn(true);
        when(agentConfig.getTransportURI()).thenReturn("http://localhost:80/dispatcher/invalidate.cache");
        when(agentConfig.getProperties()).thenReturn(hierarchicalProperties);
        when(agentConfig.getSerializationType()).thenReturn("flush");

        final boolean expected = true;
        final boolean actual = filter.isIncluded(agent);

        Assert.assertEquals(expected, actual);
    }


    @Test
    public void testIsIncluded_all_resourceOnlyIncluded() throws Exception {
        final DispatcherFlushFilter filter = new DispatcherFlushFilter();

        when(agent.isEnabled()).thenReturn(true);
        when(agentConfig.getTransportURI()).thenReturn("http://localhost:80/dispatcher/invalidate.cache");
        when(agentConfig.getProperties()).thenReturn(resourceOnlyProperties);
        when(agentConfig.getSerializationType()).thenReturn("flush");

        final boolean expected = true;
        final boolean actual = filter.isIncluded(agent);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testIsIncluded_hierarchical_included() throws Exception {
        final DispatcherFlushFilter filter = new DispatcherFlushFilter(DispatcherFlushFilter.FlushType.Hierarchical);

        when(agent.isEnabled()).thenReturn(true);
        when(agentConfig.getTransportURI()).thenReturn("http://localhost:80/dispatcher/invalidate.cache");
        when(agentConfig.getProperties()).thenReturn(hierarchicalProperties);
        when(agentConfig.getSerializationType()).thenReturn("flush");

        final boolean expected = true;
        final boolean actual = filter.isIncluded(agent);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testIsIncluded_hierarchical_notIncluded() throws Exception {
        final DispatcherFlushFilter filter = new DispatcherFlushFilter(DispatcherFlushFilter.FlushType.Hierarchical);

        when(agent.isEnabled()).thenReturn(true);
        when(agentConfig.getTransportURI()).thenReturn("http://localhost:80/dispatcher/invalidate.cache");
        when(agentConfig.getProperties()).thenReturn(resourceOnlyProperties);
        when(agentConfig.getSerializationType()).thenReturn("flush");

        final boolean expected = false;
        final boolean actual = filter.isIncluded(agent);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testIsIncluded_resourceOnly_included() throws Exception {
        final DispatcherFlushFilter filter = new DispatcherFlushFilter(DispatcherFlushFilter.FlushType.ResourceOnly);

        when(agent.isEnabled()).thenReturn(true);
        when(agentConfig.getTransportURI()).thenReturn("http://localhost:80/dispatcher/invalidate.cache");
        when(agentConfig.getProperties()).thenReturn(resourceOnlyProperties);
        when(agentConfig.getSerializationType()).thenReturn("flush");

        final boolean expected = true;
        final boolean actual = filter.isIncluded(agent);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testIsIncluded_resourceOnly_notIncluded() throws Exception {
        final DispatcherFlushFilter filter = new DispatcherFlushFilter(DispatcherFlushFilter.FlushType.ResourceOnly);

        when(agent.isEnabled()).thenReturn(true);
        when(agentConfig.getTransportURI()).thenReturn("http://localhost:80/dispatcher/invalidate.cache");
        when(agentConfig.getProperties()).thenReturn(hierarchicalProperties);
        when(agentConfig.getSerializationType()).thenReturn("flush");

        final boolean expected = false;
        final boolean actual = filter.isIncluded(agent);

        Assert.assertEquals(expected, actual);
    }
}
