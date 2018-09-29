/*
 * Copyright 2018 Adobe.
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
package com.adobe.acs.commons.mcp.impl.processes.reorganizer;

import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.wcm.api.NameConstants;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jcr.Session;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Represents an asset being moved.
 */
public class MovingAsset extends MovingNode {

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
    public void move(ReplicatorQueue replicatorQueue, ResourceResolver rr) throws IllegalAccessException, Exception {
         Session session = rr.adaptTo(Session.class);
        // Inhibits some workflows
        session.getWorkspace().getObservationManager().setUserData("changedByWorkflowProcess");
        session.move(getSourcePath(), getDestinationPath());
        session.save();
        updateReferences(replicatorQueue, rr);
    }
    
    private void updateReferences(ReplicatorQueue rep, ResourceResolver rr) {
        getAllReferences().forEach(ref -> updateReferences(rep, rr, ref));
    }

    private void updateReferences(ReplicatorQueue rep, ResourceResolver rr, String ref) {
        Resource res = rr.getResource(ref);
        ModifiableValueMap map = res.adaptTo(ModifiableValueMap.class);
        AtomicBoolean changedProperty = new AtomicBoolean(false);
        map.forEach((key,val)-> {
            if (val != null && val.equals(getSourcePath())) {
                map.put(key, getDestinationPath());
                changedProperty.set(true);
            }
        });
        
        for (Resource child : res.getChildren()) {
            if (!child.isResourceType(NameConstants.NT_PAGE)) {
                updateReferences(rep, rr, child.getPath());
            }
        }
        
        try {
            if (changedProperty.get()) {
                rep.replicate(null, ReplicationActionType.ACTIVATE, ref);
            }
        } catch (ReplicationException ex) {
            Logger.getLogger(MovingAsset.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
