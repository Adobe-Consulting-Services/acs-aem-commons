package com.adobe.acs.commons.httpcache.filter.impl;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
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

// TODO - How does this work with sling resource mapping (Vanity url).

/**
 * Intercepting sling request filter to introduce caching layer. Works with {@link com.adobe.acs.commons.httpcache
 * .engine.HttpCacheEngine} to deal with caching aspects.
 */
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
        SlingHttpServletResponse slingResponse = (SlingHttpServletResponse) response;

        HttpCacheConfig cacheConfig = null;
        boolean isResponseCacheable = false;

        try {
            // Check if the url is cacheable as per configs and rules.
            if (cacheEngine.isRequestCacheable(slingRequest)) {
                // Get the applicable cache config.
                cacheConfig = cacheEngine.getCacheConfig(slingRequest);
                // Check if cached response available for this request.
                if (cacheEngine.isCacheHit(slingRequest, cacheConfig)) {
                    // Deliver the response from cache.
                    if (cacheEngine.deliverCacheContent(slingRequest, slingResponse, cacheConfig)) {
                        return;
                    }
                } else {
                    // Mark the request as cacheable once processed.
                    isResponseCacheable = true;
                    // Wrap the response
                    slingResponse = cacheEngine.wrapResponse(slingRequest, slingResponse, cacheConfig);
                }
            }
        } catch (HttpCacheException e) {
            log.error("HttpCache exception while dealing with request. Passed on the control to filter chain.", e);
        }

        // Pass on the request to filter chain.
        chain.doFilter(request, slingResponse);

        try {
            // If the request has the attribute marked, cache the response.
            if (isResponseCacheable) {
                cacheEngine.cacheResponse(slingRequest, slingResponse, cacheConfig);
            }
        } catch (HttpCacheException e) {
            log.error("HttpCache exception while dealing with response. Returned the filter chain response", e);
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
