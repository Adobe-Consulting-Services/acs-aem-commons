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

import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(configurationPolicy = ConfigurationPolicy.REQUIRE, property = {
        "webconsole.configurationFactory.nameHint=Max Age: {max.age} for Resource Types: [{resource.types}]"
})
@Designate(ocd = ResourceTypeBasedDispatcherMaxAgeHeaderFilter.Config.class, factory = true)
public class ResourceTypeBasedDispatcherMaxAgeHeaderFilter extends ResourceBasedDispatcherMaxAgeHeaderFilter {

    @ObjectClassDefinition(name = "ACS AEM Commons - Cache Control Header Resource Type Based - Max Age", description = "Adds a Cache-Control: max-age header to responses based on resource type (for example to enable Dispatcher TTL support).")
    // meta annotation
    public @interface Config {
        @AttributeDefinition(name = "Filter Patterns", description = "Restricts adding the headers to request paths which match any of the supplied regular expression patterns.", cardinality = Integer.MAX_VALUE)
        String[] filter_pattern() default {};

        @AttributeDefinition(name = "Cache-Control Max-Age", description = "Value of the max-age directive to emit in the Cache-Control header.")
        long max_age();

        @AttributeDefinition(name = "Resource types", description = "Resource types the request's resource must have to use this filter. This also matches if the given resource type is any of the request's resource super types.", cardinality = Integer.MAX_VALUE)
        String[] resource_types();

        @AttributeDefinition(name = "Allow Authorized Requests", description = "If the header should be added also to authorized requests (carrying a \"Authorization\" header, or cookie with name \"login-token\" or \"authorizization\").")
        boolean allow_authorized() default true;

        @AttributeDefinition(name = "Allow All Parameters", description = "If the header should be added also to requests carrying any parameters except for those given in \"block.params\".")
        boolean allow_all_params() default false;

        @AttributeDefinition(name = "Disallowed Parameter Name", description = "List of request parameter names that are not allowed to be present for the header to be added. Only relevant if \"allow.all.params\" is true.", cardinality = Integer.MAX_VALUE)
        String[] block_params() default {};

        @AttributeDefinition(name = "Allow Parameter Names", description = "List of request parameter names that are allowed to be present for the header to be added. Only relevant if \"allow.all.params\" is false.", cardinality = Integer.MAX_VALUE)
        String[] pass_through_params() default {};

        @AttributeDefinition(name = "Allow Non-Dispatcher Requests", description = "If the header should be added also to requests not coming from a dispatcher (i.e. requests not carrying the \"Server-Agent\" header containing value \"Communique-Dispatcher\").")
        boolean allow_nondispatcher() default false;

        @AttributeDefinition(name = "Service Ranking", description = "Service Ranking for the OSGi service.")
        int service_ranking() default 0;
    }

    private static final Logger log = LoggerFactory.getLogger(ResourceTypeBasedDispatcherMaxAgeHeaderFilter.class);

    private final String[] resourceTypes;

    @Activate
    public ResourceTypeBasedDispatcherMaxAgeHeaderFilter(Config config, BundleContext bundleContext) {
        super(config.max_age(), new AbstractCacheHeaderFilter.ServletRequestPredicates(config.filter_pattern(), config.allow_all_params(), config.block_params(), config.pass_through_params(), config.allow_authorized(), config.allow_nondispatcher()), config.service_ranking(), bundleContext);
        this.resourceTypes = config.resource_types();
    }

    @Override
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

    @Override
    public String toString() {
        return this.getClass().getName() + "[resource-types:" + Arrays.asList(resourceTypes) + ",fallback-max-age:" + super.getHeaderValue(null) + "]";
    }
}
