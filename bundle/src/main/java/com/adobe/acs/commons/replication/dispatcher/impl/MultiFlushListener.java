package com.adobe.acs.commons.replication.dispatcher.impl;

import com.day.cq.replication.*;

import java.io.StringWriter;

public class MultiFlushListener implements ReplicationListener {
    private final StringWriter stringWriter;

    public MultiFlushListener(final StringWriter stringWriter) {
        this.stringWriter = stringWriter;
    }

    public void onStart(final Agent agent, final ReplicationAction action) {
    }

    public void onMessage(final ReplicationLog.Level level, final String message) {
    }

    public void onEnd(final Agent agent, final ReplicationAction action, final ReplicationResult result) {
        final boolean success = result.isSuccess() && result.getCode() == 200;

        this.stringWriter.append(agent.getId() + "/" + success);
    }

    public void onError(final Agent agent, final ReplicationAction action, final Exception error) {
    }



}
