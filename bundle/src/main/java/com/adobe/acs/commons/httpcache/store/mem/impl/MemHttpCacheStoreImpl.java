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
package com.adobe.acs.commons.httpcache.store.mem.impl;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.engine.CacheContent;
import com.adobe.acs.commons.httpcache.exception.HttpCacheDataStreamException;
import com.adobe.acs.commons.httpcache.exception.HttpCacheKeyCreationException;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.store.HttpCacheStore;
import com.adobe.acs.commons.httpcache.store.TempSink;
import com.adobe.acs.commons.util.impl.AbstractGuavaCacheMBean;
import com.adobe.acs.commons.util.impl.exception.CacheMBeanException;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.cache.Weigher;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.DynamicMBean;
import javax.management.NotCompliantMBeanException;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * In-memory cache store implementation. Uses Google Guava Cache.
 */
@Component(label = "ACS AEM Commons - HTTP Cache - In-Memory cache store.",
           description = "Cache data store implementation for in-memory storage.",
           metatype = true)
@Properties({
        @Property(name = HttpCacheStore.KEY_CACHE_STORE_TYPE,
                    value = HttpCacheStore.VALUE_MEM_CACHE_STORE_TYPE,
                    propertyPrivate = true),
        @Property(name = "jmx.objectname",
                    value = "com.adobe.acs.httpcache:type=In Memory HTTP Cache Store",
                    propertyPrivate = true),
        @Property(name = "webconsole.configurationFactory.nameHint",
                    value = "TTL: {httpcache.cachestore.memcache.ttl}, "
                            + "Max size in MB: {httpcache.cachestore.memcache.maxsize}",
                    propertyPrivate = true)
})
@Service(value = {DynamicMBean.class, HttpCacheStore.class})
public class MemHttpCacheStoreImpl extends AbstractGuavaCacheMBean<CacheKey, MemCachePersistenceObject> implements HttpCacheStore, MemCacheMBean {
    private static final Logger log = LoggerFactory.getLogger(MemHttpCacheStoreImpl.class);

    /** Megabyte to byte */
    private static final long MEGABYTE = 1024L * 1024L;

    @Property(label = "TTL",
              description = "TTL for all entries in this cache in seconds. Default to -1 meaning no TTL.",
              longValue = MemHttpCacheStoreImpl.DEFAULT_TTL)
    private static final String PROP_TTL = "httpcache.cachestore.memcache.ttl";
    private static final long DEFAULT_TTL = -1L; // Defaults to -1 meaning no TTL.
    private long ttl;

    @Property(label = "Maximum size of this store in MB",
              description = "Default to 10MB. If cache size goes beyond this size, least used entry will be evicted "
                      + "from the cache",
              longValue = MemHttpCacheStoreImpl.DEFAULT_MAX_SIZE_IN_MB)
    private static final String PROP_MAX_SIZE_IN_MB = "httpcache.cachestore.memcache.maxsize";
    private static final long DEFAULT_MAX_SIZE_IN_MB = 10L; // Defaults to 10MB.
    private long maxSizeInMb;

    /** Cache - Uses Google Guava's cache */
    private Cache<CacheKey, MemCachePersistenceObject> cache;

    @Activate
    protected void activate(Map<String, Object> configs) {
        // Read config and populate values.
        ttl = PropertiesUtil.toLong(configs.get(PROP_TTL), DEFAULT_TTL);
        maxSizeInMb = PropertiesUtil.toLong(configs.get(PROP_MAX_SIZE_IN_MB), DEFAULT_MAX_SIZE_IN_MB);

        // Initializing the cache.
        // If cache is present, invalidate all and reinitailize the cache.
        // Recording cache usage stats enabled.
        if (null != cache) {
            cache.invalidateAll();
            log.info("Mem cache already present. Invalidating the cache and re-initializing it.");
        }
        if (ttl != DEFAULT_TTL) {
            // If ttl is present, attach it to guava cache configuration.
            cache = CacheBuilder.newBuilder()
                    .maximumWeight(maxSizeInMb * MEGABYTE)
                    .weigher(new MemCacheEntryWeigher())
                    .expireAfterWrite(ttl, TimeUnit.SECONDS)
                    .removalListener(new MemCacheEntryRemovalListener())
                    .recordStats()
                    .build();
        } else {
            // If ttl is absent, go only with the maximum weight condition.
            cache = CacheBuilder.newBuilder()
                    .maximumWeight(maxSizeInMb * MEGABYTE)
                    .weigher(new MemCacheEntryWeigher())
                    .removalListener(new MemCacheEntryRemovalListener())
                    .recordStats()
                    .build();
        }

        log.info("MemHttpCacheStoreImpl activated / modified.");
    }

