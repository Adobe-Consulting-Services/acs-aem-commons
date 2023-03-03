/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
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
package com.adobe.acs.commons.mcp.impl.processes.renovator;

import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.ReplicationOptions;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.jcr.Session;

/**
 * Collect a list of replication events for later examination and/or replay.
 */
public class ReplicatorQueue {
    private Map<String, ReplicationOptions> deactivateOperations = Collections.synchronizedMap(new LinkedHashMap<>());
    private Map<String, ReplicationOptions> activateOperations = Collections.synchronizedMap(new LinkedHashMap<>());



    public void replicate(Session session, ReplicationActionType actionType, String path) throws ReplicationException {
        replicate(session, actionType, path, null);
    }

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
