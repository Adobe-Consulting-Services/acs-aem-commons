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
package com.adobe.acs.commons.mcp.form;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.StreamSupport;

import com.day.cq.commons.jcr.JcrConstants;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.AbstractResource;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ModifiableValueMapDecorator;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.osgi.annotation.versioning.ProviderType;

import static com.day.cq.commons.jcr.JcrConstants.JCR_PRIMARYTYPE;

/**
 * This is a mock resource class used to pass values in to the granite UI components.
 */
@ProviderType
public class AbstractResourceImpl extends AbstractResource {

    List<Resource> children;
    ResourceMetadata meta;
    ResourceResolver rr;
    Resource parent;
    String path;
    String type;
    String superType;

    ValueMap properties;

    public AbstractResourceImpl(String path, String resourceType, String resourceSuperType, Map<String, Object> properties) {
        children = new ArrayList<>();
        this.path = path;
        this.type = resourceType;
        this.superType = resourceSuperType;
        this.meta = new ResourceMetadata();
        if (resourceType != null) {
            meta.put(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, resourceType);
        }
        // Required property; Setting to path breaks functionality
        meta.setResolutionPath(path);
        meta.setCreationTime((new Date()).getTime());
        meta.setResolutionPathInfo("");


        if (properties == null) {
            properties = new HashMap<>();
        }

        if (resourceType != null) {
            properties.put(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, resourceType);
        }
        if (resourceSuperType != null) {
            properties.put(JcrResourceConstants.SLING_RESOURCE_SUPER_TYPE_PROPERTY, resourceSuperType);
        }

        this.properties = new ValueMapDecorator(properties);
    }

    @Override
    public <T> T adaptTo(Class<T> clazz) {
        if (clazz.equals(ModifiableValueMap.class)) {
            return (T) new ModifiableValueMapDecorator(this.getValueMap());
        } else if (clazz == ValueMap.class || clazz == Map.class) {
            return (T) getValueMap();
        } else {
            return null;
        }
    }

    public void setResourceResolver(ResourceResolver rr) {
        this.rr = rr;
        children.forEach(r -> ((AbstractResourceImpl) r).setResourceResolver(rr));
    }

    public void addChild(Resource res) {
        if (res instanceof AbstractResourceImpl) {
            String originalName = res.getName();
            String name = originalName;
            int seq = 1;
            while (getChildNamed(name) != null) {
                name = originalName + (seq++);
            }
            AbstractResourceImpl child = ((AbstractResourceImpl) res);
            child.parent = this;
            child.path = path + "/" + name;
            child.setResourceResolver(rr);
        }
        children.add(res);
    }

    public void removeChild(Resource res) {
        children.remove(res);
        if (res instanceof AbstractResourceImpl) {
            AbstractResourceImpl child = ((AbstractResourceImpl) res);
            child.parent = null;
            if (child.path.startsWith("/")) {
                child.path = child.path.replaceFirst(path + "/", "");
            }
        }
    }

    @Override
    public String getName() {
        return path.contains("/") ? StringUtils.substringAfterLast(path, "/") : path;
    }

    @Override
    public Resource getParent() {
        return parent;
    }

    @Override
    public Resource getChild(String relPath) {
        if (relPath.startsWith("/")) {
            relPath = relPath.replace(getPath(), "");
        }
        Resource current = this;
        for (String name : StringUtils.split(relPath, "/")) {
            if (current instanceof AbstractResourceImpl) {
                current = ((AbstractResourceImpl) current).getChildNamed(name);
                if (current == null) {
                    return null;
                }
            } else {
                return StreamSupport.stream(getChildren().spliterator(), false)
                        .filter(r->r.getName().equals(name))
                        .findFirst().orElse(null);
            }
        }
        return current;
    }

    public Resource getChildNamed(String name) {
        for (Resource child : getChildren()) {
            if (child.getName().equals(name)) {
                return child;
            }
        }
        return null;
    }

    @Override
    public Iterable<Resource> getChildren() {
        return children;
    }

    @Override
    public Iterator<Resource> listChildren() {
        return children.iterator();
    }

    @Override
    public boolean hasChildren() {
        return !children.isEmpty();
    }

    @Override
    public boolean isResourceType(String type) {
        if (getResourceResolver() != null) {
            return getResourceResolver().isResourceType(this, type);
        } else {
            return Objects.equals(getResourceType(), type);
        }
    }

    public void setPath(String path) {
        this.path = path;
        meta.setResolutionPath(this.path);
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getResourceType() {
        Object t = properties.get(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY);
        return t == null ? null : String.valueOf(t);
    }

    @Override
    public String getResourceSuperType() {
        return superType;
    }

    @Override
    public ResourceMetadata getResourceMetadata() {
        return meta;
    }

    @Override
    public ValueMap getValueMap() {
        return new ValueMapDecorator(properties);
    }

    @Override
    public ResourceResolver getResourceResolver() {
        return rr;
    }

    public AbstractResourceImpl cloneResource() {
        ValueMap clonedProperties = new ValueMapDecorator(new HashMap<>());
        if (properties != null) {
            clonedProperties.putAll(properties);
        }
        AbstractResourceImpl clone = new AbstractResourceImpl(getPath(), getResourceType(), getResourceSuperType(), properties);
        getChildren().forEach(child -> clone.addChild(((AbstractResourceImpl) child).cloneResource()));
        return clone;
    }

    public void disableMergeResourceProvider() {
        properties.put("sling:hideChildren", "*");
        children.forEach(c -> ((AbstractResourceImpl) c).disableMergeResourceProvider());
    }

    public Map<String, Object> convertTreeToMap() {
        HashMap<String, Object> out = new HashMap<>();
        out.putAll(properties);
        children.forEach(c -> out.put(c.getName(), ((AbstractResourceImpl) c).convertTreeToMap()));
        return out;
    }
}
