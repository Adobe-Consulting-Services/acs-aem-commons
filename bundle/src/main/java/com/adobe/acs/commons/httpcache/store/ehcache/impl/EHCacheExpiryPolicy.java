package com.adobe.acs.commons.httpcache.store.ehcache.impl;


import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.store.mem.MemCachePersistenceObject;
import org.ehcache.expiry.ExpiryPolicy;

import java.time.Duration;
import java.util.function.Supplier;

public class EHCacheExpiryPolicy implements ExpiryPolicy<CacheKey, MemCachePersistenceObject> {
    private final long ttl;

    public EHCacheExpiryPolicy(long ttl) {
        this.ttl = ttl;
    }

    @Override
    public Duration getExpiryForCreation(CacheKey key, MemCachePersistenceObject value) {
        long customExpiryTime = key.getExpiryForCreation();
        if(customExpiryTime > 0){
            return Duration.ofMillis(customExpiryTime);
        }else{
            if(ttl > 0){
                return Duration.ofMillis(ttl);
            }else{
                return org.ehcache.expiry.ExpiryPolicy.INFINITE;
            }
        }
    }

    @Override
    public Duration getExpiryForAccess(CacheKey key, Supplier<? extends MemCachePersistenceObject> value) {
        if(key.getExpiryForAccess() > 0){
            return Duration.ofMillis(key.getExpiryForAccess());
        }
        return null;
    }

    @Override
    public Duration getExpiryForUpdate(CacheKey key, Supplier<? extends MemCachePersistenceObject> oldValue, MemCachePersistenceObject newValue) {
        if(key.getExpiryForUpdate() > 0){
            return Duration.ofMillis(key.getExpiryForAccess());
        }
        return null;
    }
}
