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
package com.adobe.acs.commons.httpcache.keys;

import java.io.Serializable;

/**
 * Generic CacheKey interface that allows multiple implementations of CacheKey's via CacheKeyFactories. All CacheKeys
 * are scoped to being get off the Request object. Implementations are expected to override <code> hashCode(),
 * equals(Object), toString()</code> methods.
 */
public interface CacheKey extends Serializable
{
    /**
     * Get URI.
     * @return the universal resource id. This can be a RequestURI or a Resource path based on the context of the key.
     */
    String getUri();

    /**
     * Gets the Hierarchy Resource Path (the resourcePath above jcr:content). This is used for invalidations.
     *
     * @return the hierarchy resource path
     */
    String getHierarchyResourcePath();

    /**
     * Gets the expiry time for the cache entry creation.
     * If set, it will override the default TTL for entries to expire on cache creation.
     * Value is in miliseconds.
     * @return the expiry time
     */
    default long getExpiryForCreation(){
        return -1L;
    }

    /**
     * Gets the expiry time for the cache entry access / read.
     * If set, it will refresh the expiry time when an entry is read with given value.
     * Value is in miliseconds.
     * @return the expiry time
     */
    default long getExpiryForAccess(){
        return -1L;
    }

    /**
     * Gets the expiry time for the cache entry updated.
     * If set, it will refresh the expiry time when an entry is updated with given value.
     * Value is in miliseconds.
     * @return the expiry time
     */
    default long getExpiryForUpdate(){
        return -1L;
    }

    /**
     * Determines if the @{param cacheKey} will invalidate this cache key entry.
     *
     * @param cacheKey
     * @return true if is invalidated by, otherwise false
     */
    boolean isInvalidatedBy(CacheKey cacheKey);

    /**
     * The hashCode for the cache key.
     * @return the hash code.
     */
    int hashCode();

    /**
     * The useful string representation of this cache key. This should be generally unique as it drives display in the mbean.
     * @return the string representing the cache key for human consumption.
     */
    String toString();

    /**
     * The equals method used to match up request-derived cache keys with keys in the httpcache.
     * @param o the object to evaluate this against.
     * @return true if the objects represent the same cache item, false otherwise.
     */
    boolean equals(Object o);



}
