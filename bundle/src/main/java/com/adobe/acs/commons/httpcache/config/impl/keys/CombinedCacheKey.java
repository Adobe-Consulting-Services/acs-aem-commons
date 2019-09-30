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
package com.adobe.acs.commons.httpcache.config.impl.keys;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.exception.HttpCacheKeyCreationException;
import com.adobe.acs.commons.httpcache.keys.AbstractCacheKey;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.keys.CacheKeyFactory;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.sling.api.SlingHttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Combined cache key.
 * <p>Aggregates multiple cachekeys into 1 master key.</p>
 */
public class CombinedCacheKey extends AbstractCacheKey implements CacheKey, Serializable {

    private static final Logger log = LoggerFactory.getLogger(CombinedCacheKey.class);

    private LinkedList<CacheKey> cacheKeys;

    public CombinedCacheKey(SlingHttpServletRequest request, HttpCacheConfig cacheConfig, List<CacheKeyFactory> cacheKeyFactories) {
        super(request, cacheConfig);

        this.cacheKeys = cacheKeyFactories
                .stream()
                .map((factory) -> createCacheKey(request, cacheConfig, factory))
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedList::new));
    }

    public CombinedCacheKey(String uri, HttpCacheConfig cacheConfig, List<CacheKeyFactory> cacheKeyFactories) {
        super(uri, cacheConfig);

        this.cacheKeys = cacheKeyFactories
                .stream()
                .map((factory) -> createCacheKey(uri, cacheConfig, factory))
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    public boolean equals(Object o) {

        boolean selfCheck = super.equals(o);

        if (!selfCheck) {
            return false;
        }

        CombinedCacheKey other = (CombinedCacheKey) o;

        if (other == null) {
            return false;
        }

        for (int i = 0; i < cacheKeys.size(); i++) {
            CacheKey otherDelegate = other.getDelegate(i);
            CacheKey ownDelegate = this.getDelegate(i);

            if (!otherDelegate.equals(ownDelegate)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getUri())
                .append(getAuthenticationRequirement()).toHashCode();
    }

    private CacheKey createCacheKey(SlingHttpServletRequest request, HttpCacheConfig cacheConfig, CacheKeyFactory factory) {
        try {
            return factory.build(request, cacheConfig);
        } catch (HttpCacheKeyCreationException e) {
            log.error("Error creating cache key: ", e);
        }
        return null;
    }

    private CacheKey createCacheKey(String resourcePath, HttpCacheConfig cacheConfig, CacheKeyFactory factory) {
        try {
            return factory.build(resourcePath, cacheConfig);
        } catch (HttpCacheKeyCreationException e) {
            log.error("Error creating cache key: ", e);
        }
        return null;
    }

    private CacheKey getDelegate(int position) {
        return cacheKeys.get(position);
    }

    @Override
    public String toString() {
        return this.resourcePath + "[CombinedCacheKey]" + this.cacheKeys.stream().map(CacheKey::toString).collect(Collectors.joining(";")).replaceAll(this.resourcePath, "");
    }

    /**
     * For Serialization
     **/
    private void writeObject(ObjectOutputStream o) throws IOException {
        parentWriteObject(o);
        o.writeObject(cacheKeys);
    }

    /**
     * For De serialization
     **/
    private void readObject(ObjectInputStream o)
            throws IOException, ClassNotFoundException {

        parentReadObject(o);
        cacheKeys = (LinkedList<CacheKey>) o.readObject();
    }
}
