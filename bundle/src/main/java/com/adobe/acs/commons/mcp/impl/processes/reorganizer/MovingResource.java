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

import javax.jcr.Session;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Generic resource/node that just gets moved as-is
 */
public class MovingResource extends MovingNode {

    public MovingResource() {
    }

    @Override
    public boolean isCopiedBeforeMove() {
        return false;
    }

    @Override
    public boolean isSupposedToBeReferenced() {
        return false;
    }

    @Override
    public boolean isAbleToHaveChildren() {
        return false;
    }

    @Override
    public void move(ReplicatorQueue replicatorQueue, ResourceResolver rr) throws IllegalAccessException, Exception {
        Session session = rr.adaptTo(Session.class);
        session.move(getSourcePath(), getDestinationPath());
        session.save();
    }    
}
