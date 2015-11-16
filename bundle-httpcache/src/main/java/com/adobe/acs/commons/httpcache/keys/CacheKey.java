package com.adobe.acs.commons.httpcache.keys;

/**
 * Generic CacheKey interface that allows multiple implementations of CacheKey's via CacheKeyFactories.
 *
 * All CacheKeys are scoped to being build off the Request object.
 */
public interface CacheKey {

    /**
     * Checks if the cacheKey is invalidated by any change to the provided path.
     * @param path the path
     * @return true if changes to the provided path should invalidate this cache key entry.
     */
    boolean isInvalidatedBy(String path);
}
