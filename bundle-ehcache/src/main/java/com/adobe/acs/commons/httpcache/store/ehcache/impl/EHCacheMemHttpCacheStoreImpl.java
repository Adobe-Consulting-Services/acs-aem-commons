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
package com.adobe.acs.commons.httpcache.store.ehcache.impl;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.engine.CacheContent;
import com.adobe.acs.commons.httpcache.exception.HttpCacheDataStreamException;
import com.adobe.acs.commons.httpcache.exception.HttpCacheKeyCreationException;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.store.HttpCacheStore;
import com.adobe.acs.commons.httpcache.store.TempSink;
import com.adobe.acs.commons.httpcache.store.mem.MemCachePersistenceObject;
import com.adobe.acs.commons.httpcache.store.mem.MemTempSinkImpl;
import com.adobe.acs.commons.util.CacheMBean;
import com.adobe.acs.commons.util.exception.CacheMBeanException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.core.spi.service.StatisticsService;
import org.ehcache.core.statistics.CacheStatistics;
import org.ehcache.expiry.ExpiryPolicy;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.DynamicMBean;
import javax.management.NotCompliantMBeanException;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Supplier;

/**
 * In-memory cache store implementation. Uses Google Guava Cache.
 */
@Component(label = "ACS AEM Commons - HTTP Cache - In-Memory cache store.",
           description = "Cache data store implementation for in-memory storage.",
           metatype = true)
@Properties({
        @Property(name = HttpCacheStore.KEY_CACHE_STORE_TYPE,
                    value = HttpCacheStore.VALUE_EHCACHE_MEMORY_CACHE_STORE_TYPE,
                    propertyPrivate = true),
        @Property(name = "jmx.objectname",
                    value = "com.adobe.acs.httpcache:type=EHCache In Memory HTTP Cache Store",
                    propertyPrivate = true),
        @Property(name = "webconsole.configurationFactory.nameHint",
                    value = "TTL: {httpcache.cachestore.ehcache.ttl}, "
                            + "Max size in MB: {httpcache.cachestore.maxsize}",
                    propertyPrivate = true)
})
@Service(value = {DynamicMBean.class, HttpCacheStore.class})
public class EHCacheMemHttpCacheStoreImpl extends AbstractEHCacheMBean<CacheKey, MemCachePersistenceObject> implements HttpCacheStore{
    private static final Logger log = LoggerFactory.getLogger(EHCacheMemHttpCacheStoreImpl.class);

    /** Megabyte to byte */
    private static final long MEGABYTE = 1024L * 1024L;

    @Property(label = "TTL",
              description = "TTL for all entries in this cache in seconds. Default to -1 meaning no TTL.",
              longValue = EHCacheMemHttpCacheStoreImpl.DEFAULT_TTL)
    private static final String PROP_TTL = "httpcache.cachestore.ehcache.ttl";
    private static final long DEFAULT_TTL = -1L; // Defaults to -1 meaning no TTL.
    public static final String EH_CACHE_NAME = "memCacheStore";
    private long ttl;

    @Property(label = "Maximum size of this store in MB",
              description = "Default to 10MB. If cache size goes beyond this size, least used entry will be evicted "
                      + "from the cache",
              longValue = EHCacheMemHttpCacheStoreImpl.DEFAULT_MAX_SIZE_IN_MB)
    private static final String PROP_MAX_SIZE_IN_MB = "httpcache.cachestore.ehcache.maxsize";
    private static final long DEFAULT_MAX_SIZE_IN_MB = 10L; // Defaults to 10MB.
    private long maxSizeInMb;

    private Cache<CacheKey, MemCachePersistenceObject> cache;
    private CacheManager cacheManager;

    @Reference
    private StatisticsService statisticsService;

    @Reference
    private DynamicClassLoaderManager dynamicClassLoaderManager;
    private File storageDirectory;

    public EHCacheMemHttpCacheStoreImpl() throws NotCompliantMBeanException {
        super(CacheMBean.class);
    }

    @Activate
    protected void activate(ComponentContext context) {

        Dictionary<String, Object> configs = context.getProperties();
        ttl = PropertiesUtil.toLong(configs.get(PROP_TTL), DEFAULT_TTL);
        maxSizeInMb = PropertiesUtil.toLong(configs.get(PROP_MAX_SIZE_IN_MB), DEFAULT_MAX_SIZE_IN_MB);

        cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                .using(statisticsService)
                .withClassLoader(dynamicClassLoaderManager.getDynamicClassLoader())
                .withCache(EH_CACHE_NAME,
                        CacheConfigurationBuilder.newCacheConfigurationBuilder(CacheKey.class, MemCachePersistenceObject.class,
                                ResourcePoolsBuilder.heap(1000l)).withExpiry(getExpiryPolicy())).build(true);
        cache = cacheManager.getCache(EH_CACHE_NAME, CacheKey.class, MemCachePersistenceObject.class);

        log.info("EHCacheMemHttpCacheStoreImpl activated / modified.");
    }

