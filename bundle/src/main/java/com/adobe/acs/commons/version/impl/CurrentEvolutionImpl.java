/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
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
package com.adobe.acs.commons.version.impl;

import com.adobe.acs.commons.version.Evolution;
import com.adobe.acs.commons.version.EvolutionEntry;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class CurrentEvolutionImpl implements Evolution {

    public static final String LATEST_VERSION = "Latest";

    private static final Logger log = LoggerFactory.getLogger(CurrentEvolutionImpl.class);

    private final List<EvolutionEntry> versionEntries = new ArrayList<EvolutionEntry>();

    public CurrentEvolutionImpl(Resource resource, EvolutionConfig config) {
        try {
            populate(resource, config, 0);
        } catch (RepositoryException e) {
            log.warn("Could not populate Evolution", e);
        }
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

    @Override
    public List<EvolutionEntry> getVersionEntries() {
        return Collections.unmodifiableList(this.versionEntries);
    }

    private void populate(Resource r, EvolutionConfig config, int depth) throws PathNotFoundException, RepositoryException {
        ValueMap map = r.getValueMap();
        List<String> keys = new ArrayList<String>(map.keySet());
        Collections.sort(keys);
        for (String key : keys) {
            Property property = r.adaptTo(Node.class).getProperty(key);
            String relPath = EvolutionPathUtil.getLastRelativePropertyName(property.getPath());
            if (config.handleProperty(relPath)) {
                versionEntries.add(new CurrentEvolutionEntryImpl(property, config));
            }
        }
        Iterator<Resource> iter = r.getChildren().iterator();
        while (iter.hasNext()) {
            depth++;
            Resource child = iter.next();
            String relPath = EvolutionPathUtil.getLastRelativeResourceName(child.getPath());
            if (config.handleResource(relPath)) {
                versionEntries.add(new CurrentEvolutionEntryImpl(child, config));
                populate(child, config, depth);
            }
            depth--;
        }
    }
}
