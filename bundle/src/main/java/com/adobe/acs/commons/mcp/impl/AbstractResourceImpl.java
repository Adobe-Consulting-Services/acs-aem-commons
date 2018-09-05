/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
package com.adobe.acs.commons.mcp.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.sling.api.resource.AbstractResource;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ModifiableValueMapDecorator;
import org.apache.sling.api.wrappers.ValueMapDecorator;

/**
 * This is a mock resource class used to pass values in to the granite UI components.
 */
public class AbstractResourceImpl extends AbstractResource {
    
    List<Resource> children;
    ResourceMetadata meta;
    ResourceResolver rr;
    Resource parent;
    String path;
    String type;
    String superType;

    public AbstractResourceImpl(String path, String resourceType, String resourceSuperType, ResourceMetadata metadata) {
        children = new ArrayList<>();
        this.path = path;
        this.type = resourceType;
        this.superType = resourceSuperType;
        this.meta = metadata;
    }
    
    @Override
    public <T> T adaptTo(Class<T> clazz) {
        if (clazz.equals(ModifiableValueMap.class)) {
            return (T) new ModifiableValueMapDecorator(this.getValueMap());
        } else {
            return null;
        }
    }

    public void setResourceResolver(ResourceResolver rr) {
        this.rr = rr;
    }
    
    public void addChild(Resource res) {
        children.add(res);
        if (res instanceof AbstractResourceImpl) {
            AbstractResourceImpl child = ((AbstractResourceImpl) res);
            child.parent = this;
            if (!child.path.startsWith("/")) {
                child.path = path + "/" + child.path;
            }
            ((AbstractResourceImpl) res).setResourceResolver(rr);
        }
    }

    @Override
    public String getName() {
        return path.substring(path.lastIndexOf('/')+1);
    }

    @Override
    public Resource getParent() {
        return parent;
    }
    
    @Override
    public Resource getChild(String relPath) {
        return children.stream().filter(c->c.getName().equals(relPath)).findFirst().orElse(null);
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
    public String getPath() {
        return path;
    }

    @Override
    public String getResourceType() {
        return type;
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
        return new ValueMapDecorator(meta);
    }

    @Override
    public ResourceResolver getResourceResolver() {
        return rr;
    }
    
}
