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
package com.adobe.acs.commons.cmf;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import aQute.bnd.annotation.ConsumerType;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;


/**
 * This class implements the Visitor pattern for the identifyResources() method.
 *
 */
@ConsumerType
public abstract class TraversingContentModificationStep implements ContentModificationStep {

    @Override
    public List<Resource> identifyResources(Resource rootResource) {
        List<Resource> result = new LinkedList<Resource>();
        return visit(result, rootResource);
    }

    /**
     * Visits the resource r and its sub-resources.
     * @param r
     * @return
     */
    private List<Resource> visit(List<Resource> list, Resource r) {

        /**
         * This implementation tries to avoid the list.addAll method
         */

        Resource foundResource = visitResource (r);
        if (foundResource != null) {
            list.add(foundResource);
        }
        Iterator<Resource> iter =  r.listChildren();
        while (iter.hasNext()) {
            Resource innerResource = iter.next();
            if (accept(innerResource)) {
                list = (visit (list, innerResource));
            }

        }
        return list;

    }


    /**
     * <code>visitResource</code> implements the visitor pattern and is called for every resource.
     * @param res the resource to inspect
     * @return the resource if the resource should considered be for the inclusion in the result
     *   delivered by identifyResources; null otherwise
     */
    public abstract Resource visitResource(Resource res);



    /**
     * Decides if a certain resource (and sub-resources) will be traversed. This allows to cut off subtrees, where not
     * change is expected to happen
     * @param res the resource
     * @return true if the resources and it's sub-resources should be visited.
     */
    public boolean accept(Resource res) {
        return true;
    }


}
