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

import java.util.List;

/**
 * IdentifiedResources is used to hold the information about the
 * resources which have been identified by a ContentModificationStep.
 *
 */
@ProviderType
public final class IdentifiedResources {

    private final List<String> paths; // the paths only
    private final String contentModificationStep;

    public IdentifiedResources(List<String> paths, String name) {
        this.paths = paths;
        this.contentModificationStep = name;
    }


    public List<String> getPaths() {
        return paths;
    }

    /**
     * identifies the step which created this IdentifiedResource
     * @return the label of the ContentModification Step
     */
    public String getContentModificationStep() {
        return contentModificationStep;
    }

}
