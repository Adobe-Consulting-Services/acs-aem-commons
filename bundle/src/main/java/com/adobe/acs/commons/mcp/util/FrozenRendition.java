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
import com.day.cq.dam.api.Rendition;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static org.apache.jackrabbit.JcrConstants.JCR_DATA;

public class FrozenRendition implements InvocationHandler {

    private static final Logger LOG = LoggerFactory.getLogger(FrozenRendition.class);

    private final Resource container;

    private final Resource renditionData;

    private FrozenRendition(Resource container) {
        this.container = container;
        this.renditionData = container.getChild(JcrConstants.JCR_CONTENT);
    }

    @SuppressWarnings("squid:S1172")
    public static Rendition createFrozenRendition(Asset asset, Resource resource) {
        InvocationHandler handler = new FrozenRendition(resource);
        return (Rendition) Proxy.newProxyInstance(FrozenRendition.class.getClassLoader(), new Class[] { Rendition.class }, handler);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        switch (methodName) {
            case "getSize":
                return getSize();
            default:
                LOG.error("FROZEN RENDITION >> NO IMPLEMENTATION FOR "+methodName);
                throw new UnsupportedOperationException();
        }
    }

    public String getPath() {
        return container.getPath();
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
}
