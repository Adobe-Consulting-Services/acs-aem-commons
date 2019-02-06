/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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

package com.adobe.acs.commons.wcm.views.impl;

import com.adobe.acs.commons.util.CookieUtil;
import com.day.cq.wcm.api.NameConstants;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.WCMMode;
import com.day.cq.wcm.api.components.Component;
import com.day.cq.wcm.commons.WCMUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
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
import javax.servlet.http.Cookie;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@org.osgi.service.component.annotations.Component(
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        service = Filter.class,
        property = {
                "sling.filter.scope=component",
                "filter.order" + ":Integer=" + WCMViewsFilter.FILTER_ORDER
        }
)
@Designate(
        ocd = WCMViewsFilter.Config.class
)
public class WCMViewsFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(WCMViewsFilter.class);

    public static final int FILTER_ORDER = -500;

    public static final String COOKIE_WCM_VIEWS = "acs-commons.wcm-views";

    public static final String PN_WCM_VIEWS = "wcmViews";

    public static final String RP_WCM_VIEWS = "wcm-views";

    public static final String WCM_VIEW_DISABLED = "disabled";

    private static final String ATTR_FILTER = WCMViewsFilter.class.getName() + ".first-wcmmode";

    @ObjectClassDefinition(
            name = "ACS AEM Commons - WCM Views Filter"
    )
    public @interface Config {

        String DEFAULT_PREFIX = "/content";

        @AttributeDefinition(
                name = "Path Prefixes to Include",
                description = "Include paths that begin with these path prefixes. Default: [ /content ]",
                cardinality = Integer.MAX_VALUE,
                defaultValue = {DEFAULT_PREFIX}
        )
        String[] path$_$prefixes_include() default {DEFAULT_PREFIX};

        @AttributeDefinition(
                name = "Resource Types (Regex)",
                description = "Resource types to apply WCM Views rules to. Leave blank for all. Default: [ <Blank> ]",
                cardinality = Integer.MAX_VALUE
        )
        String[] resource$_$types_include();
    }

    private String[] includePathPrefixes = new String[]{"/content"};

    private List<Pattern> resourceTypesIncludes = new ArrayList<Pattern>();

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

            log.trace("WCM Filters does NOT accept [ {} ]", slingRequest.getResource().getPath());
            chain.doFilter(request, response);

        } else if ((CollectionUtils.isEmpty(requestViews) && CollectionUtils.isNotEmpty(componentViews))
                || (CollectionUtils.isNotEmpty(requestViews) && CollectionUtils.isEmpty(componentViews))
                || (CollectionUtils.isNotEmpty(requestViews)
                && CollectionUtils.isNotEmpty(componentViews)
                && !CollectionUtils.containsAny(requestViews, componentViews))) {

            log.trace("WCMView Empty/Not Empty -- Setting WCMMode [ {} ] for [ {} ]", WCMMode.DISABLED.name(),
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
     * Performs the filter chain inclusion, setting the WCMMode before and after the inclusion.
     *
     * @param request  the request
     * @param response the response
     * @param chain    the filter chain
     * @param before   the WCMMode to apply before the filter chaining
     * @param after    the WCMMode to apply before the filter chaining
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
     * Determines if the filter should process this request.
     *
     * @param request the request
     * @return true is the filter should attempt to process
     */
    @SuppressWarnings("squid:S3776")
    private boolean accepts(final SlingHttpServletRequest request) {
        final PageManager pageManager = request.getResourceResolver().adaptTo(PageManager.class);
        final Resource resource = request.getResource();

        // Only process requests that match the include path prefixes if any are provided
        if (ArrayUtils.isEmpty(this.includePathPrefixes)
                || (!StringUtils.startsWithAny(request.getResource().getPath(), this.includePathPrefixes))) {
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
                if (node.isNodeType(NameConstants.NT_PAGE) || node.isNodeType("cq:PageContent")  // Do not process Page node inclusions
                        || JcrConstants.JCR_CONTENT.equals(node.getName())) { // Do not process Page jcr:content nodes (that may not have the cq:PageContent jcr:primaryType)
                    return false;
                }
            } catch (RepositoryException e) {
                log.error("Repository exception prevented WCM Views Filter "
                        + "from determining if the resource is acceptable", e);
                return false;
            }
        }

        if (CollectionUtils.isNotEmpty(this.resourceTypesIncludes)) {
            for (final Pattern pattern : this.resourceTypesIncludes) {
                final Matcher matcher = pattern.matcher(resource.getResourceType());

                if (matcher.matches()) {
                    return true;
                }
            }

            return false;
        }

        return true;
    }

    /**
     * Gets or sets and gets the original WCMMode for the Request.
     *
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
     * Get the WCM Views from the Request passed by QueryParam.
     * *
     *
     * @param request the request
     * @return the WCM Views from the Request
     */
    private List<String> getRequestViews(final SlingHttpServletRequest request) {
        final List<String> views = new ArrayList<String>();

        // Respect Query Parameters first

        final RequestParameter[] requestParameters = request.getRequestParameters(RP_WCM_VIEWS);

        if (requestParameters != null) {
            for (final RequestParameter requestParameter : requestParameters) {
                if (StringUtils.isNotBlank(requestParameter.getString())) {
                    views.add(requestParameter.getString());
                }
            }
        }

        if (CollectionUtils.isNotEmpty(views)) {
            return views;
        }

        // If not Query Params can be found, check Cookie

        final Cookie cookie = CookieUtil.getCookie(request, COOKIE_WCM_VIEWS);

        if (cookie != null && StringUtils.isNotBlank(cookie.getValue())) {
            views.add(cookie.getValue());
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
    protected final void activate(WCMViewsFilter.Config config) throws Exception {
        String[] includes = config.resource$_$types_include();
        this.resourceTypesIncludes = new ArrayList<Pattern>();

        for (final String include : includes) {
            if (StringUtils.isNotBlank(include)) {
                this.resourceTypesIncludes.add(Pattern.compile(include));
            }
        }

        this.includePathPrefixes = config.path$_$prefixes_include();
    }
}
