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

import com.adobe.acs.commons.fam.actions.Actions;
import com.day.cq.replication.ReplicationStatus;

import java.util.List;
import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Utility functions
 */
public class Util {
    //--- Utility functions
    private Util() {
        // Utility class has no public constructor
    }

    public static boolean resourceExists(ResourceResolver rr, String path) {
        Resource res = rr.getResource(path);
        return res != null && !Resource.RESOURCE_TYPE_NON_EXISTING.equals(res.getResourceType());
    }

    public static void waitUntilResourceFound(ResourceResolver rr, String path) throws MovingException {
        try {
            Actions.retry(10, 100, resolver -> {
                if (!resourceExists(resolver, path)) {
                    throw new RepositoryException("Resource not found: " + path);
                }
            }).accept(rr);
        } catch (Exception e) {
            throw new MovingException(path, e);
        }
    }

    public static boolean isActivated(ResourceResolver rr, String path) {
        Resource res = rr.getResource(path);
        if (res == null) {
            return false;
        }
        ReplicationStatus replicationStatus = res.adaptTo(ReplicationStatus.class);
        if (replicationStatus == null) {
            return false;
        }
        return replicationStatus.isActivated();
    }

    public static String[] listToStringArray(List<String> values) {
        return values.toArray(new String[0]);
    }

}
