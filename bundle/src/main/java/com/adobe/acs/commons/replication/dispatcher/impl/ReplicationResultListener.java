package com.adobe.acs.commons.replication.dispatcher.impl;

import com.day.cq.replication.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ReplicationResultListener implements ReplicationListener {
    private static final Logger log = LoggerFactory.getLogger(ReplicationResultListener.class);

    final Map<Agent, ReplicationResult> results = new HashMap<Agent, ReplicationResult>();

    public void onStart(final Agent agent, final ReplicationAction action) {
    }

    public void onMessage(final ReplicationLog.Level level, final String message) {
    }

    public void onEnd(final Agent agent, final ReplicationAction action, final ReplicationResult result) {
        this.results.put(agent, result);
    }

    public void onError(final Agent agent, final ReplicationAction action, final Exception error) {
    }

    public Map<Agent, ReplicationResult> getResults() {
        return this.results;
    }
}
