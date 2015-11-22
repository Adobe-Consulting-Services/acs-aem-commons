package com.adobe.acs.commons.httpcache.store.disk.impl;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.engine.CacheContent;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.exception.HttpCacheDataStreamException;
import com.adobe.acs.commons.httpcache.store.HttpCacheStore;
import org.apache.commons.lang.NotImplementedException;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;

/**
 *  ACS AEM Commons - HTTP Cache - Disk based cache store
 *  Cache data store implementation for Disk storage.
 */

// TODO - Take the cache root path from config.
// TODO - Make this policy = ConfigurationPolicy.REQUIRE

@Component
@Service
@Property(name = HttpCacheStore.KEY_CACHE_STORE_TYPE,
          value = HttpCacheStore.VALUE_DISK_CACHE_STORE_TYPE,
          propertyPrivate = true)
public class DiskHttpCacheStoreImpl implements HttpCacheStore {
    @Override
    public void put(CacheKey key, CacheContent content) throws HttpCacheDataStreamException {
        throw new NotImplementedException();
    }

    @Override
    public boolean contains(CacheKey key) {
        throw new NotImplementedException();
    }

    @Override
    public CacheContent getIfPresent(CacheKey key) {
        throw new NotImplementedException();
    }

    @Override
    public long size() {
        throw new NotImplementedException();
    }

    @Override
    public void invalidate(CacheKey key) {
        throw new NotImplementedException();
    }

    @Override
    public void invalidateAll() {
        throw new NotImplementedException();
    }

    @Override
    public void invalidate(HttpCacheConfig cacheConfig) {
        
    }

    @Override
    public void invalidate(String path) {

    }
}
