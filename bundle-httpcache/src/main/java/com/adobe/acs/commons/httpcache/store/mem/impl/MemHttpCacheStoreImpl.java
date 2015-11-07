package com.adobe.acs.commons.httpcache.store.mem.impl;

import com.adobe.acs.commons.httpcache.engine.CacheContent;
import com.adobe.acs.commons.httpcache.engine.CacheKey;
import com.adobe.acs.commons.httpcache.exception.HttpCacheDataStreamException;
import com.adobe.acs.commons.httpcache.store.HttpCacheStore;
import com.adobe.granite.jmx.annotation.AnnotatedStandardMBean;
import com.google.common.cache.*;
import org.apache.commons.lang.NotImplementedException;
import org.apache.felix.scr.annotations.*;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.NotCompliantMBeanException;
import javax.management.openmbean.TabularData;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * In-memory cache store implementation. Uses Google Guava Cache.
 */
@Component(label = "ACS AEM Commons - HTTP Cache - In-Memory cache store.",
           description = "Cache data store implementation for in-memory storage.",
           metatype = true)
@Service
@Property(name = HttpCacheStore.KEY_CACHE_STORE_TYPE,
          value = HttpCacheStore.VALUE_MEM_CACHE_STORE_TYPE,
          propertyPrivate = true)
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
    private Cache<MemCacheKey, MemCacheValue> cache;

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
            log.info("Mem cache already present. Inavlidating the cache and re-initizling it.");
        }
        if (ttl != DEFAULT_TTL) {
            // If ttl is present, attach it to cache config.
            cache = CacheBuilder.newBuilder().maximumWeight(maxSizeInMb * MEGABYTE).expireAfterWrite(ttl, TimeUnit
                    .SECONDS).removalListener(new MemCacheEntryRemovalListener()).recordStats().build();
        } else {
            // If ttl is absent, go only with the maximum weight condition.
            cache = CacheBuilder.newBuilder().maximumWeight(maxSizeInMb * MEGABYTE).
                    removalListener(new MemCacheEntryRemovalListener()).recordStats().build();
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
    private static class MemCacheEntryRemovalListener implements RemovalListener<MemCacheKey, MemCacheValue> {
        private static final Logger log = LoggerFactory.getLogger(MemCacheEntryRemovalListener.class);

        @Override
        public void onRemoval(RemovalNotification<MemCacheKey, MemCacheValue> removalNotification) {
            log.debug("Mem cache entry removed due to {} - Uri of key: {}, Groups of key: {}", removalNotification
                    .getKey().getUri(), Arrays.toString(removalNotification.getKey().getUserGroups()));
            log.debug("Mem cache entry removed due to {} ", removalNotification.getCause().name());
        }
    }

    /**
     * Weigher for the cache entry.
     */
    private static class MemCacheEntryWeigher implements Weigher<MemCacheKey, MemCacheValue> {

        @Override
        public int weigh(MemCacheKey memCacheKey, MemCacheValue memCacheValue) {
            // Size of the byte array.
            return memCacheValue.getBytes().length;
        }
    }

    @Override
    public void put(CacheKey key, CacheContent content) throws HttpCacheDataStreamException {
        cache.put(new MemCacheKey().buildForCaching(key.getUri(), key.getUserGroups()), new MemCacheValue()
                .buildForCaching(content.getCharEncoding(), content.getContentType(), content.getHeaders(), content
                        .getInputDataStream()));

    }

    @Override
    public boolean contains(CacheKey key) {
        return false;
    }

    @Override
    public CacheContent getIfPresent(CacheKey key) {
        MemCacheValue value = cache.getIfPresent(new MemCacheKey().buildForLookups(key.getUri(), key.getUserGroups()));
        return new CacheContent(value.getCharEncoding(), value.getContentType(), value.getHeaders(), new
                ByteArrayInputStream(value.getBytes()));
    }

    @Override
    public long size() {
        return cache.size();
    }

    @Override
    public void invalidate(CacheKey key) {
        cache.invalidate(new MemCacheKey().buildForLookups(key.getUri(), key.getUserGroups()));
    }

    @Override
    public void invalidateAll() {
        cache.invalidateAll();
    }

    //-------------------------<Mbean specific implementation>
    // TODO -- How do we deal with the mandate of having a constructor in OSGi service.
    public MemHttpCacheStoreImpl() throws NotCompliantMBeanException{
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
    public int getCacheSizeInKB() {
        // TODO - Query from Guava cache??
        throw new NotImplementedException();
    }

    @Override
    public long getTtl() {
        return this.ttl;
    }

    @Override
    public TabularData getCacheStats() {
        // TODO - Use Guava getStats and form jmx tabular structure.
        throw new NotImplementedException();
    }

    @Override
    public TabularData getCacheKeys() {
        // TODO - Query from Guava cache.
        throw new NotImplementedException();
    }

}
