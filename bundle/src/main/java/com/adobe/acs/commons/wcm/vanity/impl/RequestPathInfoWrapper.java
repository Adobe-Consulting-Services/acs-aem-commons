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
package com.adobe.acs.commons.wcm.vanity.impl;

import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class RequestPathInfoWrapper implements InvocationHandler {
    private final RequestPathInfo requestPathInfo;
    private final Resource resource;
    private static final String SLING_STATUS = "sling:status";

    private RequestPathInfoWrapper(RequestPathInfo requestPathInfo, Resource resource) {
        this.requestPathInfo = requestPathInfo;
        this.resource = resource;
    }

    public static RequestPathInfoWrapper createRequestPathInfoWrapper(final RequestPathInfo requestPathInfo, final Resource resource) {
        return new RequestPathInfoWrapper(requestPathInfo, resource);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        switch (methodName) {
            case "getResourcePath":
                return getResourcePath();
            case "getExtension":
                return getExtension();
            case "getSelectorString":
                return getSelectorString();
            case "getSelectors":
                return getSelectors();
            case "getSuffix":
                return getSuffix();
            case "getSuffixResource":
                return getSuffixResource();
            default:
                throw new UnsupportedOperationException("REQUESTPATHINFOWRAPPER >> NO IMPLEMENTATION FOR " + methodName);
        }
    }

    public String getResourcePath() {
        return requestPathInfo.getResourcePath();
    }

    public String getExtension() {
        final ValueMap properties = resource.getValueMap();
        if (properties != null) {
            if (properties.get(SLING_STATUS, -1) < 0) {
                // Internal redirect; so keep extension
                return requestPathInfo.getExtension();
            } else {
                // External redirect; like 301 or 302 then kill the extension else it gets double added. Note this will also kill any selector addition.
                return null;
            }
        } else {
            return requestPathInfo.getExtension();
        }
    }

    public String getSelectorString() {
        return requestPathInfo.getSelectorString();
    }

    public String[] getSelectors() {
        return requestPathInfo.getSelectors();
    }

    public String getSuffix() {
        return requestPathInfo.getSuffix();
    }

    public Resource getSuffixResource() {
        return requestPathInfo.getSuffixResource();
    }
}
