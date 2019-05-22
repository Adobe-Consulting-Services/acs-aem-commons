/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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
import javax.jcr.version.Version;

import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.version.EvolutionEntry;


public final class EvolutionImpl extends EvolutionImplBase {

    private static final Logger log = LoggerFactory.getLogger(EvolutionImpl.class);

    private final Version version;

    public EvolutionImpl(final Version version, final Resource resource, final EvolutionConfig config) {
    	super(resource);
        this.version = version;
        populate(config);
    }

    @Override
    public Date getVersionDate() {
        try {
            return version.getCreated().getTime();
        } catch (final RepositoryException e) {
            log.warn("Could not get created date from version", e);
        }

        return null;
    }

    @Override
    public String getVersionName() {
        try {
            return version.getName();
        } catch (final RepositoryException e) {
            log.warn("Could not determine version name");
        }

        return "null";
    }

    @Override
    public boolean isCurrent() {
        try {
            final Version[] successors = version.getSuccessors();
            return successors == null || successors.length == 0;
        } catch (final RepositoryException e) {
            // no-op
        }

        return false;
    }

	protected String getRelativeName(final Property property) throws RepositoryException {
		return EvolutionPathUtil.getRelativePropertyName(property.getPath());
	}

	protected EvolutionEntry createEntry(final Property property)
			throws AccessDeniedException, ItemNotFoundException, RepositoryException {
		return new EvolutionEntryImpl(property, version);
	}

	protected String getRelativeName(final Resource resource) {
		return EvolutionPathUtil.getRelativeResourceName(resource.getPath());
	}

	protected EvolutionEntry createEntry(final Resource resource) {
		return new EvolutionEntryImpl(resource, version);
	}
}
