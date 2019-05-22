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

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;

import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.version.EvolutionEntry;

public final class EvolutionEntryImpl implements EvolutionEntry {

    private static final Logger log = LoggerFactory.getLogger(EvolutionEntryImpl.class);

    private static int MAX_CHARS = 200;
    static String V_ADDED = "added";
    static String V_CHANGED = "changed";
    static String V_REMOVED = "removed";
    static String V_ADDED_REMOVED = "added-removed";
    static String V_CHANGED_REMOVED = "changed-removed";

    private EvolutionEntryType type;
    private String name;
    private Object value;
    private int depth;
    private String path;
    private Version version;
    private String relativePath;
    private Property property;

    public EvolutionEntryImpl(final Resource resource, final Version version) {
        type = EvolutionEntryType.RESOURCE;
        name = resource.getName();
        depth = EvolutionPathUtil.getDepthForPath(resource.getPath());
        path = resource.getParent().getName();
        this.version = version;
        value = null;
        relativePath = EvolutionPathUtil.getRelativeResourceName(resource.getPath());
    }

    public EvolutionEntryImpl(final Property property, final Version version) {
        this.property = property;
        type = EvolutionEntryType.PROPERTY;
        this.version = version;
        value = EvolutionConfig.printProperty(property);
        try {
            final String propertyPath = property.getPath();
            name = property.getName();
			depth = EvolutionPathUtil.getDepthForPath(propertyPath);
            path = property.getParent().getName();
            relativePath = EvolutionPathUtil.getRelativePropertyName(propertyPath);
        } catch (final RepositoryException e) {
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
        return (name + path).replace(":", "_").replace("/", "_").replace("@", "_").replace("frozenNode", "node");
    }

    @Override
    public EvolutionEntryType getType() {
        return type;
    }

    @Override
    public String getValueString() {
        return EvolutionConfig.printObject(value);
    }

    @Override
    public String getValueStringShort() {
    	final String tempValue = getValueString();
        if (tempValue.length() > MAX_CHARS) {
            return tempValue.substring(0, MAX_CHARS) + "...";
        }

        return tempValue;
    }

    @Override
    public int getDepth() {
        return depth - 1;
    }

    @Override
    public boolean isCurrent() {
        try {
        	final Version[] successors = version.getSuccessors();
            if (successors == null || successors.length == 0) {
                return true;
            }
        } catch (RepositoryException e) {
            // no-op
        }
        return false;
    }

    @Override
    public String getStatus() {
        if (isChanged() && isWillBeRemoved()) {
            return V_CHANGED_REMOVED;
        } else if (isAdded() && isWillBeRemoved()) {
            return V_ADDED_REMOVED;
        } else if (isAdded()) {
            return V_ADDED;
        } else if (isWillBeRemoved()) {
            return V_REMOVED;
        } else if (isChanged()) {
            return V_CHANGED;
        } else {
            return "";
        }
    }

    @Override
    public boolean isAdded() {
        try {
            if (isResource()) {
                Node node = version.getLinearPredecessor().getFrozenNode().getNode(relativePath);
                return node == null;
            } else {
                Property prop = version.getLinearPredecessor().getFrozenNode().getProperty(relativePath);
                return prop == null;
            }
        } catch (Exception e) {
            // no-op
        }

        return true;
    }

    @Override
    public boolean isWillBeRemoved() {
        try {
            if (isCurrent()) {
                return false;
            }

            if (isResource()) {
            	final Node node = version.getLinearSuccessor().getFrozenNode().getNode(relativePath);
                return node == null;
            } else {
            	final Property prop = version.getLinearSuccessor().getFrozenNode().getProperty(relativePath);
                return prop == null;
            }
        } catch (Exception e) {
            // no-op
        }

        return true;
    }

    @Override
    public boolean isChanged() {
        try {
            if (isResource()) {
                return false;
            }
            final Property prop = version.getLinearPredecessor().getFrozenNode().getProperty(relativePath);
            final String currentValue = EvolutionConfig.printProperty(prop);
            final String oldValue = EvolutionConfig.printProperty(property);
            return !currentValue.equals(oldValue);
        } catch (Exception e) {
            log.error("Unable to check changed status", e);
        }

        return false;
    }

}
