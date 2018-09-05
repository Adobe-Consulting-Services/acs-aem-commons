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
package com.adobe.acs.commons.httpcache.store.disk.impl;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.engine.CacheContent;
import com.adobe.acs.commons.httpcache.exception.HttpCacheDataStreamException;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.store.HttpCacheStore;
import com.adobe.acs.commons.httpcache.store.TempSink;
import org.apache.commons.lang.NotImplementedException;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;

/**
 * ACS AEM Commons - HTTP Cache - Disk based cache store implementation.
 */

// TODO - Placeholder component. To be implemented.
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
    public void invalidate(HttpCacheConfig cacheConfig) {
        throw new NotImplementedException();
    }

    @Override
    public void invalidateAll() {
        throw new NotImplementedException();
    }

    @Override
    public TempSink createTempSink() {
        throw new NotImplementedException();
    }
}
