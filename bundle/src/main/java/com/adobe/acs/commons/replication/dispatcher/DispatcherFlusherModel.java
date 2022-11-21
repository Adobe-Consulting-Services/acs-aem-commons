package com.adobe.acs.commons.replication.dispatcher;

import com.day.cq.replication.Agent;
import org.osgi.annotation.versioning.ProviderType;

import java.util.Collection;
import java.util.List;

@ProviderType
public interface DispatcherFlusherModel {

    String getActionType();

    Collection<String> getPaths();

    Collection<Agent> getAgents();

    boolean isFailure();

    List<String> getResults();

    /**
     * @return true if the dispatcher flusher is ready to be used.
     */
    boolean isReady();
}


