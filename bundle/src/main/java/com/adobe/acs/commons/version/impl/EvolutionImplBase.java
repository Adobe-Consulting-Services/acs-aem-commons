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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.version.Evolution;
import com.adobe.acs.commons.version.EvolutionEntry;


public abstract class EvolutionImplBase implements Evolution {

    private static final Logger log = LoggerFactory.getLogger(EvolutionImplBase.class);

    private final List<EvolutionEntry> versionEntries = new ArrayList<EvolutionEntry>();

    private final Resource resource;

    protected EvolutionImplBase(final Resource resource) {
        this.resource = resource;
    }

    @Override
    public List<EvolutionEntry> getVersionEntries() {
        return versionEntries;
    }

    public Resource getResource() {
        return resource;
    }

    public ValueMap getProperties() {
        return resource.getValueMap();
    }

    protected final void populate(final EvolutionConfig config) {
        try {
            populate(resource, config, 0);
        } catch (final RepositoryException e) {
            log.warn("Could not populate Evolution", e);
        }
    }

    private void populate(final Resource resource, final EvolutionConfig config, final int depth) throws PathNotFoundException, RepositoryException {
        final ValueMap map = resource.getValueMap();
        final List<String> keys = new ArrayList<String>(map.keySet());
        Collections.sort(keys);
        for (final String key : keys) {
            final Property property = resource.adaptTo(Node.class).getProperty(key);
            final String relPath = getRelativeName(property);
            if (config.handleProperty(relPath)) {
                versionEntries.add(createEntry(property));
            }
        }

        final Iterator<Resource> iter = resource.getChildren().iterator();
        while (iter.hasNext()) {
            final Resource child = iter.next();
            final String relPath = getRelativeName(child);
            if (config.handleResource(relPath)) {
                versionEntries.add(createEntry(child));
                populate(child, config, depth + 1);
            }
        }
    }

    protected abstract String getRelativeName(Property property) throws RepositoryException;

    protected abstract EvolutionEntry createEntry(Property property)
            throws AccessDeniedException, ItemNotFoundException, RepositoryException;

    protected abstract String getRelativeName(Resource resource);

    protected abstract EvolutionEntry createEntry(Resource resource);

}
