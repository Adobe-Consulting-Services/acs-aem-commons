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

import java.util.List;

import aQute.bnd.annotation.ConsumerType;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

@ConsumerType
public interface ContentModificationStep {

    String STEP_NAME = "contentModification.name";

    /**
     * Identify the resources which should be considered for modification
     * @param resource the root resource of the subtree which should be considered
     * @return the resources which are going to be modified
     */
    List<Resource> identifyResources(Resource resource);


    /**
     * Modify the resource.
     *
     * Performs all relevant operations on the resource. An implementation should not persist
     * the changes, that means it should not call <code>resource.getResourceResolver().commit();</code>
     * @param resource the resource which should be modified
     */
    void performModification(Resource resource);

}
