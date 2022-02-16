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
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.version.Evolution;
import com.adobe.acs.commons.version.EvolutionEntry;


public final class EvolutionImpl implements Evolution {

    private static final Logger log = LoggerFactory.getLogger(EvolutionImpl.class);

    private final List<EvolutionEntry> versionEntries = new ArrayList<EvolutionEntry>();
    private final Resource versionResource;
    private final Version version;
    private final EvolutionConfig config;

    public EvolutionImpl(Version version, Resource resource, EvolutionConfig config) {
        this.version = version;
        this.config = config;
        this.versionResource = resource;
        try {
            populate(versionResource, 0);
        } catch (Exception e) {
            log.warn("Could not populate Evolution", e);
        }
    }

    @Override
    public List<EvolutionEntry> getVersionEntries() {
        return Collections.unmodifiableList(versionEntries);
    }

    @Override
    public Date getVersionDate() {
        try {
            return version.getCreated().getTime();
        } catch (RepositoryException e) {
            log.warn("Could not get created date from version", e);
        }
        return null;
    }

    @Override
    public String getVersionName() {
        try {
            return version.getName();
        } catch (RepositoryException e) {
            log.warn("Could not determine version name");
        }
        return "null";
    }

    @Override
    public boolean isCurrent() {
        try {
            Version[] successors = version.getSuccessors();
            if (successors == null || successors.length == 0) {
                return true;
            }
        } catch (RepositoryException e) {
            // no-op
        }
        return false;
    }

    public Resource getResource() {
        return versionResource;
    }

    public ValueMap getProperties() {
        return versionResource.getValueMap();
    }

    private void populate(Resource r, int depth) throws PathNotFoundException, RepositoryException {
        ValueMap map = r.getValueMap();
        List<String> keys = new ArrayList<String>(map.keySet());
        Collections.sort(keys);
        for (String key : keys) {
            Property property = r.adaptTo(Node.class).getProperty(key);
            String relPath = EvolutionPathUtil.getRelativePropertyName(property.getPath());
            if (config.handleProperty(relPath)) {
                versionEntries.add(new EvolutionEntryImpl(property, version, config));
            }
        }
        Iterator<Resource> iter = r.getChildren().iterator();
        while (iter.hasNext()) {
            depth++;
            Resource child = iter.next();
            String relPath = EvolutionPathUtil.getRelativeResourceName(child.getPath());
            if (config.handleResource(relPath)) {
                versionEntries.add(new EvolutionEntryImpl(child, version, config));
                populate(child, depth);
            }
            depth--;
        }
    }
}
