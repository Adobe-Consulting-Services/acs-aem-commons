/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
 * %%
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
 * #L%
 */
package com.adobe.acs.commons.httpcache.store;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.engine.CacheContent;
import com.adobe.acs.commons.httpcache.exception.HttpCacheDataStreamException;
import com.adobe.acs.commons.httpcache.keys.CacheKey;

/**
 * Data store for persisting cache items. Data store implementation could be in-memory, disk or even JCR repository.
 * Multiple implementation of this cache store can be present at any time and they can work in conjunction.
 */
@SuppressWarnings("squid:S1214")
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
     *
     * @param key     Object holding the key attributes.
     * @param content Object holding the content which needs to be cached.
     * @throws HttpCacheDataStreamException Failure when reading the input stream.
     */
    void put(CacheKey key, CacheContent content) throws HttpCacheDataStreamException;

    /**
     * Check if there is an entry in cache for the given key.
     *
     * @param key
     * @return
     */
    boolean contains(CacheKey key);

    /**
     * Get the Cache item given a key.
     *
     * @param key bject holding the key attributes.
     * @return Object holding the content which needs to be cached. Null if key not present.
     */
    CacheContent getIfPresent(CacheKey key);

    /**
     * Get the number of entries in the cache.
     *
     * @return
     */
    long size();


    /**
     * Invalidate the given cache key.
     *
     * @param key
     */
    void invalidate(CacheKey key);

    /**
     * Invalidate all the cached items applicable for the given cache config.
     *
     * @param cacheConfig
     */
    void invalidate(HttpCacheConfig cacheConfig);

    /**
     * Invalidate all entries in the cache.
     */
    void invalidateAll();

    /**
     * Create a temp sink for stashing response stream.
     * @return
     */
    TempSink createTempSink();
}
