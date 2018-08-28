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

import com.adobe.acs.commons.fam.actions.Actions;
import com.adobe.acs.commons.mcp.impl.processes.FolderRelocator;
import static com.adobe.acs.commons.mcp.impl.processes.reorganizer.Util.resourceExists;
import static com.adobe.acs.commons.mcp.impl.processes.reorganizer.Util.waitUntilResourceFound;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Represents a folder being moved
 */
public class MovingFolder extends MovingNode {

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
    public void move(ReplicatorQueue replicatorQueue, ResourceResolver rr) throws IllegalAccessException, Exception {
        Session session = rr.adaptTo(Session.class);
        session.getWorkspace().getObservationManager().setUserData("changedByWorkflowProcess");
        Resource source = rr.getResource(getSourcePath());
        if (!resourceExists(rr, getDestinationPath())) {
            Actions.setCurrentItem(getSourcePath() + "->" + getDestinationPath());
            String targetParentPath = StringUtils.substringBeforeLast(getDestinationPath(), "/");
            String targetName = StringUtils.substringAfterLast(getDestinationPath(), "/");
            waitUntilResourceFound(rr, targetParentPath);
            Resource destParent = rr.getResource(targetParentPath);
            Logger.getLogger(FolderRelocator.class.getName()).log(Level.INFO, "Creating target for {0}", getSourcePath());
            rr.create(destParent, targetName, source.getValueMap());
            rr.commit();
            rr.refresh();
        }
        String sourceJcrContent = getSourcePath() + "/jcr:content";
        if (resourceExists(rr, sourceJcrContent)) {
            Actions.getCurrentActionManager().deferredWithResolver(Actions.retry(5, 50, (rrr) -> {
                if (!resourceExists(rrr, getDestinationPath() + "/jcr:content")) {
                    waitUntilResourceFound(rrr, getDestinationPath());
                    rrr.copy(sourceJcrContent, getDestinationPath());
                    rrr.commit();
                    rrr.refresh();
                }
            }));
        }
    }
}
