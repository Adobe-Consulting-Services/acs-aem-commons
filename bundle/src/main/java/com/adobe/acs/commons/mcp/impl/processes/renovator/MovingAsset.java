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

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.commons.jcr.JcrUtil;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.wcm.api.NameConstants;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents an asset being moved.
 */
public class MovingAsset extends MovingNode {

    private static final Logger LOG = LoggerFactory.getLogger(MovingAsset.class);
    private static final String DEFAULT_LAST_MODIFIED_BY = "Renovator";

    @Override
    public boolean isCopiedBeforeMove() {
        return false;
    }

    @Override
    public boolean isSupposedToBeReferenced() {
        return true;
    }

    @Override
    public boolean isAbleToHaveChildren() {
        return false;
    }

    @Override
    public void move(ReplicatorQueue replicatorQueue, ResourceResolver rr) throws IllegalAccessException, MovingException {
         Session session = rr.adaptTo(Session.class);
        // Inhibits some workflows
        try {
            session.getWorkspace().getObservationManager().setUserData("changedByWorkflowProcess");
            session.move(getSourcePath(), getDestinationPath());
            session.save();
            if (Util.resourceExists(rr, getDestinationPath())) {
                Node originalAssetJcrContentNode = session
                        .getNode(getDestinationPath() + "/" + JcrConstants.JCR_CONTENT);
                if (originalAssetJcrContentNode!=null) {
                     JcrUtil.setProperty(originalAssetJcrContentNode, JcrConstants.JCR_LASTMODIFIED, new Date());
                     JcrUtil.setProperty(originalAssetJcrContentNode, JcrConstants.JCR_LAST_MODIFIED_BY,
                             DEFAULT_LAST_MODIFIED_BY);
                }
               
            }
            updateReferences(replicatorQueue, rr);
        } catch (RepositoryException e) {
            throw new MovingException(getSourcePath(), e);
        }
    }
    
    void updateReferences(ReplicatorQueue rep, ResourceResolver rr) {
        getAllReferences().forEach(ref -> updateReferences(rep, rr, ref));
    }

    void updateReferences(ReplicatorQueue rep, ResourceResolver rr, String ref) {
        Session session = rr.adaptTo(Session.class);
        try {
            session.refresh(true);
        } catch (RepositoryException e1) {
            LOG.error("RepositoryException", ref, e1);
        }
        rr.refresh();
        Resource res = rr.getResource(ref);
        ModifiableValueMap map = res.adaptTo(ModifiableValueMap.class);
        AtomicBoolean changedProperty = new AtomicBoolean(false);
        AtomicBoolean changedMultiValuedProperty = new AtomicBoolean(false);
        map.forEach((key, val) -> {
            if (val != null && val.equals(getSourcePath())) {
                map.put(key, getDestinationPath());
                changedProperty.set(true);
            } else if (val instanceof Object[]) {
                updateMultiValuedReferences(key, val, session, map, changedMultiValuedProperty, ref);
            }
        });
        
        for (Resource child : res.getChildren()) {
            if (!child.isResourceType(NameConstants.NT_PAGE)) {
                updateReferences(rep, rr, child.getPath());
            }
        }
        
        try {
            if (changedProperty.get() || changedMultiValuedProperty.get()) {
                rep.replicate(null, ReplicationActionType.ACTIVATE, ref);
            }
        } catch (ReplicationException ex) {
            LOG.error("Cannot replicate '{}'", ref, ex);
        }
    }
    
    void updateMultiValuedReferences(String key, Object val, Session session, ModifiableValueMap map, AtomicBoolean changedMultiValuedProperty, String ref) {
        Object[] valList = (Object[]) val;
        for (int index = 0; index < valList.length; index++) {
            Object itm = valList[index];
            if (itm.equals(getSourcePath())) {
                valList[index] = getDestinationPath();
                changedMultiValuedProperty.set(true);
                map.put(key, valList);
            }
        }
        if (changedMultiValuedProperty.get()) {
            try {
                session.getWorkspace().getObservationManager().setUserData("changedByWorkflowProcess");
                session.refresh(true);
                session.save();
            } catch (RepositoryException e) {
                LOG.error("RepositoryException", ref, e);
            }
        }
    }
}
