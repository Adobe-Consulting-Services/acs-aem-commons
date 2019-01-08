/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
package com.adobe.acs.commons.mcp.impl.processes.renovator;

import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationContentFilter;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.ReplicationOptions;
import com.day.cq.replication.ReplicationStatus;
import com.day.cq.replication.Replicator;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.jcr.Session;

/**
 * Collect a list of replication events for later examination and/or replay.
 */
public class ReplicatorQueue implements Replicator {
    private Map<String, ReplicationOptions> deactivateOperations = Collections.synchronizedMap(new LinkedHashMap<>());
    private Map<String, ReplicationOptions> activateOperations = Collections.synchronizedMap(new LinkedHashMap<>());

    @Override
    public void replicate(Session session, ReplicationActionType actionType, String path) throws ReplicationException {
        replicate(session, actionType, path, null);
    }

    @Override
    public void replicate(Session session, ReplicationActionType actionType, String path, ReplicationOptions replicationOptions) throws ReplicationException {
        Map<String, ReplicationOptions> queue;
        switch (actionType) {
            case ACTIVATE:
                queue = getActivateOperations();
                break;
            case DEACTIVATE:
            case DELETE:
                queue = getDeactivateOperations();
                break;
            default:
                queue = null;
        }
        if (queue != null) {
            queue.put(path, replicationOptions);
        }
    }

    @Override
    public void replicate(Session session, ReplicationActionType replicationActionType, String[] paths, ReplicationOptions replicationOptions) throws ReplicationException {
        for (String path : paths) {
            replicate(session, replicationActionType, path, replicationOptions);
        }
    }

    @Override
    public void checkPermission(Session sn, ReplicationActionType rat, String string) throws ReplicationException {
        // no-op
    }

    @Override
    public ReplicationStatus getReplicationStatus(Session sn, String string) {
        return null;
    }

    @Override
    public Iterator<String> getActivatedPaths(Session session, String s) throws ReplicationException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * @deprecated
     */
    @Override
    @Deprecated
    public List<ReplicationContentFilter> createContentFilterChain(ReplicationAction action) {
        throw new UnsupportedOperationException("Not supported yet.");
    }    

    /**
     * @return the deactivateOperations
     */
    public Map<String, ReplicationOptions> getDeactivateOperations() {
        return deactivateOperations;
    }

    /**
     * @return the activateOperations
     */
    public Map<String, ReplicationOptions> getActivateOperations() {
        return activateOperations;
    }
}
