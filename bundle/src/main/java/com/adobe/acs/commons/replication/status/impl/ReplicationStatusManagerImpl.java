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

package com.adobe.acs.commons.replication.status.impl;

import com.adobe.acs.commons.packaging.JcrPackageCoverageProgressListener;
import com.adobe.acs.commons.replication.status.ReplicationStatusManager;
import com.day.cq.commons.jcr.JcrUtil;
import com.day.cq.replication.ReplicationStatus;
import com.day.jcr.vault.packaging.JcrPackage;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

@Component(
        label = "ACS AEM Commons - Replication Status Manager",
        description = "Service for changing the replication status of resources."
)
@Service
public class ReplicationStatusManagerImpl implements ReplicationStatusManager {
    private static final Logger log = LoggerFactory.getLogger(ReplicationStatusManagerImpl.class);

    public boolean updateReplicationStatus(final ResourceResolver resourceResolver,
                                           final Status status,
                                           final JcrPackage... jcrPackages) throws RepositoryException, IOException {


        for (final JcrPackage jcrPackage : jcrPackages) {

            final JcrPackageCoverageProgressListener jcrPackageCoverageProgressListener = new
                    JcrPackageCoverageProgressListener();

            jcrPackage.getDefinition().dumpCoverage(jcrPackageCoverageProgressListener);

            final List<String> paths = jcrPackageCoverageProgressListener.getCoverage();

            this.updateReplicationStatus(
                    resourceResolver,
                    resourceResolver.getUserID(),
                    jcrPackage.getPackage().getLastWrapped(),
                    status,
                    paths.toArray(new String[paths.size()]));
        }

        return true;
    }

    public void updateReplicationStatus(final ResourceResolver resourceResolver,
                                        final String replicatedBy,
                                        final Calendar replicatedAt,
                                        final Status status,
                                        final String... paths) throws RepositoryException, PersistenceException {

        for (final String path : paths) {
            final Resource resource = resourceResolver.getResource(path);

            if(resource == null) {
                log.warn("Requesting a replication status update for a resource that does not exist: {}", path);
                continue;
            }

            this.updateReplicationStatus(resourceResolver, replicatedBy, replicatedAt, status, resource);
        }
    }

    public void updateReplicationStatus(final ResourceResolver resourceResolver,
                                        final String replicatedBy,
                                        final Calendar replicatedAt,
                                        final Status status,
                                        final Resource... resources) throws RepositoryException, PersistenceException {

        final Session session = resourceResolver.adaptTo(Session.class);

        int saveThreshold = 1024;

        int count = 0;
        for (final Resource resource : resources) {
            if(resource == null || ResourceUtil.isNonExistingResource(resource)) {
                return;
            }

            final Node node = resource.adaptTo(Node.class);

            if(!node.isNodeType(ReplicationStatus.NODE_TYPE)) {
                // Must be a cq:ReplicationStatus node type
                continue;
            }

            if (Status.CLEAR.equals(status)) {

                /* Clear replication status; Set all to null to remove properties */

                JcrUtil.setProperty(node, ReplicationStatus.NODE_PROPERTY_LAST_REPLICATED, null);
                JcrUtil.setProperty(node, ReplicationStatus.NODE_PROPERTY_LAST_REPLICATED_BY, null);
                JcrUtil.setProperty(node, ReplicationStatus.NODE_PROPERTY_LAST_REPLICATION_ACTION, null);

            } else {

                /* Update status to activated or de-activated */

                final String replicationStatus = Status.ACTIVATED.equals(status) ? REP_STATUS_ACTIVATE :
                        REP_STATUS_DEACTIVATE;

                JcrUtil.setProperty(node, ReplicationStatus.NODE_PROPERTY_LAST_REPLICATED, replicatedAt);
                JcrUtil.setProperty(node, ReplicationStatus.NODE_PROPERTY_LAST_REPLICATED_BY, replicatedBy);
                JcrUtil.setProperty(node, ReplicationStatus.NODE_PROPERTY_LAST_REPLICATION_ACTION, replicationStatus);
            }

            log.info("Updated replication status for resource [ {} ].", resource.getPath());

            if(count++ > saveThreshold) {
                session.save();
                count = 0;
            }
        }

        if(count > 0) {
            session.save();
        }
    }

    public void clearReplicationStatus(final ResourceResolver resourceResolver,
                                       final Resource... resources) throws RepositoryException, PersistenceException {
        this.updateReplicationStatus(resourceResolver, null, null, Status.CLEAR, resources);
    }

    private Calendar getJcrPackageLastReplicatedAt(final JcrPackage jcrPackage) throws RepositoryException {
        final Node node = jcrPackage.getNode().getNode(JcrConstants.JCR_CONTENT);

        if (node.hasProperty(ReplicationStatus.NODE_PROPERTY_LAST_REPLICATED)) {
            return node.getProperty(ReplicationStatus.NODE_PROPERTY_LAST_REPLICATED).getDate();
        }

        return null;
    }
}

