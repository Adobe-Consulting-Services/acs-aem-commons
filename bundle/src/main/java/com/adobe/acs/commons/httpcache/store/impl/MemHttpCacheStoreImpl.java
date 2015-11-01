package com.adobe.acs.commons.httpcache.store.impl;

import com.adobe.acs.commons.httpcache.engine.CacheContent;
import com.adobe.acs.commons.httpcache.engine.CacheKey;
import com.adobe.acs.commons.httpcache.store.HttpCacheStore;
import org.apache.felix.scr.annotations.*;
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
        log.info("MemHttpCacheStoreImpl activated / modified.");
    }

    @Deactivate
    protected void deactivate(Map<String, Object> configs) {
        log.info("MemHttpCacheStoreImpl deactivated.");
    }
}
