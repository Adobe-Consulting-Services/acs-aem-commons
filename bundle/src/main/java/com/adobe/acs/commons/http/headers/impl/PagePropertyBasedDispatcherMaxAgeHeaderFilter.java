package com.adobe.acs.commons.http.headers.impl;

import com.day.cq.commons.PathInfo;
import org.apache.commons.lang.StringUtils;
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
        label = "ACS AEM Commons - Dispacher Cache Control Header Page Property Based - Max Age",
        description = "Adds a Cache-Control max-age header to content based on page property enable Dispatcher TTL support.",
        metatype = true,
        configurationFactory = true,
        policy = ConfigurationPolicy.REQUIRE)
@Properties({
        @Property(
                name = "webconsole.configurationFactory.nameHint",
                value = "Max Age: {max.age} for Page property name: {page.property.name}",
                propertyPrivate = true),
        @Property(name = "sling.filter.scope", value = {"REQUEST", "FORWARD"})
})
public class PagePropertyBasedDispatcherMaxAgeHeaderFilter extends DispatcherMaxAgeHeaderFilter {

    private static final Logger log = LoggerFactory.getLogger(PagePropertyBasedDispatcherMaxAgeHeaderFilter.class);

    @Property(label = "Page property",
            description = "Page property to check on how long you want to cache this page")
    public static final String PROP_PAGE_PROPERTY_NAME = "page.property.name";

    @Property(label = "Inherit property value",
            description = "Property value to skip this filter and inherit another lower service ranking dispatcher ttl filter")
    public static final String PROP_INHERIT_PROPERTY_VALUE = "inherit.property.value";

    private String pagePropertyName;
    private String inheritPropertyValue;
    private static final String SERVICE_NAME = "page-property-dispatcher-header-filter";

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
            Resource pageContentResource = getPageContentResource(request, resourceResolver);
            if (pageContentResource == null) {
                log.debug("Request is not a page content request, not accepting");
                return false;
            }
            String headerValue = pageContentResource.getValueMap().get(pagePropertyName, String.class);
            if (!StringUtils.isBlank(headerValue) && !inheritPropertyValue.equals(headerValue)) {
                log.debug("Found a max age header value for request {} that is not the inherit value, accepting", pageContentResource.getPath());
                return true;
            }
            log.debug("Page content resource property is blank or INHERIT, not taking this filter ");
            return false;
        } catch (LoginException e) {
            log.error("Could not get resource resolver", e);
        }
        return false;
    }

    @Override
    protected String getHeaderValue(HttpServletRequest request) {
        try (ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(AUTH_INFO)) {
            Resource pageContentResource = getPageContentResource(request, resourceResolver);
            if (pageContentResource != null) {
                String headerValue = pageContentResource.getValueMap().get(pagePropertyName, String.class);
                return HEADER_PREFIX + headerValue;
            }
        } catch (LoginException e) {
            log.error("Could not get resource resolver", e);
        }
        log.debug("An error occurred, falling back to the default max age value of this filter");
        return super.getHeaderValue(request);
    }

    private Resource getPageContentResource(HttpServletRequest request, ResourceResolver resourceResolver) {
        PathInfo pathInfo = new PathInfo(request.getRequestURI());
        Resource reqResource = resourceResolver.getResource(pathInfo.getResourcePath());
        if (reqResource != null) {
            return reqResource.getChild(JcrConstants.JCR_CONTENT);
        }
        return null;
    }

    @SuppressWarnings("squid:S1149")
    protected final void doActivate(ComponentContext context) throws Exception {
        super.doActivate(context);
        Dictionary<?, ?> properties = context.getProperties();

        pagePropertyName = PropertiesUtil.toString(properties.get(PROP_PAGE_PROPERTY_NAME), null);
        if (pagePropertyName == null) {
            throw new ConfigurationException(PROP_PAGE_PROPERTY_NAME, "At least one resource type must be specified.");
        }
        inheritPropertyValue = PropertiesUtil.toString(properties.get(PROP_INHERIT_PROPERTY_VALUE), "INHERIT");
    }

    public String toString() {
        return this.getClass().getName() + "[fallback-max-age:" + super.getHeaderValue(null) + "]";
    }

}
