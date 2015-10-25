package com.adobe.acs.commons.httpcache.engine;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;

/**
 * Access gateway and controlling module for http cache sub-system. Coordinates with cache store, cache handling rules,
 * cache configs and cache invalidators.
 */
public interface HttpCacheEngine {
    /**
     * Check if the given request is cacheable. Custom cache handling rule hook {@link
     * com.adobe.acs.commons.httpcache.rule.HttpCacheHandlingRule#onRequestReceive(SlingHttpServletRequest)} exposed.
     * Cacheability can be defined if the URI of the given request qualifies to be cached per any of the supplied
     * {@link
     * com.adobe.acs.commons.httpcache.config.HttpCacheConfig}.
     *
     * @param request
     * @return True if the request is cacheable
     */
    boolean isRequestCacheable(SlingHttpServletRequest request);

    /**
     * Check if the given request can be served from available cache.
     *
     * @param request
     * @return True if the given request can be served from cache.
     */
    boolean isCacheHit(SlingHttpServletRequest request);

    /**
     * Deliver the response from the cache. Custom cache handling rule hook {@link
     * com.adobe.acs.commons.httpcache.rule.HttpCacheHandlingRule#onCacheDeliver(SlingHttpServletRequest,
     * SlingHttpServletResponse)} exposed.
     *
     * @param request
     * @param response
     */
    void deliverCacheContent(SlingHttpServletRequest request, SlingHttpServletResponse response);

    /**
     * Mark the response with an attribute that makes the response identifiable as the one that can be cached when the
     * request is processed and its response created.
     *
     * @param response
     */
    void markResponseCacheable(SlingHttpServletResponse response);

    /**
     * Check if the given response has the attribute set by {@link #markResponseCacheable(SlingHttpServletResponse)} to
     * find out if the response has to be cached.
     *
     * @param response
     * @return True if the response has the attribute which marks the response as cacheable.
     */
    boolean validateCacheableResponse(SlingHttpServletResponse response);

    /**
     * Cache the given response. Custom cache handling rule hook {@link com.adobe.acs.commons.httpcache.rule
     * .HttpCacheHandlingRule#onResponseCache(SlingHttpServletRequest,
     * SlingHttpServletResponse)} exposed.
     *
     * @param request
     * @param response
     * @return True is caching is successful.
     */
    boolean cacheResponse(SlingHttpServletRequest request, SlingHttpServletResponse response);

    /**
     * Check if the supplied JCR repository path has the potential to invalidate cache. This can be identified based on
     * the {@link com.adobe.acs.commons.httpcache.config.HttpCacheConfig}.
     *
     * @param path JCR repository path.
     * @return
     */
    boolean isPathPotentialToInvalidate(String path);

    /**
     * Invalidate the cache for the {@linkplain com.adobe.acs.commons.httpcache.config.HttpCacheConfig} which is
     * interested in the given path. Custom cache handling rule hook {@link com.adobe.acs.commons.httpcache.rule
     * .HttpCacheHandlingRule#onCacheInvalidate(String)}
     * exposed.
     *
     * @param path JCR repository path.
     * @return
     */
    boolean invalidateCache(String path);
}
