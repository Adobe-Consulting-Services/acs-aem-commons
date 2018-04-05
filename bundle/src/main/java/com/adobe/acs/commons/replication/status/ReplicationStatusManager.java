/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
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

package com.adobe.acs.commons.replication.status;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import aQute.bnd.annotation.ProviderType;

import javax.jcr.RepositoryException;

import java.util.Calendar;

@ProviderType
public interface ReplicationStatusManager {

    /**
     * Replication Status Enum
     *
     *  ACTIVATED: Marks resource as "Activated"
     *  DEACTIVATED: Marks resource as "Deactivated"
     *  CLEAR: Removes Replication status properties from Resource along with cq:ReplicationStatus mixin when
     *  applicable.
     */
    enum Status {
        ACTIVATED,
        DEACTIVATED,
        CLEAR
    }

    /**
     * Returns the Resource responsible for tracking replication properties for a given path. 
     * <p>
     * Pages and Assets return their respective content resource while any other path returns itself
     *
     * @param path             The path to retrieve the resource for.
     * @param resourceResolver The resource resolver must have access to read the specified path.
     */
    Resource getReplicationStatusResource(String path, ResourceResolver resourceResolver);
    
    /**
     * Marks the resources at the provides paths with the parameterized replication status.
     * <p>
     * Only resources that are of the OSGi Property parameterized "node-types" are candidates for Replication Status
     * updates. All other resources will be quietly skipped.
     *
     * @param resourceResolver The resource resolver must have access to modify all of target resources.
     * @param replicatedBy     name to set the last replicated property to. If value is null then a value of "Unknown" is used.
     * @param replicatedAt     date to set the last replicated date to. If value is null then the current time is used.
     * @param status           ACTIVATE | DEACTIVATE | CLEAR (Clear removes all replication properties and the
     *                         cq:ReplicationStatus mixin when possible)
     * @param paths            The paths to update.
     * @throws RepositoryException
     * @throws PersistenceException
     */
    void setReplicationStatus(ResourceResolver resourceResolver, String replicatedBy, Calendar replicatedAt,
                              Status status, String... paths) throws RepositoryException, PersistenceException;

    /**
     * Marks the resources at the provides paths with the parameterized replication status.
     * <p>
     * Only resources that are of the OSGi Property parameterized "node-types" are candidates for Replication Status
     * updates. All other resources will be quietly skipped.
     *
     * @param resourceResolver The resource resolver must have access to modify all of target resources.
     * @param replicatedBy     name to set the last replicated property to. If value is null then a value of "Unknown" is used.
     * @param replicatedAt     date to set the last replicated date to.  If value is null then the current time is used.
     * @param status           ACTIVATE | DEACTIVATE | CLEAR (Clear removes all replication properties and the
     *                         cq:ReplicationStatus mixin when possible)
     * @param resources        The resources to update.
     * @throws RepositoryException
     * @throws PersistenceException
     */
    void setReplicationStatus(ResourceResolver resourceResolver, String replicatedBy, Calendar replicatedAt,
                              Status status, Resource... resources) throws RepositoryException, PersistenceException;

    /**
     * Clear the replication status from the provides resources
     *
     * Only resources that are of the OSGi Property parameterized "node-types" are candidates for Replication Status
     * updates. All other resources will be quietly skipped.
     *
     * @param resourceResolver The resource resolver must have access to modify all of target resources.
     * @param resources The resources to update.
     * @throws RepositoryException
     * @throws PersistenceException
     */
    void clearReplicationStatus(ResourceResolver resourceResolver, final Resource... resources) throws
            RepositoryException, PersistenceException;
}
