package com.adobe.acs.commons.replication.dispatcher;

import com.day.cq.replication.ReplicationException;
import org.apache.sling.api.resource.ResourceResolver;

public interface DispatcherFlusher {
    public void flush(ResourceResolver resourceResolver, String... paths) throws ReplicationException;
}
