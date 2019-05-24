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

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;

import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EvolutionEntryImpl extends EvolutionEntryImplBase {

    private static final Logger log = LoggerFactory.getLogger(EvolutionEntryImpl.class);

    private final Version version;
    private String relativePath;
    private final Property property;

    public EvolutionEntryImpl(final Resource resource, final Version version) {
        super(resource, EvolutionPathUtil.getDepthForPath(resource.getPath()));
        property = null;
        this.version = version;
        relativePath = EvolutionPathUtil.getRelativeResourceName(resource.getPath());
    }

    public EvolutionEntryImpl(final Property property, final Version version)
            throws AccessDeniedException, ItemNotFoundException, RepositoryException {
        super(property, EvolutionPathUtil.getDepthForPath(property.getPath()));
        this.property = property;
        this.version = version;
        relativePath = EvolutionPathUtil.getRelativePropertyName(property.getPath());
    }

    @Override
    public String getUniqueName() {
        return getUniqueNameBase().replace("frozenNode", "node");
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
