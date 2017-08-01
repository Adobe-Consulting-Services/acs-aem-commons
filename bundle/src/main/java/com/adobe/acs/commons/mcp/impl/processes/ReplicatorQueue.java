/*
 * Copyright 2017 Adobe.
 *
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
 */
package com.adobe.acs.commons.mcp.impl.processes;

import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationContentFilter;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.ReplicationOptions;
import com.day.cq.replication.ReplicationStatus;
import com.day.cq.replication.Replicator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.jcr.Session;

/**
 * Collect a list of replication events for later examination and/or replay.
 */
public class ReplicatorQueue implements Replicator {
    Map<String, ReplicationOptions> deactivateOperations = new LinkedHashMap<>();
    Map<String, ReplicationOptions> activateOperations = new LinkedHashMap<>();

    @Override
    public void replicate(Session session, ReplicationActionType actionType, String path) throws ReplicationException {
        replicate(session, actionType, path, null);
    }

    @Override
    public void replicate(Session session, ReplicationActionType actionType, String path, ReplicationOptions replicationOptions) throws ReplicationException {
        Map<String, ReplicationOptions> queue = null;
        switch (actionType) {
            case ACTIVATE:
                queue = activateOperations;
                break;
            case DEACTIVATE:
            case DELETE:
                queue = deactivateOperations;
        }
        if (queue != null) {
            queue.put(path, replicationOptions);
        }
    }

    @Override
    public void checkPermission(Session sn, ReplicationActionType rat, String string) throws ReplicationException {
    }

    @Override
    public ReplicationStatus getReplicationStatus(Session sn, String string) {
        return null;
    }

    @Override
    public List<ReplicationContentFilter> createContentFilterChain(ReplicationAction action) {
        throw new UnsupportedOperationException("Not supported yet.");
    }    
}
