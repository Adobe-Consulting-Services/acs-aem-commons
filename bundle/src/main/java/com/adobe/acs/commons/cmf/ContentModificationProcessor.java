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

import aQute.bnd.annotation.ProviderType;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;

@ProviderType
public interface ContentModificationProcessor {

    /**
     * Identify the resources, which would be affected by a certain ContentModificationStep.
     *
     * @param contentModificationStep  the name of the ContentModificationStep which should be used
     * @param path determines the subtree which should be validated
     * @param resolver the resourceresolver to use
     * @return null if the ContentModificationStep is not available, or a list of resources as defined by the
     *   respective ContentModificationProcessor. Returns an empty list of no resources can be identified
     * @throws NoSuchContentModificationStepException
     */
    IdentifiedResources identifyAffectedResources(String contentModificationStep, String path, ResourceResolver resolver) throws NoSuchContentModificationStepException;

    void modifyResources(IdentifiedResources resources, ResourceResolver resolver) throws NoSuchContentModificationStepException, PersistenceException;

}
