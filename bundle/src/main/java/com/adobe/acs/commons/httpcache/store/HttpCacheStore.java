package com.adobe.acs.commons.httpcache.store;

import com.adobe.acs.commons.httpcache.engine.CacheContent;
import com.adobe.acs.commons.httpcache.engine.CacheKey;

/**
 * Data store for persisting cache items. Data store implementation could be in-memory, disk or even JCR repository.
 * Multiple implementation of this cache store can be present at any time and they can work in conjunction.
 */
public interface HttpCacheStore {
    /** Represents the key to find out the type of cache data store. Type could be MEM, DISK, JCR, etc. */
    String KEY_CACHE_STORE_TYPE = "httpcache.cachestore.type";
    /** Value representing in-memory type of cache store for the key {@link #KEY_CACHE_STORE_TYPE} */
    String VALUE_MEM_CACHE_STORE_TYPE = "MEM";
    /** Value representing disk type of cache store for the key {@link #KEY_CACHE_STORE_TYPE} */
    String VALUE_DISK_CACHE_STORE_TYPE = "DISK";
    /** Value representing JCR type of cache store for the key {@link #KEY_CACHE_STORE_TYPE} */
    String VALUE_JCR_CACHE_STORE_TYPE = "JCR";

    /**
     * Put an item into the cache.
     * @param key Object holding the key attributes.
     * @param content Object holding the content which needs to be cached.
     */
    void put(CacheKey key, CacheContent content);

    /**
     * Get the Cache item given a key.
     * @param key bject holding the key attributes.
     * @return Object holding the content which needs to be cached. Null if key not present.
     */
    CacheContent getIfPresent(CacheKey key);

    /**
     * Get the number of entries in the cache.
     * @return
     */
    int size();

    /**
     * Invalidate the given cache key.
     * @param key
     */
    void invalidate(CacheKey key);

    /**
     * Invalidate all entries in the cache.
     */
    void invalidateAll();
}
