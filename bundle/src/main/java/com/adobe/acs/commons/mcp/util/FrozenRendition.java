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
package com.adobe.acs.commons.mcp.util;

import com.adobe.granite.asset.api.AssetIOException;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.Rendition;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Iterator;

import static org.apache.jackrabbit.JcrConstants.JCR_DATA;

public class FrozenRendition implements InvocationHandler {

    private static final Logger LOG = LoggerFactory.getLogger(FrozenRendition.class);

    private final Asset asset;

    private final Resource container;

    private final Resource renditionData;

    private FrozenRendition(Asset asset, Resource container) {
        this.asset = asset;
        this.container = container;
        this.renditionData = container.getChild(JcrConstants.JCR_CONTENT);
    }

    public static Rendition createFrozenRendition(Asset asset, Resource resource) {
        InvocationHandler handler = new FrozenRendition(asset, resource);
        return (Rendition) Proxy.newProxyInstance(FrozenRendition.class.getClassLoader(), new Class[] { Rendition.class }, handler);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        switch (methodName) {
            case "getParent":
                return getParent();
            case "listChildren":
                return listChildren();
            case "getChildren":
                return getChildren();
            case "getChild":
                return getChild((String) args[0]);
            case "getResourceType":
                return getResourceType();
            case "getResourceSuperType":
                return getResourceSuperType();
            case "hasChildren":
                return hasChildren();
            case "isResourceType":
                return isResourceType((String) args[0]);
            case "getResourceMetadata":
                return getResourceMetadata();
            case "getResourceResolver":
                return getResourceResolver();
            case "getValueMap":
                return getValueMap();
            case "adaptTo":
                return adaptTo((Class<?>) args[0]);
            case "getMimeType":
                return getMimeType();
            case "getName":
                return getName();
            case "getPath":
                return getPath();
            case "getProperties":
                return getProperties();
            case "getStream":
                return getStream();
            case "getAsset":
                return getAsset();
            default:
                throw new UnsupportedOperationException();
        }
    }

    public Resource getParent() {
        return container.getParent();
    }

    public Iterator<Resource> listChildren() {
        return container.listChildren();
    }

    public Iterable<Resource> getChildren() {
        return container.getChildren();
    }

    public Resource getChild(String relPath) {
        return container.getChild(relPath);
    }

    public String getResourceType() {
        return container.getResourceType();
    }

    public String getResourceSuperType() {
        return container.getResourceSuperType();
    }

    public boolean hasChildren() {
        return container.hasChildren();
    }

    public boolean isResourceType(String resourceType) {
        return container.isResourceType(resourceType);
    }

    public ResourceMetadata getResourceMetadata() {
        return container.getResourceMetadata();
    }

    public ResourceResolver getResourceResolver() {
        return container.getResourceResolver();
    }

    public ValueMap getValueMap() {
        return container.getValueMap();
    }

    public Object adaptTo(Class<?> type) {
        if (type == Resource.class) {
            return container;
        }
        return container.adaptTo(type);
    }

    public String getMimeType() {
        return getProperties().get(JcrConstants.JCR_MIMETYPE, String.class);
    }

    public String getName() {
        return container.getName();
    }

    public String getPath() {
        return container.getPath();
    }

    public ValueMap getProperties() {
        return renditionData.getValueMap();
    }

    public long getSize() {
        int size = 0;
        final Property p = renditionData.getValueMap().get(JCR_DATA, Property.class);
        try {
            return (null != p) ? p.getBinary().getSize() : 0;
        } catch (RepositoryException e) {
            LOG.error("Failed to get the Rendition binary size in bytes [{}]: ", getPath(), e);
        }
        return size;
    }

    public InputStream getStream() {
        try {
            return renditionData.getValueMap().get(JCR_DATA, InputStream.class);
        } catch (Throwable t) {
            throw new AssetIOException(t);
        }
    }

    public Asset getAsset() {
        return asset;
    }
}
