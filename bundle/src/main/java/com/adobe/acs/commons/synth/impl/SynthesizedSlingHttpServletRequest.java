/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
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

package com.adobe.acs.commons.synth.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;

/**
 * Used to build internal sling requests to do nice things :)
 *
 * @see com.adobe.acs.commons.forms.helpers.impl.synthetics.SyntheticSlingHttpServletGetRequest
 */
public class SynthesizedSlingHttpServletRequest extends SlingHttpServletRequestWrapper {

    /** GET */
    public static final String METHOD_GET = "GET";
    /** POST */
    public static final String METHOD_POST = "POST";
    /** PUT */
    public static final String METHOD_PUT = "PUT";
    /** DELETE */
    public static final String METHOD_DELETE = "DELETE";
    /** HEAD */
    public static final String METHOD_HEAD = "HEAD";
    /** OPTIONS */
    public static final String METHOD_OPTIONS = "OPTIONS";
    /** TRACE */
    public static final String METHOD_TRACE = "TRACE";


    protected RequestPathInfo requestPathInfo;

    protected String method;
    protected Resource resource;

    protected String resourcePath;
    protected String extension;
    protected String suffix;
    protected String[] selectors;
    protected boolean isSuffixOverridden;
    protected boolean isSelectorOverridden;
    protected boolean isExtensionOverridden;
    protected boolean isResourcePathOverridden;

    public SynthesizedSlingHttpServletRequest(final SlingHttpServletRequest request) {
        super(request);

        WrappedRequestPathInfo wrappedRequestPathInfo = createWrappedRequestPathInfo();
        RequestPathInfo wrappedRequestInfo = (RequestPathInfo) Proxy.newProxyInstance(RequestPathInfo.class.getClassLoader(), new Class[] { RequestPathInfo.class, com.adobe.acs.commons.synth.WrappedRequestPathInfo.class  }, wrappedRequestPathInfo);
        requestPathInfo = wrappedRequestInfo;
    }

    @Override
    public String getMethod() {
        return (method != null) ? method : super.getMethod();
    }

    @Override
    public Resource getResource() {
        return (resource != null) ? resource : super.getResource();
    }

    @Override
    public RequestPathInfo getRequestPathInfo() {
        return requestPathInfo;
    }

    /**
     * Explicitly overwrites the request method
     *
     * @param method
     * @return
     */
    public SynthesizedSlingHttpServletRequest setMethod(String method) {
        this.method = method;

        return this;
    }

    /**
     * Explicitly overwrites the resource being request, but keeps the request resource path
     *
     * @param resource
     * @return
     */
    public SynthesizedSlingHttpServletRequest setResource(Resource resource) {
        this.resource = resource;

        return this;
    }

    /**
     * Explicitly overwrites the request resource path, but keeps the resource
     *
     * @param resourcePath
     * @return
     */
    public SynthesizedSlingHttpServletRequest setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
        this.isResourcePathOverridden = true;

        return this;
    }

    /**
     * Explicitly overwrites the request extension
     *
     * @param extension
     * @return
     */
    public SynthesizedSlingHttpServletRequest setExtension(String extension) {
        this.extension = extension;
        this.isExtensionOverridden = true;

        return this;
    }

    /**
     * Explicitly overwrites the request suffix
     *
     * @param suffix
     * @return
     */
    public SynthesizedSlingHttpServletRequest setSuffix(String suffix) {
        this.suffix = suffix;
        this.isSuffixOverridden = true;

        return this;
    }

    /**
     * Explicitly clears all request selectors
     *
     * @return
     */
    public SynthesizedSlingHttpServletRequest clearSelectors() {
        this.selectors = new String[] {};
        this.isSelectorOverridden = true;

        return this;
    }

    /**
     * Explicitly sets the request selectors
     *
     * @param selectors
     * @return
     */
    public SynthesizedSlingHttpServletRequest setSelectors(String[] selectors) {
        if (selectors == null) {
            this.selectors = null;
        } else {
            this.selectors = Arrays.copyOf(selectors, selectors.length);
        }
        this.isSelectorOverridden = true;

        return this;
    }

    public WrappedRequestPathInfo createWrappedRequestPathInfo() {
        return new WrappedRequestPathInfo();
    }

    class WrappedRequestPathInfo implements InvocationHandler {

        private RequestPathInfo getOriginal() {
            return getSlingRequest().getRequestPathInfo();
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
            return isResourcePathOverridden ? resourcePath : getOriginal().getResourcePath();
        }

        public String getExtension() {
            return isExtensionOverridden ? extension : getOriginal().getExtension();
        }

        public String getSelectorString() {
            if (isSelectorOverridden) {
                return StringUtils.join(selectors, ".");
            }

            return getOriginal().getSelectorString();
        }

        public String[] getSelectors() {
            return isSelectorOverridden ? selectors : getOriginal().getSelectors();
        }

        public String getSuffix() {
            return isSuffixOverridden ? suffix : getOriginal().getSuffix();
        }

        public Resource getSuffixResource() {
            if (isSuffixOverridden) {
                return getSlingRequest().getResourceResolver().getResource(suffix);
            }

            return getOriginal().getSuffixResource();
        }
    }
}
