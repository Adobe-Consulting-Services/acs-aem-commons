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

        requestPathInfo = new WrappedRequestPathInfo();
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


    private class WrappedRequestPathInfo implements RequestPathInfo {

        private RequestPathInfo getOriginal() {
            return getSlingRequest().getRequestPathInfo();
        }

        @Override
        public String getResourcePath() {
            return isResourcePathOverridden ? resourcePath : getOriginal().getResourcePath();
        }

        @Override
        public String getExtension() {
            return isExtensionOverridden ? extension : getOriginal().getExtension();
        }

        @Override
        public String getSelectorString() {
            if (isSelectorOverridden) {
                return StringUtils.join(selectors, ".");
            }

            return getOriginal().getSelectorString();
        }

        @Override
        public String[] getSelectors() {
            return isSelectorOverridden ? selectors : getOriginal().getSelectors();
        }

        @Override
        public String getSuffix() {
            return isSuffixOverridden ? suffix : getOriginal().getSuffix();
        }

        @Override
        public Resource getSuffixResource() {
            if (isSuffixOverridden) {
                return getSlingRequest().getResourceResolver().getResource(suffix);
            }

            return getOriginal().getSuffixResource();
        }
    }
}
