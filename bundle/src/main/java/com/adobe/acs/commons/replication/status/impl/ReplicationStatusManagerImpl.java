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

import com.adobe.acs.commons.replication.status.ReplicationStatusManager;
import com.day.cq.commons.jcr.JcrUtil;
import com.day.cq.replication.ReplicationStatus;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import java.util.Calendar;

@Component(
        label = "ACS AEM Commons - Replication Status Manager",
        description = "OSGi Service for changing the replication status of resources."
)
@Service
public class ReplicationStatusManagerImpl implements ReplicationStatusManager {
    private static final Logger log = LoggerFactory.getLogger(ReplicationStatusManagerImpl.class);

    private static final String REP_STATUS_ACTIVATE = "Activate";
    private static final String REP_STATUS_DEACTIVATE = "Deactivate";
    private static final int SAVE_THRESHOLD = 1024;

    /**
     * {@inheritDoc}
     */
    public final void setReplicationStatus(final ResourceResolver resourceResolver,
                                           final String replicatedBy,
                                           final Calendar replicatedAt,
                                           final Status status,
                                           final String... paths) throws RepositoryException, PersistenceException {
        for (final String path : paths) {
            final Resource resource = resourceResolver.getResource(path);

            if (resource == null) {
                log.warn("Requesting a replication status update for a resource that does not exist: {}", path);
                continue;
            }

            this.setReplicationStatus(resourceResolver, replicatedBy, replicatedAt, status, resource);
        }
    }

    /**
     * {@inheritDoc}
     */
    public final void setReplicationStatus(final ResourceResolver resourceResolver,
                                           final String replicatedBy,
                                           final Calendar replicatedAt,
                                           final Status status,
                                           final Resource... resources)
            throws RepositoryException, PersistenceException {
        final Session session = resourceResolver.adaptTo(Session.class);

        int count = 0;
        for (final Resource resource : resources) {

            final Node node = resource.adaptTo(Node.class);

            if (Status.CLEAR.equals(status)) {

                /* Clear replication status; Set all to null to remove properties */

                JcrUtil.setProperty(node, ReplicationStatus.NODE_PROPERTY_LAST_REPLICATED, null);
                JcrUtil.setProperty(node, ReplicationStatus.NODE_PROPERTY_LAST_REPLICATED_BY, null);
                JcrUtil.setProperty(node, ReplicationStatus.NODE_PROPERTY_LAST_REPLICATION_ACTION, null);

                if (!node.isNodeType(ReplicationStatus.NODE_TYPE)) {
                    // Remove Mixin if node is not a cq:ReplicationStatus nodeType
                    this.removeReplicationStatusMixin(node);
                }

            } else {

                /* Update status to activated or de-activated */

                final String replicationStatus = Status.ACTIVATED.equals(status) ? REP_STATUS_ACTIVATE
                        : REP_STATUS_DEACTIVATE;

                if (!node.isNodeType(ReplicationStatus.NODE_TYPE)) {
                    // Add mixin if node is not already a cq:ReplicationStatus nodeType
                    this.addReplicationStatusMixin(node);
                }

                JcrUtil.setProperty(node, ReplicationStatus.NODE_PROPERTY_LAST_REPLICATED, replicatedAt);
                JcrUtil.setProperty(node, ReplicationStatus.NODE_PROPERTY_LAST_REPLICATED_BY, replicatedBy);
                JcrUtil.setProperty(node, ReplicationStatus.NODE_PROPERTY_LAST_REPLICATION_ACTION, replicationStatus);
            }

            log.info("Updated replication status for resource [ {} ] to [ {} ].", resource.getPath(), status.name());

            if (count++ > SAVE_THRESHOLD) {
                session.save();
                count = 0;
            }
        }

        if (count > 0) {
            session.save();
        }
    }

    /**
     * {@inheritDoc}
     */
    public final void clearReplicationStatus(final ResourceResolver resourceResolver,
                                       final Resource... resources) throws RepositoryException, PersistenceException {
        this.setReplicationStatus(resourceResolver, null, null, Status.CLEAR, resources);
    }

    /**
     * Adds the cq:ReplicationStatus mixin if the node doesnt already have it or does have it as its jcr:supertype
     * already.
     *
     * @param node the node obj
     * @throws RepositoryException
     */
    private void addReplicationStatusMixin(final Node node) throws RepositoryException {
        if (!this.hasMixin(node, ReplicationStatus.NODE_TYPE) &&
                node.canAddMixin(ReplicationStatus.NODE_TYPE)) {
            node.addMixin(ReplicationStatus.NODE_TYPE);
        }
    }

    /**
     * Removes the cq:ReplicationStatus mixin from the node if it has it.
     *
     * @param node the node
     * @throws RepositoryException
     */
    private void removeReplicationStatusMixin(final Node node) throws RepositoryException {
        if (this.hasMixin(node, ReplicationStatus.NODE_TYPE)) {
            node.removeMixin(ReplicationStatus.NODE_TYPE);
        }
    }

    /**
     * Checks if the node has the mixin.
     *
     * @param node the node obj
     * @param mixin the mixin name
     * @return true if the node has the mixin
     * @throws RepositoryException
     */
    private boolean hasMixin(final Node node, final String mixin) throws RepositoryException {
        if (StringUtils.isBlank(mixin)) {
            return false;
        }

        for (final NodeType nodeType : node.getMixinNodeTypes()) {
            if (StringUtils.equals(nodeType.getName(), mixin)) {
                return true;
            }
        }

        return false;
    }
}