    private ExpiryPolicy<CacheKey, MemCachePersistenceObject> getExpiryPolicy() {
        return new ExpiryPolicy<CacheKey, MemCachePersistenceObject>() {
            @Override
            public Duration getExpiryForCreation(CacheKey key, MemCachePersistenceObject value) {
                long customExpiryTime = key.getExpiryForCreation();
                if(customExpiryTime > 0){
                    return Duration.ofMillis(customExpiryTime);
                }else{
                    if(ttl > 0){
                        return Duration.ofMillis(ttl);
                    }else{
                        return ExpiryPolicy.INFINITE;
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
        };
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        cache.clear();
        cacheManager.close();
        log.info("EHCacheMemHttpCacheStoreImpl deactivated.");
    }

    //-------------------------<CacheStore interface specific implementation>
    @Override
    public void put(CacheKey key, CacheContent content) throws HttpCacheDataStreamException {
        cache.put(key, new MemCachePersistenceObject().buildForCaching(content.getStatus(), content.getCharEncoding(),
                content.getContentType(), content.getHeaders(), content.getInputDataStream()));
    }

    @Override
    public boolean contains(CacheKey key) {
        try{
            MemCachePersistenceObject value = cache.get(key);
            return value != null;
        }catch(NullPointerException ex){
            return false;
        }
    }

    @Override
    public CacheContent getIfPresent(CacheKey key) {
        try{
            MemCachePersistenceObject value = cache.get(key);
            // Increment hit count
            value.incrementHitCount();

            return new CacheContent(value.getStatus(), value.getCharEncoding(), value.getContentType(), value.getHeaders(), new
                    ByteArrayInputStream(value.getBytes()));
        }catch(NullPointerException ex){
            return null;
        }
    }

    @Override
    public long size() {
        CacheStatistics ehCacheStat = getStatistics();
        return ehCacheStat.getTierStatistics().get("OnHeap").getMappings();
    }

    @Override
    public void invalidate(CacheKey invalidationKey) {
        cache.remove(invalidationKey);
    }

    @Override
    public void invalidate(HttpCacheConfig cacheConfig) {
        Iterator<Cache.Entry<CacheKey, MemCachePersistenceObject>> cacheIterator = cache.iterator();

        while(cacheIterator.hasNext()){
            Cache.Entry<CacheKey, MemCachePersistenceObject> entry = cacheIterator.next();
            try {
                if (cacheConfig.knows(entry.getKey())) {
                    // If matches, invalidate that particular key.
                    cache.remove(entry.getKey());
                }
            } catch (HttpCacheKeyCreationException e) {
                log.error("Could not invalidate HTTP cache. Falling back to full cache invalidation.", e);
                this.invalidateAll();
            }
        }
    }

    @Override
    public void invalidateAll() {
        cache.clear();
    }

    @Override
    public TempSink createTempSink() {
        return new MemTempSinkImpl();
    }

    @Override
    protected Cache<CacheKey, MemCachePersistenceObject> getCache() {
        return cache;
    }

    @Override
    protected CacheStatistics getStatistics() {
        return statisticsService.getCacheStatistics(EH_CACHE_NAME);
    }

    @Override
    protected long getBytesLength(MemCachePersistenceObject cacheObj) {
        return cacheObj.getBytes().length;
    }

    @Override
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
        return new CompositeType(JMX_PN_CACHEENTRY, JMX_PN_CACHEENTRY,
                new String[] { JMX_PN_CACHEKEY, JMX_PN_STATUS, JMX_PN_SIZE, JMX_PN_CONTENTTYPE, JMX_PN_CHARENCODING, JMX_PN_HITS, JMX_PN_TOTALSIZESERVED },
                new String[] { JMX_PN_CACHEKEY, JMX_PN_STATUS, JMX_PN_SIZE, JMX_PN_CONTENTTYPE, JMX_PN_CHARENCODING, JMX_PN_HITS, JMX_PN_TOTALSIZESERVED },
                new OpenType[] { SimpleType.STRING, SimpleType.INTEGER, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.INTEGER, SimpleType.STRING });
    }

    //-------------------------<Mbean specific implementation>


}