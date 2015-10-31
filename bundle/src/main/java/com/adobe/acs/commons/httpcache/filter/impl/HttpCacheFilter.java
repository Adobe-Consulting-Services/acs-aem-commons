package com.adobe.acs.commons.httpcache.filter.impl;

import com.adobe.acs.commons.httpcache.engine.HttpCacheEngine;
import com.adobe.acs.commons.httpcache.exception.HttpCacheException;
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
 * Intercepting sling request filter to introduce caching layer. Works with {@link com.adobe.acs.commons.httpcache
 * .engine.HttpCacheEngine} to deal with caching aspects.
 */
// TODO - Need to decide where to insert this filter in the filter processing chain.
// TODO - How does this work when there is sling mapping (Vanity url).
// TODO - How does the clustering of servers impact this.

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
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        log.trace("In HttpCache filter.");

        final SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;
        final SlingHttpServletResponse slingResponse = (SlingHttpServletResponse) response;


        try {
            // Check if the url is cacheable as per configs and rules.
            if (cacheEngine.isRequestCacheable(slingRequest)) {
                // Check if cached response available for this request.
                if (cacheEngine.isCacheHit(slingRequest)) {
                    // Deliver the response from cache.
                    cacheEngine.deliverCacheContent(slingRequest, slingResponse);
                    log.debug("Response delivered from cache for the url - {}", slingRequest.getRequestURI());
                    return;
                } else {
                    // Mark the response as cacheable once processed.
                    cacheEngine.markResponseCacheable(slingResponse);
                }
            }
        } catch (HttpCacheException e) {
            log.error("HttpCache exception while dealing with request. Did nothing. Passed on the control to filter "
                    + "chain.", e);
        }

        // Pass on the request to filter chain.
        chain.doFilter(request, response);

        try {
            // If the response has the attribute marked, cache the response.
            if (cacheEngine.validateCacheableResponse(slingResponse)) {
                cacheEngine.cacheResponse(slingRequest, slingResponse);
                log.debug("Response for the URI cached - {}", slingRequest.getRequestURI());
            }
        } catch (HttpCacheException e) {
            log.error("HttpCache exception while dealing with response. Did nothing. Returned the filter chain " +
                    "response", e);
        }
    }

    //---------------<Do nothing methods. Just to satisfy interface contract>
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void destroy() {
    }
}
