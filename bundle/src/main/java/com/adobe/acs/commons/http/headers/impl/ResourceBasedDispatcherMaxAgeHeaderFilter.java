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

package com.adobe.acs.commons.http.headers.impl;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ResourceBasedDispatcherMaxAgeHeaderFilter extends DispatcherMaxAgeHeaderFilter {

    private static final Logger log = LoggerFactory.getLogger(ResourceBasedDispatcherMaxAgeHeaderFilter.class);

    protected Resource getResource(SlingHttpServletRequest slingRequest) {
        if (slingRequest.getResource().isResourceType("cq:Page")) {
           log.trace("Found page resource, checking page content resource type");
            return slingRequest.getResource().getChild(JcrConstants.JCR_CONTENT);
        }
        log.trace("Found non-page resource, checking request resource type");
        return slingRequest.getResource();
    }

}
