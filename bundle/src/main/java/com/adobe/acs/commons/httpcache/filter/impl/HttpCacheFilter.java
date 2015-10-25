package com.adobe.acs.commons.httpcache.filter.impl;

import com.adobe.acs.commons.httpcache.engine.HttpCacheEngine;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.sling.SlingFilter;
import org.apache.felix.scr.annotations.sling.SlingFilterScope;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import java.io.IOException;

/**
 * Intercepting http request filter to introduce caching layer. Works with {@link com.adobe.acs.commons.httpcache
 * .engine.HttpCacheEngine} to deal with caching aspects.
 */
// TODO - Need to decide where to insert this filter in the filter processing chain.
// TODO - This filter is suppose to work only when http cache engine is active.

@SlingFilter(
        label = "ACS AEM Samples - Http Cache - Intercepting request filter for dealing with cache.",
        description = "Intercepts http requests to deal with caching.",
        generateComponent = true,
        generateService = true,
        order = 0,
        scope = SlingFilterScope.REQUEST)
public class HttpCacheFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(HttpCacheFilter.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private HttpCacheEngine cacheEngine;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        final SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;
        final SlingHttpServletResponse slingResponse = (SlingHttpServletResponse) response;
        log.trace("In HttpCache filter.");
        dealWithRequest(slingRequest, response);
        chain.doFilter(request, response);
        dealwithResponse(request, slingResponse);
    }

    private void dealwithResponse(ServletRequest request, SlingHttpServletResponse slingResponse) {

    }

    private void dealWithRequest(SlingHttpServletRequest slingRequest, ServletResponse response) {

    }

    @Override
    public void destroy() {

    }
}
