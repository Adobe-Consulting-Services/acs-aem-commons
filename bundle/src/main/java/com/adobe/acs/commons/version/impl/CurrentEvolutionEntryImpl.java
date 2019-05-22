/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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
package com.adobe.acs.commons.version.impl;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.Resource;

public final class CurrentEvolutionEntryImpl extends EvolutionEntryImplBase {

    public CurrentEvolutionEntryImpl(final Resource resource) {
    	super(resource, EvolutionPathUtil.getLastDepthForPath(resource.getPath()));
    }

    public CurrentEvolutionEntryImpl(final Property property)
    		throws AccessDeniedException, ItemNotFoundException, RepositoryException {
    	super(property,	EvolutionPathUtil.getLastDepthForPath(property.getPath()));
    }

    @Override
    public String getUniqueName() {
        return getUniqueNameBase().replace("content", "node");
    }

    @Override
    public boolean isCurrent() {
        return true;
    }

    @Override
    public boolean isAdded() {
        return false;
    }

    @Override
    public boolean isWillBeRemoved() {
        return false;
    }

    @Override
    public boolean isChanged() {
        return false;
    }
}
