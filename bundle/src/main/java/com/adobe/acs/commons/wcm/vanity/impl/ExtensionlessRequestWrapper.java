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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;

public class ExtensionlessRequestWrapper extends SlingHttpServletRequestWrapper {

    private static final String SLING_STATUS = "sling:status";

    public ExtensionlessRequestWrapper(SlingHttpServletRequest wrappedRequest) {
        super(wrappedRequest);
    }

    public RequestPathInfo getRequestPathInfo() {
        return new RequestPathInfoWrapper(super.getRequestPathInfo(), super.getResource());
    }

    private class RequestPathInfoWrapper implements RequestPathInfo {
        private final RequestPathInfo requestPathInfo;
        private final Resource resource;

        public RequestPathInfoWrapper(RequestPathInfo requestPathInfo, Resource resource) {
            this.requestPathInfo = requestPathInfo;
            this.resource = resource;
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
}