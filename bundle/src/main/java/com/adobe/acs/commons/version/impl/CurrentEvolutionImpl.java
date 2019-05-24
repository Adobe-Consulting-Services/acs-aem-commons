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

import java.util.Date;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.Resource;

import com.adobe.acs.commons.version.EvolutionEntry;

public final class CurrentEvolutionImpl extends EvolutionImplBase {

    public static final String LATEST_VERSION = "Latest";

    public CurrentEvolutionImpl(final Resource resource, final EvolutionConfig config) {
        super(resource);
        populate(config);
    }

    @Override
    public boolean isCurrent() {
        return true;
    }

    @Override
    public String getVersionName() {
        return LATEST_VERSION;
    }

    @Override
    public Date getVersionDate() {
        return new Date();
    }

    protected String getRelativeName(final Property property) throws RepositoryException {
        return EvolutionPathUtil.getLastRelativePropertyName(property.getPath());
    }

    protected String getRelativeName(final Resource resource) {
        return EvolutionPathUtil.getLastRelativeResourceName(resource.getPath());
    }

    protected EvolutionEntry createEntry(final Property property)
            throws AccessDeniedException, ItemNotFoundException, RepositoryException {
        return new CurrentEvolutionEntryImpl(property);
    }

    protected EvolutionEntry createEntry(final Resource resource) {
        return new CurrentEvolutionEntryImpl(resource);
    }
}
