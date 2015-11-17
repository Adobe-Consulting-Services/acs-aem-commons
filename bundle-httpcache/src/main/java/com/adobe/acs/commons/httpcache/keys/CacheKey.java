package com.adobe.acs.commons.httpcache.keys;

/**
 * Generic CacheKey interface that allows multiple implementations of CacheKey's via CacheKeyFactories.
 *
 * All CacheKeys are scoped to being build off the Request object.
 */
public interface CacheKey {
    public String getUri();
}
