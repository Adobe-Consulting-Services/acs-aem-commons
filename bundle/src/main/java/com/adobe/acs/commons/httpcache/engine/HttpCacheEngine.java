/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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
package com.adobe.acs.commons.httpcache.engine;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.exception.HttpCacheConfigConflictException;
import com.adobe.acs.commons.httpcache.exception.HttpCacheDataStreamException;
import com.adobe.acs.commons.httpcache.exception.HttpCacheKeyCreationException;
import com.adobe.acs.commons.httpcache.exception.HttpCachePersistenceException;
import com.adobe.acs.commons.httpcache.exception.HttpCacheRepositoryAccessException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;

/**
 * Access gateway and controlling module for http cache sub-system. Coordinates with cache store, cache handling rules,
 * cache configs and cache invalidators.
 */
public interface HttpCacheEngine {
    /**
     * Check if the given request is cache-able per custom cache handling rules. Rules hook {@link
     * com.adobe.acs.commons.httpcache.rule.HttpCacheHandlingRule#onRequestReceive(SlingHttpServletRequest)} exposed.
     *
     * @param request
     * @param cacheConfig
     * @return True if the request is cache-able
     * @throws HttpCacheRepositoryAccessException
     */
    boolean isRequestCacheable(SlingHttpServletRequest request, HttpCacheConfig cacheConfig) throws
            HttpCacheRepositoryAccessException;

    /**
     * Get the first, based on cache config order, cache config applicable for the given request.
     *
     * Defaults to the Request scope.
     *
     * @param request
     * @return Applicable CacheConfig
     * @throws HttpCacheConfigConflictException When more than one cache config matches.
     * @throws HttpCacheRepositoryAccessException
     */
    HttpCacheConfig getCacheConfig(SlingHttpServletRequest request) throws HttpCacheConfigConflictException,
            HttpCacheRepositoryAccessException;

    /**
     * Get the first, based on cache config order, cache config applicable for the given request.
     *
     * @param request
     * @param filterScope
     * @return Applicable CacheConfig
     * @throws HttpCacheConfigConflictException When more than one cache config matches.
     * @throws HttpCacheRepositoryAccessException
     */
    HttpCacheConfig getCacheConfig(SlingHttpServletRequest request, HttpCacheConfig.FilterScope filterScope) throws HttpCacheConfigConflictException,
            HttpCacheRepositoryAccessException;


    /**
     * Check if the given request can be served from available cache.
     *
     * @param request
     * @param cacheConfig
     * @return True if the given request can be served from cache.
     * @throws HttpCachePersistenceException
     * @throws HttpCacheKeyCreationException
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
     * @throws HttpCachePersistenceException
     */
    HttpCacheServletResponseWrapper wrapResponse(SlingHttpServletRequest request, SlingHttpServletResponse response,
                                                 HttpCacheConfig httpCacheConfig) throws
            HttpCacheDataStreamException, HttpCacheKeyCreationException, HttpCachePersistenceException;

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
     * the {@link HttpCacheConfig}.
     *
     * @param path JCR repository path.
     * @return
     */
    boolean isPathPotentialToInvalidate(String path);

    /**
     * Invalidate the cache for the {@linkplain HttpCacheConfig} which is
     * interested in the given path. Custom cache handling rule hook {@link com.adobe.acs.commons.httpcache.rule
     * .HttpCacheHandlingRule#onCacheInvalidate(String)} exposed.
     *
     * @param path JCR repository path.
     * @throws HttpCachePersistenceException
     */
    void invalidateCache(String path) throws HttpCachePersistenceException, HttpCacheKeyCreationException;
}