    @Deactivate
    protected void deactivate(Map<String, Object> configs) {
        cache.invalidateAll();
        log.info("MemHttpCacheStoreImpl deactivated.");
    }

    /**
     * Removal listener for cache entry items.
     */
    private static class MemCacheEntryRemovalListener implements RemovalListener<CacheKey, MemCachePersistenceObject> {
        private static final Logger log = LoggerFactory.getLogger(MemCacheEntryRemovalListener.class);

        @Override
        public void onRemoval(RemovalNotification<CacheKey, MemCachePersistenceObject> removalNotification) {
            log.debug("Mem cache entry for uri {} removed due to {}", removalNotification.getKey().toString(),
                    removalNotification.getCause().name());
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
                content.getContentType(), content.getHeaders(), content.getInputDataStream()));

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
        return cache.size();
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

    //-------------------------<Mbean specific implementation>

    public MemHttpCacheStoreImpl() throws NotCompliantMBeanException {
        super(MemCacheMBean.class);
    }


    @Override
    public long getTtl() {
        return this.ttl;
    }

    @Override
    protected Cache<CacheKey, MemCachePersistenceObject> getCache() {
        return cache;
    }

    @Override
    protected long getBytesLength(MemCachePersistenceObject cacheObj) {
        return cacheObj.getBytes().length;
    }

    @Override
    @SuppressWarnings("squid:S1192")
    protected void addCacheData(Map<String, Object> data, MemCachePersistenceObject cacheObj) {
        int hitCount = cacheObj.getHitCount();
        long size = cacheObj.getBytes().length;
        data.put(JMX_PN_STATUS, cacheObj.getStatus());
        data.put(JMX_PN_SIZE, FileUtils.byteCountToDisplaySize(size));
        data.put(JMX_PN_CONTENTTYPE, cacheObj.getContentType());
        data.put(JMX_PN_CHARENCODING, cacheObj.getCharEncoding());
        data.put(JMX_PN_HITS, hitCount);
        data.put(JMX_PN_TOTALSIZESERVED, FileUtils.byteCountToDisplaySize(hitCount * size));

    }

    @Override
    protected String toString(MemCachePersistenceObject cacheObj) throws CacheMBeanException{
        try {
            return IOUtils.toString(
                    new ByteArrayInputStream(cacheObj.getBytes()),
                    cacheObj.getCharEncoding());
        } catch (IOException e) {
            throw new CacheMBeanException("Error getting the content from the cacheObject", e);
        }
    }

    @Override
    @SuppressWarnings("squid:S1192")
    protected CompositeType getCacheEntryType() throws OpenDataException {
       return new CompositeType(JMX_PN_CACHEENTRY, JMX_PN_CACHEENTRY,
                new String[] { JMX_PN_CACHEKEY, JMX_PN_STATUS, JMX_PN_SIZE, JMX_PN_CONTENTTYPE, JMX_PN_CHARENCODING, JMX_PN_HITS, JMX_PN_TOTALSIZESERVED },
                new String[] { JMX_PN_CACHEKEY, JMX_PN_STATUS, JMX_PN_SIZE, JMX_PN_CONTENTTYPE, JMX_PN_CHARENCODING, JMX_PN_HITS, JMX_PN_TOTALSIZESERVED },
                new OpenType[] { SimpleType.STRING, SimpleType.INTEGER, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.INTEGER, SimpleType.STRING });

    }

}