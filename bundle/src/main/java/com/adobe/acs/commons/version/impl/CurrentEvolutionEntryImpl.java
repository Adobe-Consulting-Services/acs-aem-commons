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

import com.adobe.acs.commons.version.EvolutionEntry;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Property;

public class CurrentEvolutionEntryImpl implements EvolutionEntry {

    private static final Logger log = LoggerFactory.getLogger(CurrentEvolutionEntryImpl.class);

    private static final int MAX_CHARS = 200;

    private EvolutionEntryType type;
    private String name;
    private Object value;
    private int depth;
    private String path;
    private EvolutionConfig config;

    public CurrentEvolutionEntryImpl(Resource resource, EvolutionConfig config) {
        this.config = config;
        this.type = EvolutionEntryType.RESOURCE;
        this.name = resource.getName();
        this.depth = EvolutionPathUtil.getLastDepthForPath(resource.getPath());
        this.path = resource.getParent().getName();
        this.value = null;
    }

    public CurrentEvolutionEntryImpl(Property property, EvolutionConfig config) {
        try {
            this.config = config;
            this.type = EvolutionEntryType.PROPERTY;
            this.name = property.getName();
            this.depth = EvolutionPathUtil.getLastDepthForPath(property.getPath());
            this.path = property.getParent().getName();
            this.value = config.printProperty(property);
        } catch (Exception e) {
            log.error("Could not inititalize VersionEntry", e);
        }
    }

    @Override
    public boolean isResource() {
        return EvolutionEntryType.RESOURCE == type;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getUniqueName() {
        return (name + path).replace(":", "_").replace("/", "_").replace("@", "_").replace("content", "node");
    }

    @Override
    public EvolutionEntryType getType() {
        return type;
    }

    @Override
    public String getValueString() {
        return config.printObject(value);
    }

    @Override
    public String getValueStringShort() {
        String tmpValue = getValueString();
        if (tmpValue.length() > MAX_CHARS) {
            return tmpValue.substring(0, MAX_CHARS) + "...";
        }
        return tmpValue;
    }

    @Override
    public int getDepth() {
        return depth - 1;
    }

    @Override
    public boolean isCurrent() {
        return true;
    }

    @Override
    public String getStatus() {
        if (isChanged() && isWillBeRemoved()) {
            return EvolutionEntryImpl.V_CHANGED_REMOVED;
        } else if (isAdded() && isWillBeRemoved()) {
            return EvolutionEntryImpl.V_ADDED_REMOVED;
        } else if (isAdded()) {
            return EvolutionEntryImpl.V_ADDED;
        } else if (isWillBeRemoved()) {
            return EvolutionEntryImpl.V_REMOVED;
        } else if (isChanged()) {
            return EvolutionEntryImpl.V_CHANGED;
        } else {
            return "";
        }
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
