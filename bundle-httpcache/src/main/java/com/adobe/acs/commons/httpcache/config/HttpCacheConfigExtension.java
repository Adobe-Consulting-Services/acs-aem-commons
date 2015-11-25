package com.adobe.acs.commons.httpcache.config;

import com.adobe.acs.commons.httpcache.exception.HttpCacheRepositoryAccessException;
import org.apache.sling.api.SlingHttpServletRequest;

/**
 * Hook to supply custom extension to the {@link HttpCacheConfig}.
 */
public interface HttpCacheConfigExtension {
    /**
     * Examine if this extension accepts the request. <p> Implementation of <code>HttpCacheConfig.accept
     * (SlingHttpServletRequest)</code> method invokes this to check if the given cache config custom attributes matches
     * with the given request.</p>
     *
     * @param request
     * @param cacheConfig
     * @return True if this cache config extension accepts the cache config custom attributes.
     * @throws HttpCacheRepositoryAccessException
     */
    boolean accepts(SlingHttpServletRequest request, HttpCacheConfig cacheConfig) throws HttpCacheRepositoryAccessException;
}
