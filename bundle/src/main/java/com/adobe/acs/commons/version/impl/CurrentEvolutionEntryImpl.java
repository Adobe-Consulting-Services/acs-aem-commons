package com.adobe.acs.commons.version.impl;

import com.adobe.acs.commons.version.EvolutionEntry;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;

/**
 * Created by Dominik Foerderreuther <df@adobe.com> on 19/12/16.
 */
public class CurrentEvolutionEntryImpl implements EvolutionEntry {

    private static final Logger log = LoggerFactory.getLogger(CurrentEvolutionEntryImpl.class);

    private static int MAX_CHARS = 200;
    private static String V_ADDED = "added";
    private static String V_CHANGED = "changed";
    private static String V_REMOVED = "removed";
    private static String V_ADDED_REMOVED = "added-removed";
    private static String V_CHANGED_REMOVED = "changed-removed";

    private EvolutionEntryType type;
    private String name;
    private Object value;
    private int depth;
    private String path;
    private String relativePath;
    private Property property;
    private EvolutionConfig config;

    public CurrentEvolutionEntryImpl(Resource resource, EvolutionConfig config) {
        this.config = config;
        this.type = EvolutionEntryType.RESOURCE;
        this.name = resource.getName();
        this.depth = config.getDepthForPath(resource.getPath());
        this.path = resource.getParent().getName();
        this.value = null;
        this.relativePath = config.getRelativeResourceName(resource.getPath());
    }

    public CurrentEvolutionEntryImpl(Property property, EvolutionConfig config) {
        try {
            this.config = config;
            this.property = property;
            this.type = EvolutionEntryType.PROPERTY;
            this.name = property.getName();
            this.depth = config.getDepthForPath(property.getPath());
            this.path = property.getParent().getName();
            this.value = config.printProperty(property);
            this.relativePath = config.getRelativePropertyName(property.getPath());
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
        return (name + path).replace(":", "_").replace("/", "_").replace("@", "_");
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
        String value = getValueString();
        if (value.length() > MAX_CHARS) {
            return value.substring(0, MAX_CHARS) + "...";
        }
        return value;
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
