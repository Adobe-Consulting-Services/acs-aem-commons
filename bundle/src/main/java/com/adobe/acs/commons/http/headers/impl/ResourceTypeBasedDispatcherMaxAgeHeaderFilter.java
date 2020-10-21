/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2015 Adobe
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
package com.adobe.acs.commons.http.headers.impl;

import com.day.cq.commons.PathInfo;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Map;

@Component(
        label = "ACS AEM Commons - Dispacher Cache Control Header Resource Type Based - Max Age",
        description = "Adds a Cache-Control max-age header to content based on resource type to enable Dispatcher TTL support.",
        metatype = true,
        configurationFactory = true,
        policy = ConfigurationPolicy.REQUIRE)
@Properties({
        @Property(
                name = "webconsole.configurationFactory.nameHint",
                value = "Max Age: {max.age} for Resource Types: [{resource.types}]",
                propertyPrivate = true),
        @Property(name = "sling.filter.scope", value = {"REQUEST", "FORWARD"})
})
public class ResourceTypeBasedDispatcherMaxAgeHeaderFilter extends DispatcherMaxAgeHeaderFilter {

    private static final Logger log = LoggerFactory.getLogger(ResourceTypeBasedDispatcherMaxAgeHeaderFilter.class);

    @Property(label = "Resource types",
            description = "Resource types the page should have to use this filter.",
            cardinality = Integer.MAX_VALUE)
    public static final String PROP_RESOURCE_TYPES = "resource.types";

    private String[] resourceTypes;

    private static final String SERVICE_NAME = "resource-type-dispatcher-header-filter";
    private static final Map<String, Object> AUTH_INFO;

    static {
        AUTH_INFO = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, (Object) SERVICE_NAME);
    }

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Override
    @SuppressWarnings("unchecked")
    protected boolean accepts(final HttpServletRequest request) {
        if (!super.accepts(request)) {
            log.debug("Not accepting request because it is not coming from the dispatcher.");
            return false;
        }

        try (ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(AUTH_INFO)) {
            PathInfo pathInfo = new PathInfo(request.getRequestURI());
            Resource reqResource = resourceResolver.getResource(pathInfo.getResourcePath());
            if (reqResource != null) {
                Resource childResource = reqResource.getChild(JcrConstants.JCR_CONTENT);
                if (childResource != null) {
                    return verifyResourceType(childResource);
                }
            }

        } catch (LoginException e) {
            log.error("Could not get resource resolver", e);
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

}
