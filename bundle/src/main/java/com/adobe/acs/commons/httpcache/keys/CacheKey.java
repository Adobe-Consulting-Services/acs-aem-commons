package com.adobe.acs.commons.httpcache.keys;

/**
 * Generic CacheKey interface that allows multiple implementations of CacheKey's via CacheKeyFactories. All CacheKeys
 * are scoped to being build off the Request object. Implementations are expected to override <code> hashCode(),
 * equals(Object), toString()</code> methods.
 */
public interface CacheKey {
    /**
     * Get URI.
     * @return
     */
    String getUri();
}
