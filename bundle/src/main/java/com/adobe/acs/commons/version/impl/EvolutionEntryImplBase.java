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
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.Resource;

import com.adobe.acs.commons.version.EvolutionEntry;

public abstract class EvolutionEntryImplBase implements EvolutionEntry {

    private static String V_ADDED = "added";
    private static String V_CHANGED = "changed";
    private static String V_REMOVED = "removed";
    private static String V_ADDED_REMOVED = "added-removed";
    private static String V_CHANGED_REMOVED = "changed-removed";

    private static int MAX_CHARS = 200;

    private final String name;
    private final String path;
    private final EvolutionEntryType type;
    private final Object value;
    private final int depth;

    protected EvolutionEntryImplBase(
    		final String name,
    		final String path,
    		final EvolutionEntryType type,
    		final Object value,
    		final int depth) {
        this.name = name;
        this.path = path;
        this.type = type;
        this.value = value;
        this.depth = depth;
    }

    protected EvolutionEntryImplBase(final Resource resource, final int depth) {
    	this(
			resource.getName(),
			resource.getParent().getName(),
			EvolutionEntryType.RESOURCE,
			null,
			depth
    	);
    }

    protected EvolutionEntryImplBase(final Property property, final int depth)
    		throws AccessDeniedException, ItemNotFoundException, RepositoryException {
    	this(
			property.getName(),
			property.getParent().getName(),
			EvolutionEntryType.PROPERTY,
			EvolutionConfig.printProperty(property),
			depth
    	);
    }

    @Override
    public boolean isResource() {
        return EvolutionEntryType.RESOURCE == type;
    }

    @Override
    public String getName() {
        return name;
    }

	protected String getUniqueNameBase() {
		return (name + path).replace(":", "_").replace("/", "_").replace("@", "_");
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
    	final String fullValueString = getValueString();
        if (fullValueString.length() > MAX_CHARS) {
            return fullValueString.substring(0, MAX_CHARS) + "...";
        }

        return fullValueString;
    }

    @Override
    public int getDepth() {
        return depth - 1;
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

}
