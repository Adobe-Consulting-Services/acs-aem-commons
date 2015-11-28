package com.adobe.acs.commons.httpcache.engine;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.engine.impl.HttpCacheServletResponseWrapper;
import com.adobe.acs.commons.httpcache.exception.*;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;

/**
 * Access gateway and controlling module for http cache sub-system. Coordinates with cache store, cache handling rules,
 * cache configs and cache invalidators.
 */
public interface HttpCacheEngine {
    /**
     * Check if the given request is cache-able. Custom cache handling rule hook {@link
     * com.adobe.acs.commons.httpcache.rule.HttpCacheHandlingRule#onRequestReceive(SlingHttpServletRequest)} exposed.
     * Cacheability can be defined if the URI of the given request qualifies to be cached per any of the supplied {@link
     * com.adobe.acs.commons.httpcache.config.HttpCacheConfig}.
     *
     * @param request
     * @return True if the request is cacheable
     * @throws HttpCacheRepositoryAccessException
     */
    boolean isRequestCacheable(SlingHttpServletRequest request) throws HttpCacheRepositoryAccessException;

    /**
     * Get the first, based on cache config order, cache config applicable for the given request.
     *
     * @param request
     * @return Applicable CacheConfig
     * @throws HttpCacheRepositoryAccessException
     */
    HttpCacheConfig getCacheConfig(SlingHttpServletRequest request) throws HttpCacheConfigConflictException,
            HttpCacheRepositoryAccessException;

    /**
     * Check if the given request can be served from available cache.
     *
     * @param request
     * @param cacheConfig
     * @return True if the given request can be served from cache.
     * @throws HttpCachePersistenceException
     */
    boolean isCacheHit(SlingHttpServletRequest request, HttpCacheConfig cacheConfig) throws
            HttpCachePersistenceException, HttpCacheKeyCreationException;

    /**
     * Deliver the response from the cache. Custom cache handling rule hook {@link com.adobe.acs.commons.httpcache
     * .rule.HttpCacheHandlingRule#onCacheDeliver(SlingHttpServletRequest, SlingHttpServletResponse)} exposed.
     *
     * @param request
     * @param response
     * @param cacheConfig
     * @return False if cache cannot deliver this.
     * @throws HttpCachePersistenceException
     * @throws HttpCacheKeyCreationException
     * @throws HttpCacheDataStreamException
     */
    boolean deliverCacheContent(SlingHttpServletRequest request, SlingHttpServletResponse response, HttpCacheConfig
            cacheConfig) throws HttpCachePersistenceException, HttpCacheKeyCreationException,
            HttpCacheDataStreamException;

    /**
     * Wrap the response so that response stream can be duplicated.
     *
     * @param request
     * @param response
     * @param httpCacheConfig
     * @return
     * @throws HttpCacheDataStreamException
     * @throws HttpCacheKeyCreationException
     */
    HttpCacheServletResponseWrapper wrapResponse(SlingHttpServletRequest request, SlingHttpServletResponse response,
                                                 HttpCacheConfig httpCacheConfig) throws
            HttpCacheDataStreamException, HttpCacheKeyCreationException;

    /**
     * Cache the given response. Custom cache handling rule hook {@link com.adobe.acs.commons.httpcache.rule
     * .HttpCacheHandlingRule#onResponseCache(SlingHttpServletRequest, SlingHttpServletResponse)} exposed.
     *
     * @param request
     * @param response
     * @param cacheConfig
     * @throws HttpCacheKeyCreationException
     * @throws HttpCacheDataStreamException
     * @throws HttpCachePersistenceException
     */
    void cacheResponse(SlingHttpServletRequest request, SlingHttpServletResponse response, HttpCacheConfig
            cacheConfig) throws HttpCacheKeyCreationException, HttpCacheDataStreamException,
            HttpCachePersistenceException;

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
     * @throws HttpCachePersistenceException
     */
    void invalidateCache(String path) throws HttpCachePersistenceException;
}
