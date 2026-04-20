/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.adobe.acs.commons.httpcache.store.caffeine.impl;

import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.store.mem.impl.MemCachePersistenceObject;
import com.github.benmanes.caffeine.cache.Expiry;

import static com.adobe.acs.commons.httpcache.store.caffeine.impl.CaffeineMemHttpCacheStoreImpl.NANOSECOND_MODIFIER;


public class CacheExpiryPolicy implements Expiry<CacheKey, MemCachePersistenceObject> {

    private final long standardTtl;

    public CacheExpiryPolicy(long standardTtl) {
        this.standardTtl = standardTtl;
    }

    @Override
    public long expireAfterCreate(
            CacheKey key, MemCachePersistenceObject value, long currentTime) {
        long customExpiryTime = key.getExpiryForCreation();
        if (customExpiryTime > 0) {
            return customExpiryTime * NANOSECOND_MODIFIER;
        } else {
            if (standardTtl > 0) {
                return standardTtl * NANOSECOND_MODIFIER;
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
