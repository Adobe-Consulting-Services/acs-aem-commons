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

import org.apache.sling.api.SlingHttpServletRequest;
import org.osgi.service.component.annotations.Component;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Dictionary;

import static com.adobe.acs.commons.http.headers.impl.AbstractDispatcherCacheHeaderFilter.PROP_DISPATCHER_FILTER_ENGINE;
import static com.adobe.acs.commons.http.headers.impl.AbstractDispatcherCacheHeaderFilter.PROP_DISPATCHER_FILTER_ENGINE_SLING;

@Component(configurationFactory = true)
public class ResourceTypeBasedDispatcherMaxAgeHeaderFilter extends ResourceBasedDispatcherMaxAgeHeaderFilter {

    private static final Logger log = LoggerFactory.getLogger(ResourceTypeBasedDispatcherMaxAgeHeaderFilter.class);

        public static final String PROP_RESOURCE_TYPES = "resource.types";

    private String[] resourceTypes;

    @Override
    @SuppressWarnings("unchecked")
    protected boolean accepts(final HttpServletRequest request) {
        if (!super.accepts(request)) {
            log.debug("Not accepting request because it is not coming from the dispatcher.");
            return false;
        }
        if (request instanceof SlingHttpServletRequest) {
            SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;
            Resource resource = getResource(slingRequest);
            if (resource == null) {
                log.debug("Could not find resource for request, not accepting");
                return false;
            } else {
                return verifyResourceType(resource);
            }
        }
        return false;
    }

    private boolean verifyResourceType(Resource resource) {
        for (String resourceType : resourceTypes) {
            if (resource.isResourceType(resourceType)) {
                log.debug("Accepting request for resource: {} with resource type: {}.", resource.getPath(), resourceType);
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("squid:S1149")
    protected final void doActivate(ComponentContext context) throws Exception {
        super.doActivate(context);
        Dictionary<?, ?> properties = context.getProperties();

        resourceTypes = PropertiesUtil.toStringArray(properties.get(PROP_RESOURCE_TYPES));
        if (resourceTypes == null || resourceTypes.length == 0) {
            throw new ConfigurationException(PROP_RESOURCE_TYPES, "At least one resource type must be specified.");
        }
    }

    public String toString() {
        return this.getClass().getName() + "[resource-types:" + Arrays.asList(resourceTypes) + ",fallback-max-age:" + super.getHeaderValue(null) + "]";
    }
}
