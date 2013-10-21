package com.adobe.acs.commons.replication.dispatcher;

import com.day.cq.replication.Agent;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.ReplicationResult;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.Map;

public interface DispatcherFlusher {
    public Map<Agent, ReplicationResult> flush(ResourceResolver resourceResolver, String... paths) throws ReplicationException;
    public Map<Agent, ReplicationResult> flush(ResourceResolver resourceResolver, ReplicationActionType actionType, boolean synchronous, String... paths) throws ReplicationException;
    public Agent[] getFlushAgents();
}
