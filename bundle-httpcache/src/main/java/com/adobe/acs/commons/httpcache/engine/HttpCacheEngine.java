package com.adobe.acs.commons.httpcache.engine;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.exception.HttpCacheConfigConflictException;
import com.adobe.acs.commons.httpcache.exception.HttpCacheException;
import com.adobe.acs.commons.httpcache.exception.HttpCachePersistenceException;
import com.adobe.acs.commons.httpcache.exception.HttpCacheReposityAccessException;
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
     * Cacheability can be defined if the URI of the given request qualifies to be cached per any of the supplied {@link
     * com.adobe.acs.commons.httpcache.config.HttpCacheConfig}.
     *
     * @param request
     * @return True if the request is cacheable
     * @throws HttpCacheReposityAccessException
     */
    boolean isRequestCacheable(SlingHttpServletRequest request) throws HttpCacheReposityAccessException;

    /**
     * Get the cache config applicable for the given request.
     *
     * @param request
     * @return Applicable CacheConfig
     * @throws HttpCacheConfigConflictException
     * @throws HttpCacheReposityAccessException
     */
    HttpCacheConfig getCacheConfig(SlingHttpServletRequest request) throws HttpCacheConfigConflictException, HttpCacheReposityAccessException;

    /**
     * Check if the given request can be served from available cache.
     *
     * @param request
     * @param cacheConfig
     * @return True if the given request can be served from cache.
     * @throws HttpCachePersistenceException
     */
    boolean isCacheHit(SlingHttpServletRequest request, HttpCacheConfig cacheConfig) throws
            HttpCachePersistenceException;

    /**
     * Deliver the response from the cache. Custom cache handling rule hook {@link com.adobe.acs.commons.httpcache
     * .rule.HttpCacheHandlingRule#onCacheDeliver(SlingHttpServletRequest, SlingHttpServletResponse)} exposed.
     *
     * @param request
     * @param response
     * @param cacheConfig
     * @throws HttpCachePersistenceException
     */
    void deliverCacheContent(SlingHttpServletRequest request, SlingHttpServletResponse response, HttpCacheConfig
            cacheConfig) throws HttpCachePersistenceException;

    /**
     * Mark the request with an attribute that makes its response identifiable as the one that can be cached when the
     * request is processed and its response created.
     *
     * @param request
     */
    void markRequestCacheable(SlingHttpServletRequest request);

    /**
     * Mark the request with an attribute that makes its response identifiable as the one that CANNOT be cached when the
     * request is processed and its response created.
     *
     * @param request
     */
    void markRequestNotCacheable(SlingHttpServletRequest request);


    /**
     * Check if the given response has the attribute set by {@link #markRequestCacheable(SlingHttpServletResponse)} to
     * find out if the response has to be cached.
     *
     * @param request
     * @return True if the response has the attribute which marks the response as cacheable.
     */
    boolean isResponseCacheable(SlingHttpServletRequest request);

    /**
     * Wrap the response so that response stream can be duplicated.
     *
     * @param request
     * @param response
     * @param httpCacheConfig
     * @return
     * @throws HttpCacheException
     */
    HttpCacheServletResponseWrapper wrapResponse(SlingHttpServletRequest request, SlingHttpServletResponse response,
                                                 HttpCacheConfig httpCacheConfig) throws HttpCacheException;

    /**
     * Cache the given response. Custom cache handling rule hook {@link com.adobe.acs.commons.httpcache.rule
     * .HttpCacheHandlingRule#onResponseCache(SlingHttpServletRequest, SlingHttpServletResponse)} exposed.
     *
     * @param request
     * @param response
     * @param cacheConfig
     */
    void cacheResponse(SlingHttpServletRequest request, SlingHttpServletResponse response, HttpCacheConfig
            cacheConfig) throws HttpCacheException;

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
     * .HttpCacheHandlingRule#onCacheInvalidate(String)} exposed.
     *
     * @param path JCR repository path.
     */
    void invalidateCache(String path) throws HttpCacheException;

    /**
     * Attribute key set on <code>SlingHttpServletRequest</code> to identify if its response is cacheable.
     */
    String FLAG_IS_REQUEST_CACHEABLE_KEY = "com.adobe.acs.commons.httpcache.engine.iscacheable";

    /**
     * Attribute value for the key <code>FLAG_IS_REQUEST_CACHEABLE_KEY</code> set on
     * <code>SlingHttpServletRequest</code> to mark that its response is cacheable.
     */
    String FLAG_IS_REQUEST_CACHEABLE_VALUE_YES = "cache";

    /**
     * Attribute value for the key <code>FLAG_IS_REQUEST_CACHEABLE_KEY</code> set on
     * <code>SlingHttpServletRequest</code> to mark that its response is not cacheable.
     */
    String FLAG_IS_REQUEST_CACHEABLE_VALUE_NO = "no-cache";
}
