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
import static com.adobe.acs.commons.mcp.impl.processes.reorganizer.Util.*;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.PageManagerFactory;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.reflect.FieldUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Represents a page being moved.
 */
public class MovingPage extends MovingNode {
    PageManagerFactory pageManagerFactory;
    
    public MovingPage(PageManagerFactory pageManagerFactory) {
        this.pageManagerFactory = pageManagerFactory;
    }
    
    
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
        return true;
    }    

    @Override
    public void move(ReplicatorQueue replicatorQueue, ResourceResolver rr) throws IllegalAccessException, Exception {
        // For starters, create a page manager with a modified replicator queue
        PageManager manager = pageManagerFactory.getPageManager(rr);
        Field replicatorField = FieldUtils.getDeclaredField(manager.getClass(), "replicator", true);
        FieldUtils.writeField(replicatorField, manager, replicatorQueue);

        // Some simple transformations
        String contentPath = getSourcePath() + "/jcr:content";
        String destinationParent = StringUtils.substringBeforeLast(getDestinationPath(), "/");

        // Attempt move operation
        Actions.retry(10, 500, res -> {
            waitUntilResourceFound(res, destinationParent);
            Resource source = rr.getResource(getSourcePath());
            if (resourceExists(res, contentPath)) {
                manager.move(source,
                        getDestinationPath(),
                        getPreviousSibling(),
                        true,
                        true,
                        listToStringArray(getAllReferences()),
                        listToStringArray(getPublishedReferences()));
            } else {
                Map<String, Object> props = new HashMap<>();
                Resource parent = res.getResource(destinationParent);
                res.create(parent, source.getName(), source.getValueMap());
            }
            res.commit();
            res.refresh();

            source = rr.getResource(getSourcePath());
            if (source != null && source.hasChildren()) {
                for (Resource child : source.getChildren()) {
                    res.move(child.getPath(), getDestinationPath());
                }
                res.commit();
            }
        }).accept(rr);
    }
}
