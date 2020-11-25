package com.adobe.acs.commons.http.headers.impl;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.Dictionary;

import static com.adobe.acs.commons.http.headers.impl.AbstractDispatcherCacheHeaderFilter.PROP_DISPATCHER_FILTER_ENGINE;
import static com.adobe.acs.commons.http.headers.impl.AbstractDispatcherCacheHeaderFilter.PROP_DISPATCHER_FILTER_ENGINE_SLING;

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
        @Property(
                name = PROP_DISPATCHER_FILTER_ENGINE,
                value = PROP_DISPATCHER_FILTER_ENGINE_SLING,
                propertyPrivate = true)
})
public class ResourceTypeBasedDispatcherMaxAgeHeaderFilter extends DispatcherMaxAgeHeaderFilter {

    private static final Logger log = LoggerFactory.getLogger(ResourceTypeBasedDispatcherMaxAgeHeaderFilter.class);

    @Property(label = "Resource types",
            description = "Resource types the page should have to use this filter.",
            cardinality = Integer.MAX_VALUE)
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
            return verifyResourceType(resource);
        }
        return false;
    }

    private Resource getResource(SlingHttpServletRequest slingRequest) {
        if (slingRequest.getResource().isResourceType("cq:Page")) {
            log.trace("Found page resource, checking page content resource type");
            return slingRequest.getResource().getChild(JcrConstants.JCR_CONTENT);
        }
        log.trace("Found non-page resource, checking request resource type");
        return slingRequest.getResource();
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
