package com.adobe.acs.commons.httpcache.store.jcr.impl;

import com.adobe.acs.commons.httpcache.engine.CacheContent;
import com.adobe.acs.commons.httpcache.engine.CacheKey;
import com.adobe.acs.commons.httpcache.exception.HttpCacheDataStreamException;
import com.adobe.acs.commons.httpcache.store.HttpCacheStore;

/**
 *  JCR cache store implementation.
 */
// TODO - Take the cache root path from config.
public class JCRHttpCacheStoreImpl implements HttpCacheStore{
    @Override
    public void put(CacheKey key, CacheContent content) throws HttpCacheDataStreamException {

    }

    @Override
    public CacheContent getIfPresent(CacheKey key) {
        return null;
    }

    @Override
    public long size() {
        return 0;
    }

    @Override
    public void invalidate(CacheKey key) {

    }

    @Override
    public void invalidateAll() {

    }
}
