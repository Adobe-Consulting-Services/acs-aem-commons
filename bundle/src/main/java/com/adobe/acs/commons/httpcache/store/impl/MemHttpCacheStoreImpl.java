package com.adobe.acs.commons.httpcache.store.impl;

import com.adobe.acs.commons.httpcache.engine.CacheContent;
import com.adobe.acs.commons.httpcache.engine.CacheKey;
import com.adobe.acs.commons.httpcache.store.HttpCacheStore;
import org.apache.felix.scr.annotations.*;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * In-memory cache store implementation.
 */
@Component(label = "ACS AEM Commons - HTTP Cache - In-Memory cache store.",
           description = "Cache data store implementation for in-memory storage.",
           metatype = true)
@Service
@Property(name = HttpCacheStore.KEY_CACHE_STORE_TYPE,
          value = HttpCacheStore.VALUE_MEM_CACHE_STORE_TYPE,
          propertyPrivate = true)
public class MemHttpCacheStoreImpl implements HttpCacheStore {
    private static final Logger log = LoggerFactory.getLogger(MemHttpCacheStoreImpl.class);

    @Property(label = "TTL",
              description = "TTL for all entries in this cache in seconds. Default to -1 meaning no TTL.",
              longValue = MemHttpCacheStoreImpl.DEFAULT_TTL)
    private static final String PROP_TTL = "httpcache.cachestore.memcache.ttl";
    private static final long DEFAULT_TTL = -1L; // Defaults to -1 meaning no TTL.
    private long ttl;

    @Property(label = "Maximum size of this store in MB",
              description = "Default to 100MB. If cache size goes beyond this size, least used entry will be evicted " +
                      "" + "from the cache",
              longValue = MemHttpCacheStoreImpl.DEFAULT_MAX_SIZE_IN_MB)
    private static final String PROP_MAX_SIZE_IN_MB = "httpcache.cachestore.memcache.maxsize";
    private static final long DEFAULT_MAX_SIZE_IN_MB = 100L; // Defaults to 100MB.
    private long maxSizeInMb;

    @Override
    public void put(CacheKey key, CacheContent content) {

    }

    @Override
    public CacheContent getIfPresent(CacheKey key) {
        return null;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public void invalidate(CacheKey key) {

    }

    @Override
    public void invalidateAll() {

    }

    @Activate
    protected void activate(Map<String, Object> configs) {
        // Read config and populate values.
        ttl = PropertiesUtil.toLong(configs.get(PROP_TTL), DEFAULT_TTL);
        maxSizeInMb = PropertiesUtil.toLong(configs.get(PROP_MAX_SIZE_IN_MB), DEFAULT_MAX_SIZE_IN_MB);
        log.info("MemHttpCacheStoreImpl activated / modified.");
    }

    @Deactivate
    protected void deactivate(Map<String, Object> configs) {
        log.info("MemHttpCacheStoreImpl deactivated.");
    }
}
