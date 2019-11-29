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
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static com.adobe.acs.commons.mcp.impl.processes.renovator.Util.resourceExists;
import static com.adobe.acs.commons.mcp.impl.processes.renovator.Util.waitUntilResourceFound;

/**
 * Represents a folder being moved
 */
public class MovingFolder extends MovingNode {
    protected static final String DEFAULT_FOLDER_TYPE = "sling:Folder";

    private static final Logger LOG = LoggerFactory.getLogger(MovingFolder.class);

    @Override
    public boolean isCopiedBeforeMove() {
        return true;
    }

    @Override
    public boolean isSupposedToBeReferenced() {
        return false;
    }

    @Override
    public boolean isAbleToHaveChildren() {
        return true;
    }

    @Override
    public void move(ReplicatorQueue replicatorQueue, ResourceResolver rr) throws MovingException {
        try {
            Session session = rr.adaptTo(Session.class);
            session.getWorkspace().getObservationManager().setUserData("changedByWorkflowProcess");
            Resource source = rr.getResource(getSourcePath());
            createMissingTargetFolders(rr, source);
            String sourceJcrContent = getSourcePath() + "/" + JcrConstants.JCR_CONTENT;
            if (resourceExists(rr, sourceJcrContent)) {
                Actions.getCurrentActionManager().deferredWithResolver(Actions.retry(5, 50, (rrr) -> {
                    if (!resourceExists(rrr, getDestinationPath() + "/" + JcrConstants.JCR_CONTENT)) {
                        waitUntilResourceFound(rrr, getDestinationPath());
                        rrr.copy(sourceJcrContent, getDestinationPath());
                        rrr.commit();
                        rrr.refresh();
                    }
                }));
            }
            replicatorQueue.replicate(session, ReplicationActionType.DEACTIVATE, getSourcePath());
        } catch (RepositoryException | ReplicationException | PersistenceException e) {
            throw new MovingException(getSourcePath(), e);
        }
    }

    private void createMissingTargetFolders(ResourceResolver rr, Resource source) throws RepositoryException, PersistenceException, MovingException {
        if (!resourceExists(rr, getDestinationPath())) {
            Actions.setCurrentItem(getSourcePath() + "->" + getDestinationPath());
            String targetParentPath = StringUtils.substringBeforeLast(getDestinationPath(), "/");
            String targetName = StringUtils.substringAfterLast(getDestinationPath(), "/");
            if (getParent() == null) {
                if (!resourceExists(rr, getDestinationPath())) {
                    createFolderNode(targetParentPath, rr);
                }
            } else {
                waitUntilResourceFound(rr, targetParentPath);
            }
            Resource destParent = rr.getResource(targetParentPath);
            LOG.info("Creating target for {}", getSourcePath());
            rr.create(destParent, targetName, getClonedProperties(source));
            rr.commit();
            rr.refresh();
        }
    }

    protected boolean createFolderNode(String folderPath, ResourceResolver r) throws RepositoryException, PersistenceException {
        Session s = r.adaptTo(Session.class);
        if (s.nodeExists(folderPath)) {
            return false;
        }

        String name = StringUtils.substringAfterLast(folderPath, "/");
        String parentPath = StringUtils.substringBeforeLast(folderPath, "/");
        createFolderNode(parentPath, r);

        s.getNode(parentPath).addNode(name, DEFAULT_FOLDER_TYPE);
        r.commit();
        r.refresh();
        return true;
    }
}
