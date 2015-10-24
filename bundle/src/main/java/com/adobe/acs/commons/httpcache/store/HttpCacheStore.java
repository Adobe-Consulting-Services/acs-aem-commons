package com.adobe.acs.commons.httpcache.store;

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
}
