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
package com.adobe.acs.commons.httpcache.store.caffeine.impl;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.engine.CacheContent;
import com.adobe.acs.commons.httpcache.exception.HttpCacheDataStreamException;
import com.adobe.acs.commons.httpcache.exception.HttpCacheKeyCreationException;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.store.HttpCacheStore;
import com.adobe.acs.commons.httpcache.store.TempSink;
import com.adobe.acs.commons.httpcache.store.mem.impl.MemCachePersistenceObject;
import com.adobe.acs.commons.httpcache.store.mem.impl.MemTempSinkImpl;
import com.adobe.acs.commons.util.impl.AbstractCacheMBean;
import com.adobe.acs.commons.util.impl.exception.CacheMBeanException;
import com.github.benmanes.caffeine.cache.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.NotCompliantMBeanException;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory cache store implementation. Uses Caffeine Cache.
 */


@Component(
        label = "ACS AEM Commons - HTTP Cache - Caffeine In-Memory cache store",
        description = "Cache data store implementation for storage using Caffeine cache. The 3rd-party Caffeine OSGi bundle MUST be installed and active before the OSGi service is enabled.",
        metatype = true,
        policy = ConfigurationPolicy.REQUIRE
)
@Properties({
        @Property(
                name = HttpCacheStore.KEY_CACHE_STORE_TYPE,
                value = HttpCacheStore.VALUE_CAFFEINE_MEMORY_STORE_TYPE,
                propertyPrivate = true
        ),
        @Property(
                name = "jmx.objectname",
                value = "com.adobe.acs.commons.httpcache:type=HTTP Cache - Caffeine Cache Store",
                propertyPrivate = true
        ),
        @Property(
                name = "webconsole.configurationFactory.nameHint",
                value = "TTL: {httpcache.cachestore.caffeine.ttl}, Max size in MB: {httpcache.cachestore.caffeine.maxsize}",
                propertyPrivate = true
        )
})
@Service(HttpCacheStore.class)
public class CaffeineMemHttpCacheStoreImpl extends AbstractCaffeineCacheMBean<CacheKey, MemCachePersistenceObject> implements HttpCacheStore, CaffeineCacheMBean {
    private static final Logger log = LoggerFactory.getLogger(CaffeineMemHttpCacheStoreImpl.class);

    private static final long DEFAULT_TTL = -1L; // Defaults to -1 meaning no TTL.
    @Property(label = "TTL",
            description = "TTL for all entries in this cache in seconds. Default to -1 meaning no TTL.",
            longValue = DEFAULT_TTL)
    private static final String PROP_TTL = "httpcache.cachestore.caffeine.ttl";
    private long ttl;

    private static final long DEFAULT_MAX_SIZE_IN_MB = 10L; // Defaults to 10MB.
    @Property(label = "Maximum size of this store in MB",
            description = "Default to 10MB. If cache size goes beyond this size, least used entry will be evicted "
                    + "from the cache",
            longValue = DEFAULT_MAX_SIZE_IN_MB)
    private static final String PROP_MAX_SIZE_IN_MB = "httpcache.cachestore.caffeine.maxsize";
    private long maxSizeInMb;


    /** Megabyte to byte */
    private static final long MEGABYTE = 1024L * 1024L;

    protected static final int NANOSECOND_MODIFIER = 1000000;

    /** Cache - Uses Caffeine cache */
    private Cache<CacheKey, MemCachePersistenceObject> cache;
    private Expiry<CacheKey, MemCachePersistenceObject> expiryPolicy;

    @Activate
    protected void activate(Map<String, Object> config) {
        // Read config and populate values.
        ttl = PropertiesUtil.toLong(config.get(PROP_TTL), DEFAULT_TTL);
        maxSizeInMb = PropertiesUtil.toLong(config.get(PROP_MAX_SIZE_IN_MB), DEFAULT_MAX_SIZE_IN_MB);
        expiryPolicy = new CacheExpiryPolicy(ttl);

        // Initializing the cache.
        // If cache is present, invalidate all and reinitialize the cache.
        deactivate();

        // Recording cache usage stats enabled.
        try {
            cache = buildCache();
        } catch (Exception e) {
            log.error("The 3rd-party Caffeine OSGi bundle must be installed and active for this OSGi service to be used. Please disable this OSGi service until the required Caffeine OSGi bundle is installed", e);
            throw e;
        }
    }

    @Deactivate
    public void deactivate() {
        if (cache != null) {
            cache.invalidateAll();
        }
    }

    private Cache<CacheKey, MemCachePersistenceObject> buildCache() {
        return Caffeine.newBuilder()
                .maximumWeight(maxSizeInMb * MEGABYTE)
                .weigher(new MemCacheEntryWeigher())
                .expireAfter(expiryPolicy)
                .removalListener(new MemCacheEntryRemovalListener())
                .recordStats()
                .build();
    }

    public CaffeineMemHttpCacheStoreImpl() throws NotCompliantMBeanException {
        super(CaffeineCacheMBean.class);
    }

    @Override
    public long getTtl() {
        return ttl;
    }

    @Override
    protected long getBytesLength(MemCachePersistenceObject cacheObj) {
        return cacheObj.getBytes().length;
    }

