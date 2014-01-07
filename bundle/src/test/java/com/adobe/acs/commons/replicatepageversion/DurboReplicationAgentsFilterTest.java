package com.adobe.acs.commons.replicatepageversion;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Before;
import org.junit.Test;

import com.day.cq.replication.Agent;
import com.day.cq.replication.AgentConfig;

public class DurboReplicationAgentsFilterTest {
    private Agent agent;
    private AgentConfig agentConfig;
    private ValueMap configProperties;
    
    @Before
    public final void setup() throws Exception {
        agent = mock(Agent.class);
        agentConfig = mock(AgentConfig.class);
        when(agent.getId()).thenReturn("mock-agent");
        when(agent.getConfiguration()).thenReturn(agentConfig);
        
       
    }
    
    @Test
    public final void testIsIncluded_isDurbo() throws Exception {
        final DurboReplicationAgentsFilter filter=new DurboReplicationAgentsFilter();
        when(agentConfig.getSerializationType()).thenReturn("durbo");
        Map<String, Object> tmp = new HashMap<String, Object>();
        tmp.put("reverseReplication", "false");
        configProperties=new ValueMapDecorator(tmp);
        when(agentConfig.getTransportURI()).thenReturn("http://localhost:4503/bin/receive");
        when(agentConfig.getProperties()).thenReturn(configProperties);
        when(agent.isEnabled()).thenReturn(true);
        when(agent.isValid()).thenReturn(true);
        final boolean expected=true;
        final boolean actual=filter.isIncluded(agent);
        Assert.assertEquals(expected, actual);
    }
    @Test
    public final void testIsIncluded_isNotDurbo() throws Exception {
        final DurboReplicationAgentsFilter filter=new DurboReplicationAgentsFilter();
        when(agentConfig.getSerializationType()).thenReturn("static");
        Map<String, Object> tmp = new HashMap<String, Object>();
        tmp.put("reverseReplication", "false");
        configProperties=new ValueMapDecorator(tmp);
        when(agentConfig.getTransportURI()).thenReturn("http://localhost:4503/bin/receive");
        when(agentConfig.getProperties()).thenReturn(configProperties);
        when(agent.isEnabled()).thenReturn(true);
        when(agent.isValid()).thenReturn(true);
        final boolean expected=false;
        final boolean actual=filter.isIncluded(agent);
        Assert.assertEquals(expected, actual);
    }    
    @Test
    public final void testIsIncluded_isReverseRepAgent() throws Exception {
        final DurboReplicationAgentsFilter filter=new DurboReplicationAgentsFilter();
        when(agentConfig.getSerializationType()).thenReturn("durbo");
        Map<String, Object> tmp = new HashMap<String, Object>();
        tmp.put("reverseReplication", "true");
        configProperties=new ValueMapDecorator(tmp);
        when(agentConfig.getTransportURI()).thenReturn("http://localhost:4503/bin/receive");
        when(agentConfig.getProperties()).thenReturn(configProperties);
        when(agent.isEnabled()).thenReturn(true);
        when(agent.isValid()).thenReturn(true);
        final boolean expected=false;
        final boolean actual=filter.isIncluded(agent);
        Assert.assertEquals(expected, actual);
    }
    @Test
    public final void testIsIncluded_isNotReverseRepAgent() throws Exception {
        final DurboReplicationAgentsFilter filter=new DurboReplicationAgentsFilter();
        when(agentConfig.getSerializationType()).thenReturn("durbo");
        Map<String, Object> tmp = new HashMap<String, Object>();
        tmp.put("reverseReplication", "false");
        configProperties=new ValueMapDecorator(tmp);
        when(agentConfig.getTransportURI()).thenReturn("http://localhost:4503/bin/receive");
        when(agentConfig.getProperties()).thenReturn(configProperties);
        when(agent.isEnabled()).thenReturn(true);
        when(agent.isValid()).thenReturn(true);
        final boolean expected=true;
        final boolean actual=filter.isIncluded(agent);
        Assert.assertEquals(expected, actual);
    } 
    @Test
    public final void testIsIncluded_isNotEnabled() throws Exception {
        final DurboReplicationAgentsFilter filter=new DurboReplicationAgentsFilter();
        when(agentConfig.getSerializationType()).thenReturn("durbo");
        Map<String, Object> tmp = new HashMap<String, Object>();
        tmp.put("reverseReplication", "false");
        configProperties=new ValueMapDecorator(tmp);
        when(agentConfig.getTransportURI()).thenReturn("http://localhost:4503/bin/receive");
        when(agentConfig.getProperties()).thenReturn(configProperties);
        when(agent.isEnabled()).thenReturn(false);
        when(agent.isValid()).thenReturn(false);
        final boolean expected=false;
        final boolean actual=filter.isIncluded(agent);
        Assert.assertEquals(expected, actual);
    } 
    @Test
    public final void testIsIncluded_isEnabled() throws Exception {
        final DurboReplicationAgentsFilter filter=new DurboReplicationAgentsFilter();
        when(agentConfig.getSerializationType()).thenReturn("durbo");
        Map<String, Object> tmp = new HashMap<String, Object>();
        tmp.put("reverseReplication", "false");
        configProperties=new ValueMapDecorator(tmp);
        when(agentConfig.getTransportURI()).thenReturn("http://localhost:4503/bin/receive");
        when(agentConfig.getProperties()).thenReturn(configProperties);
        when(agent.isEnabled()).thenReturn(true);
        when(agent.isValid()).thenReturn(true);
        final boolean expected=true;
        final boolean actual=filter.isIncluded(agent);
        Assert.assertEquals(expected, actual);
    } 
    @Test
    public final void testIsIncluded_isLocal() throws Exception {
        final DurboReplicationAgentsFilter filter=new DurboReplicationAgentsFilter();
        when(agentConfig.getSerializationType()).thenReturn("durbo");
        Map<String, Object> tmp = new HashMap<String, Object>();
        tmp.put("reverseReplication", "false");
        configProperties=new ValueMapDecorator(tmp);
        when(agentConfig.getTransportURI()).thenReturn("repo://var");
        when(agentConfig.getProperties()).thenReturn(configProperties);
        when(agent.isEnabled()).thenReturn(true);
        when(agent.isValid()).thenReturn(true);
        final boolean expected=false;
        final boolean actual=filter.isIncluded(agent);
        Assert.assertEquals(expected, actual);
    } 
    @Test
    public final void testIsIncluded_isNotLocal() throws Exception {
        final DurboReplicationAgentsFilter filter=new DurboReplicationAgentsFilter();
        when(agentConfig.getSerializationType()).thenReturn("durbo");
        Map<String, Object> tmp = new HashMap<String, Object>();
        tmp.put("reverseReplication", "false");
        configProperties=new ValueMapDecorator(tmp);
        when(agentConfig.getTransportURI()).thenReturn("http://localhost:4503/bin/receive");
        when(agentConfig.getProperties()).thenReturn(configProperties);
        when(agent.isEnabled()).thenReturn(true);
        when(agent.isValid()).thenReturn(true);
        final boolean expected=true;
        final boolean actual=filter.isIncluded(agent);
        Assert.assertEquals(expected, actual);
    } 
}
