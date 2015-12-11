package com.adobe.acs.commons.httpcache.keys;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.exception.HttpCacheKeyCreationException;
import org.apache.sling.api.SlingHttpServletRequest;

/**
 * CacheKeyFactory is a OSGi Service interface that allows for consumers to generate their own CacheKey's based on their
 * out use-cases.
 * This project will provide a GroupBased CacheKey factory.
 */
public interface CacheKeyFactory {
    /**
     * Build a cache key.
     *
     * @param request
     * @param cacheConfig
     * @return
     * @throws HttpCacheKeyCreationException
     */
    CacheKey build(SlingHttpServletRequest request, HttpCacheConfig cacheConfig) throws HttpCacheKeyCreationException;

    /**
     * Does the Cache Key matches the Htt[ Cache Config.
     * @param key
     * @param cacheConfig
     * @return True if key and config match.
     * @throws HttpCacheKeyCreationException
     */
    boolean doesKeyMatchConfig(CacheKey key, HttpCacheConfig cacheConfig) throws HttpCacheKeyCreationException;
}
