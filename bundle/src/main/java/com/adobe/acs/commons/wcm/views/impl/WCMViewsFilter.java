package com.adobe.acs.commons.wcm.views.impl;

import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.WCMMode;
import com.day.cq.wcm.api.components.Component;
import com.day.cq.wcm.commons.WCMUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@org.apache.felix.scr.annotations.Component(
        label = "ACS AEM Commons - WCM Views",
        metatype = true
)
@Properties({
        @Property(
                name = "sling.filter.scope",
                value = "include",
                propertyPrivate = true
        ),
        @Property(
                name = "filter.order",
                intValue = -500,
                propertyPrivate = true
        )
})
@Service
public class WCMViewsFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(WCMViewsFilter.class);

    private static final String PN_WCM_VIEWS = "wcmViews";

    private static final String RP_WCM_VIEW = "wcm-view";

    private static final String ATTR_FILTER = WCMViewsFilter.class.getName() + ".first-wcmmode";

    private List<Pattern> resourceTypeExcludes = new ArrayList<Pattern>();
    @Property(label = "Resource Types Exclude",
            description = "Exclude resource types.",
            cardinality = 100000,
            value = { })
    public static final String PROP_RESOURCE_TYPES_EXCLUDE = "resource-types.exclude";


    private List<Pattern> resourceTypesIncludes = new ArrayList<Pattern>();
    @Property(label = "Resource Types Includes",
            description = "Include resource types.",
            cardinality = 100000,
            value = { })
    public static final String PROP_RESOURCE_TYPES_INCLUDE = "resource-types.include";


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Usually, do nothing
    }

    @Override
    public final void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {

        final SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;

        final WCMMode requestMode = this.getAndSetFirstWCMMode(slingRequest);

        final List<String> requestViews = this.getRequestViews(slingRequest);
        final List<String> componentViews = this.getComponentViews(slingRequest);

        if (!this.accepts(slingRequest)) {

            requestMode.toRequest(slingRequest);
            chain.doFilter(slingRequest, response);
            requestMode.toRequest(slingRequest);

        } else if ((CollectionUtils.isEmpty(requestViews) && CollectionUtils.isNotEmpty(componentViews))
                || (CollectionUtils.isNotEmpty(requestViews) && CollectionUtils.isEmpty(componentViews))) {

            log.debug("WCMView Empty/Not Empty -- Setting WCMMode [ {} ] for [ {} ]", WCMMode.DISABLED.name(),
                    slingRequest.getResource().getPath());

            WCMMode.DISABLED.toRequest(slingRequest);

            chain.doFilter(slingRequest, response);
            requestMode.toRequest(slingRequest);


        } else if (CollectionUtils.containsAny(requestViews, componentViews)) {

            log.debug("WCMView Match -- Setting WCMMode [ {} ] for [ {} ]", requestMode.name(),
                    slingRequest.getResource().getPath());
            requestMode.toRequest(slingRequest);

            chain.doFilter(slingRequest, response);
            requestMode.toRequest(slingRequest);

        } else {

            requestMode.toRequest(slingRequest);
            chain.doFilter(slingRequest, response);
            requestMode.toRequest(slingRequest);

        }
    }

    private boolean accepts(final SlingHttpServletRequest slingRequest) {
        final PageManager pageManager = slingRequest.getResourceResolver().adaptTo(PageManager.class);
        final Resource resource = slingRequest.getResource();

        if (pageManager.getContainingPage(slingRequest.getResource()) == null) {
            log.debug("Not a page") ;
            return false;
        }

        for (final Pattern pattern : this.resourceTypeExcludes) {
            final Matcher matcher = pattern.matcher(resource.getResourceType());

            if (matcher.matches()) {
                return false;
            }
        }

        /*
        for (final Pattern pattern : this.resourceTypesIncludes) {
            final Matcher matcher = pattern.matcher(resource.getResourceType());

            if (matcher.matches()) {
                return true;
            }
        }

        */
        return true;
    }

    private WCMMode getAndSetFirstWCMMode(final SlingHttpServletRequest slingRequest) {
        WCMMode wcmMode = (WCMMode) slingRequest.getAttribute(ATTR_FILTER);

        if (wcmMode == null) {
            wcmMode = WCMMode.fromRequest(slingRequest);
            slingRequest.setAttribute(ATTR_FILTER, wcmMode);
        }

        return wcmMode;
    }

    @Override
    public void destroy() {
        // Usually, do Nothing
    }


    private List<String> getRequestViews(final SlingHttpServletRequest request) {
        final List<String> views = new ArrayList<String>();

        final RequestParameter[] requestParameters = request.getRequestParameters(RP_WCM_VIEW);

        if (requestParameters != null) {
            for (final RequestParameter requestParameter : requestParameters) {
                if (StringUtils.isNotBlank(requestParameter.getString())) {
                    views.add(requestParameter.getString());
                }
            }
        }

        return views;
    }


    private List<String> getComponentViews(final SlingHttpServletRequest request) {
        final List<String> views = new ArrayList<String>();
        final Resource resource = request.getResource();

        if (resource == null) {
            return views;
        }

        final Component component = WCMUtils.getComponent(resource);
        final ValueMap properties = resource.adaptTo(ValueMap.class);

        if (component != null) {
            views.addAll(Arrays.asList(component.getProperties().get(PN_WCM_VIEWS, new String[]{ })));
        }

        if (properties != null) {
            views.addAll(Arrays.asList(properties.get(PN_WCM_VIEWS, new String[]{ })));
        }

        return views;
    }


    @Activate
    protected final void activate(final Map<String, String> properties) throws Exception {
        String[] excludes = PropertiesUtil.toStringArray(properties.get(PROP_RESOURCE_TYPES_EXCLUDE), new String[]{ });
        this.resourceTypeExcludes = new ArrayList<Pattern>();

        for (final String exclude : excludes) {
            this.resourceTypeExcludes.add(Pattern.compile(exclude));
        }

        String[] includes = PropertiesUtil.toStringArray(properties.get(PROP_RESOURCE_TYPES_INCLUDE), new String[]{ });
        this.resourceTypesIncludes = new ArrayList<Pattern>();

        for (final String include : includes) {
            this.resourceTypesIncludes.add(Pattern.compile(include));
        }
    }

}