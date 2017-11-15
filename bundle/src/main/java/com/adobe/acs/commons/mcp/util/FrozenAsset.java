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

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.dam.api.Rendition;
import com.day.cq.dam.api.Revision;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


public class FrozenAsset implements InvocationHandler {

    private static final Logger LOG = LoggerFactory.getLogger(FrozenAsset.class);
    private static final Joiner pathJoiner = Joiner.on('/').skipNulls();

    private static final String RENDITIONS_PATH = pathJoiner.join(JcrConstants.JCR_CONTENT, DamConstants.RENDITIONS_FOLDER);
    private static final String METADATA_PATH = pathJoiner.join(JcrConstants.JCR_CONTENT, DamConstants.METADATA_FOLDER);

    private final Asset head;
    private final Resource frozenResource;

    private FrozenAsset(Asset head, Revision revision) throws RepositoryException {
        final Node frozenNode = revision.getVersion().getFrozenNode();
        this.head = head;
        frozenResource = head.adaptTo(Resource.class).getResourceResolver().getResource(frozenNode.getPath());
    }

    private FrozenAsset(ResourceResolver resourceResolver, String path) throws RepositoryException {
        this.head = null;
        frozenResource = resourceResolver.getResource(path);
    }

    public static Asset createFrozenAsset(Asset asset, Revision revision) throws RepositoryException {
        InvocationHandler handler = new FrozenAsset(asset, revision);
        return (Asset) Proxy.newProxyInstance(FrozenAsset.class.getClassLoader(), new Class[] { Asset.class }, handler);
    }

    @SuppressWarnings("squid:S1144")
    private static Asset createFrozenAsset(ResourceResolver resourceResolver, String path) throws RepositoryException {
        InvocationHandler handler = new FrozenAsset(resourceResolver, path);
        return (Asset) Proxy.newProxyInstance(FrozenAsset.class.getClassLoader(), new Class[] { Asset.class }, handler);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        Asset asset = (Asset) proxy;
        switch (methodName) {
            case "adaptTo":
                return adaptTo((Class<?>) args[0]);
            case "getMetadataValue":
                return getMetadataValue((String) args[0]);
            case "isSubAsset":
                return isSubAsset();
            case "getRenditions":
                return getRenditions(asset);
            case "getSubAssets":
                return getSubAssets();
            default:
                LOG.error("FROZEN ASSET >> NO IMPLEMENTATION FOR "+methodName);
                throw new UnsupportedOperationException();
        }
    }

    public Object adaptTo(Class<?> type) {
        if (type.equals(Resource.class)) {
            return  frozenResource;
        }
        return null;
    }

    public String getMetadataValue(final String name) {
        Resource meta = frozenResource.getChild(METADATA_PATH);
        if (meta == null) {
            return null;
        }

        return meta.getValueMap().get(name, String.class);
    }

    public Rendition getRendition(Asset asset, String name) {
        Resource r = frozenResource.getChild(pathJoiner.join(RENDITIONS_PATH, name));
        if (r == null) {
            return null;
        }
        return FrozenRendition.createFrozenRendition(asset, r);
    }

    public boolean isSubAsset() {
        return head == null;
    }

    public Map<String, Object> getMetadata() {
        Resource meta = frozenResource.getChild(METADATA_PATH);
        if (meta == null) {
            return null;
        }
        return meta.getValueMap();
    }

    public List<Rendition> getRenditions(Asset asset) {
        Resource renditions = frozenResource.getChild(RENDITIONS_PATH);
        if (renditions == null) {
            return Lists.newArrayList();
        }
        List<Rendition> rv = Lists.newArrayList();
        for (Resource r : renditions.getChildren()) {
            rv.add(getRendition(asset, r.getName()));
        }
        return rv;
    }

    public Collection<Asset> getSubAssets() {
        if (head == null) {
            throw new UnsupportedOperationException();
        }
        Resource subassets = frozenResource.getChild(DamConstants.SUBASSETS_FOLDER);
        if (subassets != null) {
            Stream<Resource> subs = StreamSupport.stream(
                    subassets.getChildren().spliterator(), false
            );

            return subs.map(r -> {
                try {
                    return createFrozenAsset(
                            frozenResource.getResourceResolver(),
                            r.getPath());
                } catch (RepositoryException ex) {
                    LOG.error("Error retrieving subasset from "+r.getPath(), ex);
                }
                return null;
            }).collect(Collectors.toList());
        } else {
            return Collections.EMPTY_LIST;
        }
    }
}
