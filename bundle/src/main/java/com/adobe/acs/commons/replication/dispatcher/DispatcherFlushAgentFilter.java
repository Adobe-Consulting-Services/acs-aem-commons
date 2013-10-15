package com.adobe.acs.commons.replication.dispatcher;

import com.day.cq.replication.Agent;
import com.day.cq.replication.AgentFilter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DispatcherFlushAgentFilter implements AgentFilter {
    private static final Logger log = LoggerFactory.getLogger(DispatcherFlushAgentFilter.class);
    public static final String SERIALIZATION_TYPE = "flush";

    @Override
    public boolean isIncluded(final Agent agent) {
        if(!StringUtils.equals(SERIALIZATION_TYPE, agent.getConfiguration().getSerializationType())) {
            // Is not a Flush Agent
            return false;
        } else if(!agent.isEnabled()) {
            // Is not enabled
            log.info("Ignoring Dispatcher Flush agent [ {} ] because it is disabled.", agent.getId());
            return false;
        } else {
            // Is a flush agent and is enabled
            return true;
        }
    }
}
