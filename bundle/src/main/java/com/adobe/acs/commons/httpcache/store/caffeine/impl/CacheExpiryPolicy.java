package com.adobe.acs.commons.httpcache.store.caffeine.impl;

import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.store.mem.impl.MemCachePersistenceObject;
import com.github.benmanes.caffeine.cache.Expiry;

import static com.adobe.acs.commons.httpcache.store.caffeine.impl.CaffeineMemHttpCacheStoreImpl.NANOSECOND_MODIFIER;


public class CacheExpiryPolicy implements Expiry<CacheKey, MemCachePersistenceObject> {

    private final long standardTTL;

    public CacheExpiryPolicy(long standardTTL) {
        this.standardTTL = standardTTL;
    }

    @Override
    public long expireAfterCreate(
            CacheKey key, MemCachePersistenceObject value, long currentTime) {
        long customExpiryTime = key.getExpiryForCreation();
        if (customExpiryTime > 0) {
            return customExpiryTime * NANOSECOND_MODIFIER;
        } else {
            if (standardTTL > 0) {
                return standardTTL * NANOSECOND_MODIFIER;
            } else {
                return Long.MAX_VALUE;
            }
        }
    }

    @Override
    public long expireAfterUpdate(
            CacheKey key, MemCachePersistenceObject value, long currentTime, long currentDuration) {
        if (key.getExpiryForUpdate() > 0) {
            return key.getExpiryForUpdate() * NANOSECOND_MODIFIER;
        }
        return currentDuration;
    }

    @Override
    public long expireAfterRead(
            CacheKey key, MemCachePersistenceObject value, long currentTime, long currentDuration) {
        if (key.getExpiryForAccess() > 0) {
            return key.getExpiryForAccess() * NANOSECOND_MODIFIER;
        }
        return currentDuration;
    }
}
