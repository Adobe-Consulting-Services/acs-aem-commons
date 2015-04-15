package com.adobe.acs.commons.wcm.views.impl;

import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.WCMMode;
import com.day.cq.wcm.api.components.Component;
import com.day.cq.wcm.commons.WCMUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@org.apache.felix.scr.annotations.Component(
        label = "ACS AEM Commons - WCM Views Filter",
        metatype = true
)
@Properties({
        @Property(
                name = "sling.filter.scope",
                value = "component",
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

    public static final String PN_WCM_VIEWS = "wcmViews";

    public static final String RP_WCM_VIEWS = "wcm-views";

    public static final String WCM_VIEW_DISABLED = "disabled";


    private static final String ATTR_FILTER = WCMViewsFilter.class.getName() + ".first-wcmmode";

    private String[] includePathPrefixes = new String[]{};
    @Property(label = "Path Prefixes to Include",
            description = "Include paths that begin with these path prefixes",
            cardinality = Integer.MAX_VALUE,
            value = {})
    public static final String PROP_PATH_PREFIXES_INCLUDE = "path-prefixes.include";

    private List<Pattern> resourceTypeExcludes = new ArrayList<Pattern>();
    @Property(label = "Resource Types Exclude",
            description = "Exclude resource types.",
            cardinality = Integer.MAX_VALUE,
            value = {})
    public static final String PROP_RESOURCE_TYPES_EXCLUDE = "resource-types.exclude";


    private List<Pattern> resourceTypesIncludes = new ArrayList<Pattern>();
    @Property(label = "Resource Types Includes",
            description = "Include resource types.",
            cardinality = Integer.MAX_VALUE,
            value = {})
    public static final String PROP_RESOURCE_TYPES_INCLUDE = "resource-types.include";


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // do nothing
    }

    @Override
    public final void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {

        final SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;

        final WCMMode requestMode = this.getOrSetFirstWCMMode(slingRequest);

        final List<String> requestViews = this.getRequestViews(slingRequest);
        final List<String> componentViews = this.getComponentViews(slingRequest);

        if (!this.accepts(slingRequest)) {

            log.debug("WCM Filters does NOT accept [ {} ]", slingRequest.getResource().getPath());
            chain.doFilter(request, response);

        } else if ((CollectionUtils.isEmpty(requestViews) && CollectionUtils.isNotEmpty(componentViews))
                || (CollectionUtils.isNotEmpty(requestViews) && CollectionUtils.isEmpty(componentViews))
                || (CollectionUtils.isNotEmpty(requestViews)
                    && CollectionUtils.isNotEmpty(componentViews)
                    && !CollectionUtils.containsAny(requestViews, componentViews))) {

            log.debug("{} containsAny {}", requestViews, componentViews);
            log.debug("WCMView Empty/Not Empty -- Setting WCMMode [ {} ] for [ {} ]", WCMMode.DISABLED.name(),
                    slingRequest.getResource().getPath());

            this.processChain(slingRequest, response, chain, WCMMode.DISABLED, requestMode);

        } else if (CollectionUtils.containsAny(requestViews, componentViews)) {

            log.debug("WCMView Match -- Setting WCMMode [ {} ] for [ {} ]", requestMode.name(),
                    slingRequest.getResource().getPath());

            this.processChain(slingRequest, response, chain, requestMode, requestMode);

        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
        // Do Nothing
    }

    /**
     * Performs the filter chain inclusion, setting the WCMMode before and after the inclusion
     * @param request the request
     * @param response the response
     * @param chain the filter chain
     * @param before the WCMMode to apply before the filter chaining
     * @param after the WCMMode to apply before the filter chaining
     * @throws IOException
     * @throws ServletException
     */
    private void processChain(final ServletRequest request,
                              final ServletResponse response,
                              final FilterChain chain,
                              final WCMMode before, final WCMMode after) throws IOException, ServletException {

        before.toRequest(request);
        chain.doFilter(request, response);
        after.toRequest(request);
    }

    /**
     * Determines if the filter should process this request
     *
     * @param request the request
     * @return true is the filter should attempt to process
     */
    private boolean accepts(final SlingHttpServletRequest request) {
        final PageManager pageManager = request.getResourceResolver().adaptTo(PageManager.class);
        final Resource resource = request.getResource();

        // Only process requests that match the include path prefixes if any are provided
        if (ArrayUtils.isNotEmpty(this.includePathPrefixes)
                && !StringUtils.startsWithAny(request.getResource().getPath(), this.includePathPrefixes)) {
            return false;
        }

        // If the WCM Views on Request is set to disabled; do not process
        if (this.getRequestViews(request).contains(WCM_VIEW_DISABLED)) {
            return false;
        }
        
        // Only process resources that are part of a Page
        if (pageManager.getContainingPage(request.getResource()) == null) {
            return false;
        }

        final Node node = request.getResource().adaptTo(Node.class);

        if (node != null) {
            try {
                // Do not process cq:Page or cq:PageContent nodes as this will break all sorts of things, 
                // and they dont have dropzone of their own
                if (node.isNodeType("cq:Page")
                        || node.isNodeType("cq:PageContent")) {
                    // Do not process Page node inclusions
                    return false;
                } else if ("jcr:content".equals(node.getName())) {
                    // Do not process Page jcr:content nodes (that may not have the cq:PageContent jcr:primaryType)
                    return false;
                }
            } catch (RepositoryException e) {
                log.error("Repository exception prevented WCM Views Filter from determining if the resource is acceptable", e);
                return false;
            }
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

    /**
     * Gets or sets and gets the original WCMMode for the Request
     * @param request the Request
     * @return the original WCMMode for the Request
     */
    private WCMMode getOrSetFirstWCMMode(final SlingHttpServletRequest request) {
        WCMMode wcmMode = (WCMMode) request.getAttribute(ATTR_FILTER);

        if (wcmMode == null) {
            wcmMode = WCMMode.fromRequest(request);
            request.setAttribute(ATTR_FILTER, wcmMode);
        }

        return wcmMode;
    }

    /**
     * Get the WCM Views from the Request passed by QueryParam
     * *
     * @param request the request
     * @return the WCM Views from the Request
     */
    private List<String> getRequestViews(final SlingHttpServletRequest request) {
        final List<String> views = new ArrayList<String>();

        final RequestParameter[] requestParameters = request.getRequestParameters(RP_WCM_VIEWS);

        if (requestParameters != null) {
            for (final RequestParameter requestParameter : requestParameters) {
                if (StringUtils.isNotBlank(requestParameter.getString())) {
                    views.add(requestParameter.getString());
                }
            }
        }

        return views;
    }

    /**
     * Get the WCM Views for the component; Looks at both the content resource for the special wcmViews property 
     * and looks up to the resourceType's cq:Component properties for wcmViews.
     *
     * @param request the request
     * @return the WCM Views for the component
     */
    private List<String> getComponentViews(final SlingHttpServletRequest request) {
        final Set<String> views = new HashSet<String>();
        final Resource resource = request.getResource();

        if (resource == null) {
            return new ArrayList<String>(views);
        }

        final Component component = WCMUtils.getComponent(resource);
        final ValueMap properties = resource.adaptTo(ValueMap.class);

        if (component != null) {
            views.addAll(Arrays.asList(component.getProperties().get(PN_WCM_VIEWS, new String[]{})));
        }

        if (properties != null) {
            views.addAll(Arrays.asList(properties.get(PN_WCM_VIEWS, new String[]{})));
        }

        return new ArrayList<String>(views);
    }


    @Activate
    protected final void activate(final Map<String, String> properties) throws Exception {
        String[] excludes = PropertiesUtil.toStringArray(properties.get(PROP_RESOURCE_TYPES_EXCLUDE), new String[]{});
        this.resourceTypeExcludes = new ArrayList<Pattern>();

        for (final String exclude : excludes) {
            this.resourceTypeExcludes.add(Pattern.compile(exclude));
        }

        String[] includes = PropertiesUtil.toStringArray(properties.get(PROP_RESOURCE_TYPES_INCLUDE), new String[]{});
        this.resourceTypesIncludes = new ArrayList<Pattern>();

        for (final String include : includes) {
            this.resourceTypesIncludes.add(Pattern.compile(include));
        }

        this.includePathPrefixes = PropertiesUtil.toStringArray(properties.get(PROP_PATH_PREFIXES_INCLUDE), new String[]{});
    }
}