    @Override
    protected void addCacheData(Map<String, Object> data, MemCachePersistenceObject cacheObj) {
        int hitCount = cacheObj.getHitCount();
        long size = cacheObj.getBytes().length;
        data.put(AbstractCacheMBean.JMX_PN_STATUS, cacheObj.getStatus());
        data.put(AbstractCacheMBean.JMX_PN_SIZE, FileUtils.byteCountToDisplaySize(size));
        data.put(AbstractCacheMBean.JMX_PN_CONTENTTYPE, cacheObj.getContentType());
        data.put(AbstractCacheMBean.JMX_PN_CHARENCODING, cacheObj.getCharEncoding());
        data.put(AbstractCacheMBean.JMX_PN_HITS, hitCount);
        data.put(AbstractCacheMBean.JMX_PN_TOTALSIZESERVED, FileUtils.byteCountToDisplaySize(hitCount * size));
    }

    @Override
    protected String toString(MemCachePersistenceObject cacheObj) throws CacheMBeanException {
        try {
            return IOUtils.toString(
                    new ByteArrayInputStream(cacheObj.getBytes()),
                    cacheObj.getCharEncoding());
        } catch (IOException e) {
            throw new CacheMBeanException("Error getting the content from the cacheObject", e);
        }
    }

    @Override
    protected CompositeType getCacheEntryType() throws OpenDataException {
        return new CompositeType(AbstractCacheMBean.JMX_PN_CACHEENTRY, AbstractCacheMBean.JMX_PN_CACHEENTRY,
                new String[] { AbstractCacheMBean.JMX_PN_CACHEKEY, AbstractCacheMBean.JMX_PN_STATUS, AbstractCacheMBean.JMX_PN_SIZE, AbstractCacheMBean.JMX_PN_CONTENTTYPE, AbstractCacheMBean.JMX_PN_CHARENCODING, AbstractCacheMBean.JMX_PN_HITS, AbstractCacheMBean.JMX_PN_TOTALSIZESERVED },
                new String[] { AbstractCacheMBean.JMX_PN_CACHEKEY, AbstractCacheMBean.JMX_PN_STATUS, AbstractCacheMBean.JMX_PN_SIZE, AbstractCacheMBean.JMX_PN_CONTENTTYPE, AbstractCacheMBean.JMX_PN_CHARENCODING, AbstractCacheMBean.JMX_PN_HITS, AbstractCacheMBean.JMX_PN_TOTALSIZESERVED },
                new OpenType[] { SimpleType.STRING, SimpleType.INTEGER, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.INTEGER, SimpleType.STRING });
    }

    @Override
    protected Cache<CacheKey, MemCachePersistenceObject> getCache() {
        return cache;
    }

    /**
     * Removal listener for cache entry items.
     */
    private static class MemCacheEntryRemovalListener implements RemovalListener<CacheKey, MemCachePersistenceObject> {
        @Override
        public void onRemoval(CacheKey cacheKey, MemCachePersistenceObject memCachePersistenceObject, RemovalCause removalCause) {
            //do nothing as of now
        }
    }

    /**
     * Weigher for the cache entry.
     */
    private static class MemCacheEntryWeigher implements Weigher<CacheKey, MemCachePersistenceObject> {

        @Override
        public int weigh(CacheKey memCacheKey, MemCachePersistenceObject memCachePersistenceObject) {
            // Size of the byte array.
            return memCachePersistenceObject.getBytes().length;
        }
    }

    //-------------------------<CacheStore interface specific implementation>
    @Override
    public void put(CacheKey key, CacheContent content) throws HttpCacheDataStreamException {
        cache.put(key, new MemCachePersistenceObject().buildForCaching(content.getStatus(), content.getCharEncoding(),
                content.getContentType(), content.getHeaders(), content.getInputDataStream(), content.getWriteMethod()));

    }

    @Override
    public boolean contains(CacheKey key) {
        if (null == cache.getIfPresent(key)) {
            return false;
        }
        return true;
    }

    @Override
    public CacheContent getIfPresent(CacheKey key) {
        MemCachePersistenceObject value = cache.getIfPresent(key);
        if (null == value) {
            return null;
        }

        // Increment hit count
        value.incrementHitCount();

        return new CacheContent(value.getStatus(), value.getCharEncoding(), value.getContentType(), value.getHeaders(), new
                ByteArrayInputStream(value.getBytes()));
    }

    @Override
    public long size() {
        return cache.estimatedSize();
    }

    @Override
    public void invalidate(CacheKey invalidationKey) {
        final ConcurrentMap<CacheKey, MemCachePersistenceObject> cacheAsMap = cache.asMap();

        for (CacheKey key : cacheAsMap.keySet()) {
            if (key.isInvalidatedBy(invalidationKey)) {
                cache.invalidate(key);
            }
        }
    }

    @Override
    public void invalidate(HttpCacheConfig cacheConfig) {
        ConcurrentMap<CacheKey, MemCachePersistenceObject> cacheAsMap = cache.asMap();
        for (CacheKey key : cacheAsMap.keySet()) {
            // Match the cache key with cache config.
            try {
                if (cacheConfig.knows(key)) {
                    // If matches, invalidate that particular key.
                    cache.invalidate(key);
                }
            } catch (HttpCacheKeyCreationException e) {
                log.error("Could not invalidate HTTP cache. Falling back to full cache invalidation.", e);
                this.invalidateAll();
            }
        }
    }

    @Override
    public void invalidateAll() {
        cache.invalidateAll();
    }

    @Override
    public TempSink createTempSink() {
        return new MemTempSinkImpl();
    }

    @Override
    public String getStoreType() {
        return HttpCacheStore.VALUE_CAFFEINE_MEMORY_STORE_TYPE;
    }

}
