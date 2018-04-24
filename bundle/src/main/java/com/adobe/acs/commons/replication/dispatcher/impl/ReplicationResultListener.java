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

package com.adobe.acs.commons.replication.dispatcher.impl;

import com.day.cq.replication.Agent;
import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationListener;
import com.day.cq.replication.ReplicationLog;
import com.day.cq.replication.ReplicationResult;

import java.util.HashMap;
import java.util.Map;

/**
 * Replication Listener that stores replication results for a series of agents.
 */
public class ReplicationResultListener implements ReplicationListener {

    private final Map<Agent, ReplicationResult> results = new HashMap<Agent, ReplicationResult>();

    public final void onStart(final Agent agent, final ReplicationAction action) {
        // no-op
    }

    public final void onMessage(final ReplicationLog.Level level, final String message) {
        // no-op
    }

    public final void onEnd(final Agent agent, final ReplicationAction action, final ReplicationResult result) {
        this.results.put(agent, result);
    }

    public final void onError(final Agent agent, final ReplicationAction action, final Exception error) {
        // no-op
    }

    /**
     * Gets the results of the Replication operation.
     *
     * @return the Mapped results between the Agent and ReplicationResult
     */
    public final Map<Agent, ReplicationResult> getResults() {
        return this.results;
    }
}
