package com.adobe.acs.commons.mcp.util;

import com.adobe.acs.commons.mcp.form.AbstractResourceImpl;
import java.util.Map;
import org.apache.sling.api.resource.Resource;

/**
 *
 */
public class SyntheticResourceBuilder {

    AbstractResourceImpl rootResource;
    AbstractResourceImpl currentResource;

    public SyntheticResourceBuilder(AbstractResourceImpl start) {
        rootResource = currentResource = start;
    }

    public SyntheticResourceBuilder(String name, String resourceType) {
        rootResource = new AbstractResourceImpl(name, resourceType, null, null);
        currentResource = rootResource;
    }

    public SyntheticResourceBuilder createChild(String name, String resourceType) {
        AbstractResourceImpl child = new AbstractResourceImpl(name, resourceType, null, null);
        currentResource.addChild(child);
        currentResource = child;
        return this;
    }

    public SyntheticResourceBuilder createChild(String name) {
        return createChild(name, null);
    }

    public SyntheticResourceBuilder createSibling(String name, String resourceType) {
        return up().createChild(name, resourceType);
    }

    public SyntheticResourceBuilder createSibling(String name) {
        return createSibling(name, null);
    }

    public SyntheticResourceBuilder withPath(String path) {
        currentResource.setPath(path);
        return this;
    }

    /**
     * Note: this doesn't navigate to the child, it just adds it to the current
     * resource
     */
    public SyntheticResourceBuilder withChild(Resource child) {
        currentResource.addChild(child);
        return this;
    }

    public SyntheticResourceBuilder withAttributes(Object... attrs) {
        for (int i = 0; i < attrs.length - 1; i += 2) {
            currentResource.getResourceMetadata().put(String.valueOf(attrs[i]), attrs[i + 1]);
        }
        return this;
    }

    public SyntheticResourceBuilder withAttributes(Map<String, Object> attrs) {
        currentResource.getResourceMetadata().putAll(attrs);
        return this;
    }

    public SyntheticResourceBuilder up(int levels) {
        for (int i = 0; i < levels && currentResource.getParent() != null; i++) {
            currentResource = (AbstractResourceImpl) currentResource.getParent();
        }
        return this;
    }

    public SyntheticResourceBuilder up(String name) {
        while (!currentResource.getName().equals(name) && currentResource.getParent() != null) {
            currentResource = (AbstractResourceImpl) currentResource.getParent();
        }
        return this;
    }

    public SyntheticResourceBuilder up() {
        return up(1);
    }

    public AbstractResourceImpl build() {
        return rootResource;
    }
}
