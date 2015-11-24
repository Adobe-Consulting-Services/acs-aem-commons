package com.adobe.acs.commons.httpcache.store.mem.impl;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.engine.CacheContent;
import com.adobe.acs.commons.httpcache.exception.HttpCacheDataStreamException;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.store.HttpCacheStore;
import com.adobe.granite.jmx.annotation.AnnotatedStandardMBean;
import com.google.common.cache.*;
import org.apache.felix.scr.annotations.*;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.DynamicMBean;
import javax.management.NotCompliantMBeanException;
import javax.management.openmbean.*;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * In-memory cache store implementation. Uses Google Guava Cache.
 */
@Component(label = "ACS AEM Commons - HTTP Cache - In-Memory cache store.",
           description = "Cache data store implementation for in-memory storage.",
           metatype = true,
           immediate = true)
@Properties({@Property(name = HttpCacheStore.KEY_CACHE_STORE_TYPE,
                       value = HttpCacheStore.VALUE_MEM_CACHE_STORE_TYPE,
                       propertyPrivate = true), @Property(name = "jmx.objectname",
                                                          value = "com.adobe.acs.httpcache:type=CRX")})
@Service(value = {DynamicMBean.class, HttpCacheStore.class})
public class MemHttpCacheStoreImpl extends AnnotatedStandardMBean implements HttpCacheStore, MemCacheMBean {
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
              description = "Default to 10MB. If cache size goes beyond this size, least used entry will be evicted " +
                      "" + "from the cache",
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
            cache = CacheBuilder.newBuilder().maximumWeight(maxSizeInMb * MEGABYTE).expireAfterWrite(ttl, TimeUnit
                    .SECONDS).removalListener(new MemCacheEntryRemovalListener()).recordStats().build();
        } else {
            // If ttl is absent, go only with the maximum weight condition.
            cache = CacheBuilder.newBuilder().maximumWeight(maxSizeInMb * MEGABYTE).weigher(new MemCacheEntryWeigher
                    ()).removalListener(new MemCacheEntryRemovalListener()).recordStats().build();
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
        cache.put(key, new MemCachePersistenceObject().buildForCaching(content.getCharEncoding(), content
                .getContentType(), content.getHeaders(), content.getInputDataStream()));

    }

    @Override
    public boolean contains(CacheKey key) {
        if (null == this.getIfPresent(key)) {
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
        return new CacheContent(value.getCharEncoding(), value.getContentType(), value.getHeaders(), new
                ByteArrayInputStream(value.getBytes()));
    }

    @Override
    public long size() {
        return cache.size();
    }

    @Override
    public void invalidate(CacheKey key) {
        cache.invalidate(key);
    }

    @Override
    public void invalidateAll() {
        cache.invalidateAll();
    }

    @Override
    public void invalidate(HttpCacheConfig cacheConfig) {
        ConcurrentMap<CacheKey, MemCachePersistenceObject> cacheAsMap = cache.asMap();
        for (CacheKey key : cacheAsMap.keySet()) {
            // Match the cache key with cache config.
            if (cacheConfig.knows(key)) {
                // If matches, invalidate that particular key.
                cache.invalidate(key);
            }
        }
    }

    //-------------------------<Mbean specific implementation>
    public MemHttpCacheStoreImpl() throws NotCompliantMBeanException {
        super(MemCacheMBean.class);
    }

    @Override
    public void clearCache() {
        cache.invalidateAll();
    }

    @Override
    public long getCacheEntriesCount() {
        return this.size();
    }

    @Override
    public long getCacheSizeInBytes() {

        // Iterate through the cache entries and compute the total size of byte array.
        long size = 0L;
        ConcurrentMap<CacheKey, MemCachePersistenceObject> cacheAsMap = cache.asMap();
        for (final CacheKey key : cacheAsMap.keySet()) {
            size += cacheAsMap.get(key).getBytes().length;
        }

        return size;
    }

    @Override
    public long getTtl() {
        return this.ttl;
    }

    @Override
    public TabularData getCacheStats() throws OpenDataException {
        // Exposing all google guava stats.
        final CompositeType cacheEntryType = new CompositeType("cacheStat", "Cache Stats", new
                String[]{"averageLoadPenalty", "evictionCount", "hitCount", "hitRate", "loadCount",
                "loadExceptionCount", "loadExceptionRate", "loadSuccessCount", "missCount", "missRate", " " +
                "requestCount", "totalLoadTime"}, new String[]{"Average load penality", "Eviction Count", "Hit " +
                "Count", "Hit Rate", "Load Count", "Load Exception Count", "Load Exception Rate", "Load Success " +
                "Count", "Miss Count", "Miss Rate", "Request Count", " Total Load Time"}, new OpenType[]{SimpleType
                .DOUBLE, SimpleType.LONG, SimpleType.LONG, SimpleType.DOUBLE, SimpleType.LONG, SimpleType.LONG,
                SimpleType.DOUBLE, SimpleType.LONG, SimpleType.LONG, SimpleType.DOUBLE, SimpleType.LONG, SimpleType
                .LONG});

        final TabularDataSupport tabularData = new TabularDataSupport(new TabularType("cacheEntries", "Cache " +
                "Entries", cacheEntryType, new String[]{}));

        CacheStats cacheStats = this.cache.stats();

        final Map<String, Object> data = new HashMap<>();
        data.put("averageLoadPenalty", cacheStats.averageLoadPenalty());
        data.put("evictionCount", cacheStats.evictionCount());
        data.put("hitCount", cacheStats.hitCount());
        data.put("hitRate", cacheStats.hitRate());
        data.put("loadCount", cacheStats.loadCount());
        data.put("loadExceptionCount", cacheStats.loadExceptionCount());
        data.put("loadExceptionRate", cacheStats.loadExceptionRate());
        data.put("loadSuccessCount", cacheStats.loadSuccessCount());
        data.put("missCount", cacheStats.missCount());
        data.put("missRate", cacheStats.missRate());
        data.put("requestCount", cacheStats.requestCount());
        data.put("requestCount", cacheStats.requestCount());
        data.put("requestCount", cacheStats.totalLoadTime());
        tabularData.put(new CompositeDataSupport(cacheEntryType, data));

        return tabularData;
    }

    @Override
    public TabularData getCacheKeys() throws OpenDataException {

        final CompositeType cacheEntryType = new CompositeType("cacheEntry", "Cache Entry", new String[]{"cacheKey"},
                new String[]{"Cache Key - String representation"}, new OpenType[]{SimpleType.STRING});

        final TabularDataSupport tabularData = new TabularDataSupport(new TabularType("cacheEntries", "Cache " +
                "Entries", cacheEntryType, new String[]{"cacheKey"}));

        ConcurrentMap<CacheKey, MemCachePersistenceObject> cacheAsMap = cache.asMap();
        for (final CacheKey key : cacheAsMap.keySet()) {
            final Map<String, String> data = new HashMap<>();
            data.put("cacheKey", key.toString());
            tabularData.put(new CompositeDataSupport(cacheEntryType, data));
        }

        return tabularData;
    }

}
