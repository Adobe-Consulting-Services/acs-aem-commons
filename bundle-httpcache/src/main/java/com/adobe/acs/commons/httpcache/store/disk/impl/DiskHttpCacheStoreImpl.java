package com.adobe.acs.commons.httpcache.store.disk.impl;

import com.adobe.acs.commons.httpcache.engine.CacheContent;
import com.adobe.acs.commons.httpcache.engine.CacheKey;
import com.adobe.acs.commons.httpcache.exception.HttpCacheDataStreamException;
import com.adobe.acs.commons.httpcache.store.HttpCacheStore;
import org.apache.commons.lang.NotImplementedException;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;

/**
 * Disk cache store implementation.
 */
// TODO - Take the cache root path from config.
// TODO - Make this policy = ConfigurationPolicy.REQUIRE
@Component(label = "ACS AEM Commons - HTTP Cache - Disk based cache store.",
           description = "Cache data store implementation for Disk storage.",
           metatype = true)
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
}
