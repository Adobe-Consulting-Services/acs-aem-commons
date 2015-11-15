package com.adobe.acs.commons.httpcache.config;

import com.adobe.acs.commons.httpcache.exception.HttpCacheConfigConflictException;
import com.adobe.acs.commons.httpcache.exception.HttpCacheReposityAccessException;
import org.apache.sling.api.SlingHttpServletRequest;

/**
 * Resolves cache config for the given http request.
 */
public interface CacheConfigResolver {

    /**
     * Check if there is any cache config available for the given http request.
     *
     * @param request
     * @return True if at least one of the config is applicable for the given request.
     * @throws HttpCacheReposityAccessException
     */
    boolean isConfigFound(SlingHttpServletRequest request) throws HttpCacheReposityAccessException;

    /**
     * Resolve cache config for the given request.
     *
     * @param request
     * @return Cache config if available or null if not available.
     * @throws HttpCacheConfigConflictException
     * @throws HttpCacheReposityAccessException
     */
    HttpCacheConfig resolveConfig(SlingHttpServletRequest request) throws HttpCacheConfigConflictException, HttpCacheReposityAccessException;
}
