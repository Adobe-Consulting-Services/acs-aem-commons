package com.adobe.acs.commons.replication.dispatcher.impl;

import com.adobe.acs.commons.replication.dispatcher.DispatcherFlushAgentFilter;
import com.adobe.acs.commons.replication.dispatcher.DispatcherFlusher;
import com.day.cq.replication.*;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolver;

import javax.jcr.Session;

@Component(
        label = "ACS AEM Commons - Dispatcher Flusher",
        description = "Service used to issue flush requests to configured Dispatcher Flush Agents.",
        immediate = false
)
@Service
public class DispatcherFlusherImpl implements DispatcherFlusher {

    @Reference
    private Replicator replicator;

    @Override
    public void flush(final ResourceResolver resourceResolver, final String... paths) throws ReplicationException {
        final ReplicationOptions opts = new ReplicationOptions();

        opts.setFilter(new DispatcherFlushAgentFilter());
        opts.setSynchronous(true);
        opts.setSuppressStatusUpdate(true);
        opts.setSuppressVersions(true);

        for(final String path : paths) {
            replicator.replicate(resourceResolver.adaptTo(Session.class),
                    ReplicationActionType.ACTIVATE, path, opts);
        }
    }
}



