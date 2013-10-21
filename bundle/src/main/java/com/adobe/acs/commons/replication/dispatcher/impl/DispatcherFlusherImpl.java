package com.adobe.acs.commons.replication.dispatcher.impl;

import com.adobe.acs.commons.replication.dispatcher.DispatcherFlushAgentFilter;
import com.adobe.acs.commons.replication.dispatcher.DispatcherFlusher;
import com.day.cq.replication.Agent;
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

@Component(
        label = "ACS AEM Commons - Dispatcher Flusher",
        description = "Service used to issue flush requests to configured Dispatcher Flush Agents.",
        immediate = false
)
@Service
public class DispatcherFlusherImpl implements DispatcherFlusher {
    private static final Logger log = LoggerFactory.getLogger(DispatcherFlusherImpl.class);

    @Reference
    private Replicator replicator;

    @Reference
    private AgentManager agentManager;

    @Override
    public Map<Agent, ReplicationResult> flush(final ResourceResolver resourceResolver, final String... paths) throws ReplicationException {
        return this.flush(resourceResolver, ReplicationActionType.ACTIVATE, false, paths);
    }

    @Override
    public Map<Agent, ReplicationResult> flush(final ResourceResolver resourceResolver, final ReplicationActionType actionType, final boolean synchronous, final String... paths) throws ReplicationException {
        final ReplicationOptions options = new ReplicationOptions();
        final ReplicationResultListener listener = new ReplicationResultListener();

        options.setFilter(new DispatcherFlushAgentFilter());
        options.setSynchronous(synchronous);
        options.setSuppressStatusUpdate(true);
        options.setSuppressVersions(true);
        options.setListener(listener);

        for(final String path : paths) {
            replicator.replicate(resourceResolver.adaptTo(Session.class),
                    actionType, path, options);
        }

        return listener.getResults();
    }

    public Agent[] getFlushAgents() {
        final List<Agent> flushAgents = new ArrayList<Agent>();
        final DispatcherFlushAgentFilter filter = new DispatcherFlushAgentFilter();

        for(final Agent agent : agentManager.getAgents().values()) {
            if(filter.isIncluded(agent)) {
                flushAgents.add(agent);
            }
        }
        return flushAgents.toArray(new Agent[flushAgents.size()]);
    }
}



