/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
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

package com.adobe.acs.commons.errorpagehandler.cache.impl;

import com.adobe.acs.commons.errorpagehandler.cache.ErrorPageCache;
import com.adobe.acs.commons.errorpagehandler.cache.ErrorPageCacheMBean;
import com.adobe.acs.commons.util.ResourceDataUtil;
import com.adobe.granite.jmx.annotation.AnnotatedStandardMBean;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.DynamicMBean;
import javax.management.NotCompliantMBeanException;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component(
        label = "ACS AEM Commons - Error Page Handler Cache",
        description = "In-memory cache for pages. Details fo cache available via the JMX console.",
        metatype = true,
        immediate = true
)
@Properties({
        @Property(
                label = "MBean Name",
                name = "jmx.objectname",
                value = "com.adobe.acs.commons:type=ErrorPageHandlerCache",
                propertyPrivate = true
        )
})
@Service(value = { DynamicMBean.class, ErrorPageCache.class })
public class ErrorPageCacheImpl extends AnnotatedStandardMBean implements ErrorPageCache, ErrorPageCacheMBean {
    private static final Logger log = LoggerFactory.getLogger(ErrorPageCacheImpl.class);

    private static final int DEFAULT_TTL = 60 * 5; // 5 Mins
    private static final int KB_IN_BYTES = 1000;

    private int ttl = DEFAULT_TTL;

    @Property(label = "TTL (in seconds)",
            description = "TTL for each cache entry in seconds. [ Default: 300 ]",
            intValue = DEFAULT_TTL)
    public static final String PROP_TTL = "ttl";


    private static final boolean DEFAULT_SERVE_AUTHENTICATED_FROM_CACHE = false;

    private boolean serveAuthenticatedFromCache = DEFAULT_SERVE_AUTHENTICATED_FROM_CACHE;

    @Property(label = "Serve authenticated from cache",
            description = "Serve authenticated requests from the error page cache. [ Default: false ]",
            boolValue = DEFAULT_SERVE_AUTHENTICATED_FROM_CACHE)
    public static final String PROP_SERVE_AUTHENTICATED_FROM_CACHE = "serve-authenticated-from-cache";

    private ConcurrentHashMap<String, CacheEntry> cache;

    public ErrorPageCacheImpl() throws NotCompliantMBeanException {
        super(ErrorPageCacheMBean.class);
    }


    @Override
    public final String get(final String path,
                            final SlingHttpServletRequest request,
                            final SlingHttpServletResponse response) {


        if (!serveAuthenticatedFromCache && !isAnonymousRequest(request)) {
            // For authenticated requests, dont return from cache
            return ResourceDataUtil.getIncludeAsString(path, request, response);
        }


        final long start = System.currentTimeMillis();

        // Lock the cache because we we increment values within the cache even on valid cache hits"
        synchronized (this.cache) {

            CacheEntry cacheEntry = cache.get(path);

            if (cacheEntry == null || cacheEntry.isExpired(new Date())) {

                // Cache Miss

                if (cacheEntry == null) {
                    cacheEntry = new CacheEntry(ttl);
                }

                final String data = ResourceDataUtil.getIncludeAsString(path, request, response);

                cacheEntry.setData(data);
                cacheEntry.resetExpiresAt(ttl);
                cacheEntry.incrementMisses();

                // Add entry to cache
                cache.put(path, cacheEntry);


                log.debug("Served cache MISS for [ {} ] in [ {} ] ms", path, System.currentTimeMillis() - start);

                return data;
            } else {
                // Cache Hit

                final String data = cacheEntry.getData();

                cacheEntry.incrementHits();
                cache.put(path, cacheEntry);

                log.debug("Served cache HIT for [ {} ] in [ {} ] ms", path, System.currentTimeMillis() - start);

                return data;
            }
        }
    }

    private boolean isAnonymousRequest(final SlingHttpServletRequest request) {
        return (request.getAuthType() == null || request.getRemoteUser() == null);
    }

    @Activate
    protected final void activate(Map<String, String> config) {
        cache = new ConcurrentHashMap<String, CacheEntry>();

        ttl = PropertiesUtil.toInteger(config.get(PROP_TTL), DEFAULT_TTL);

        serveAuthenticatedFromCache = PropertiesUtil.toBoolean(config.get(PROP_SERVE_AUTHENTICATED_FROM_CACHE),
                DEFAULT_SERVE_AUTHENTICATED_FROM_CACHE);

        log.info("Starting ACS AEM Commons Error Page Handler Cache");
        log.info(" > TTL (in seconds): {}", ttl);
        log.info(" > Serve authenticated requests from cache: {}", serveAuthenticatedFromCache);
    }

    @Deactivate
    protected final void deactivate(Map<String, String> config) {
        cache = null;
    }

    /* MBean Attributes */

    @Override
    public final int getTtlInSeconds() {
        return ttl;
    }

    @Override
    public final int getTotalHits() {
        int hits = 0;

        for (final Map.Entry<String, CacheEntry> entry : this.cache.entrySet()) {
            hits = hits + entry.getValue().getHits();
        }

        return hits;
    }

    @Override
    public final int getCacheEntriesCount() {
        if (this.cache == null) {
            return 0;
        } else {
            return this.cache.size();
        }
    }

    @Override
    public final int getTotalMisses() {
        int misses = 0;

        for (final Map.Entry<String, CacheEntry> entry : this.cache.entrySet()) {
            misses = misses + entry.getValue().getMisses();
        }

        return misses;
    }

    @Override
    public final int getTotalCacheRequests() {
        return this.getTotalHits() + getTotalMisses();
    }

    @Override
    public final long getCacheSizeInKB() {
        long bytes = 0;

        for (final Map.Entry<String, CacheEntry> entry : this.cache.entrySet()) {
            bytes = bytes + entry.getValue().getData().getBytes().length;
        }

        return bytes / KB_IN_BYTES;
    }


    public final TabularData getCacheEntries() throws OpenDataException {

        final CompositeType cacheEntryType = new CompositeType(
                "cacheEntry", /* type name */
                "Cache Entry", /* type description */
                new String[]{"errorPage", "hit", "miss", "hitRate", "missRate", "sizeInKB" }, /* item names */
                new String[]{"Error Page", "Hit", "Miss", "Hit Rate", "Miss Rate", "Size in KB" },
                /* item descriptions */
                new OpenType[]{SimpleType.STRING, SimpleType.INTEGER, SimpleType.INTEGER, SimpleType.FLOAT,
                        SimpleType.FLOAT, SimpleType.INTEGER }  /* item types */
        );

        final TabularDataSupport tabularData = new TabularDataSupport(
                new TabularType("cacheEntries",
                        "Cache Entries",
                        cacheEntryType,
                        new String[]{"errorPage" })
        );

        for (final Map.Entry<String, CacheEntry> entry : this.cache.entrySet()) {
            final CacheEntry cacheEntry = entry.getValue();

            final Map<String, Object> data = new HashMap<String, Object>();
            data.put("errorPage", entry.getKey());
            data.put("hit", cacheEntry.getHits());
            data.put("miss", cacheEntry.getMisses());
            data.put("hitRate", cacheEntry.getHitRate());
            data.put("missRate", cacheEntry.getMissRate());
            data.put("sizeInKB", cacheEntry.getData().getBytes().length / KB_IN_BYTES);

            tabularData.put(new CompositeDataSupport(cacheEntryType, data));
        }

        return tabularData;
    }

    /* MBean Operations */

    @Override
    public final void clearCache() {
        synchronized (this.cache) {
            this.cache.clear();
        }
    }


    @Override
    public final String getCacheData(final String errorPage) {
        final CacheEntry cacheEntry = this.cache.get(StringUtils.trim(errorPage));
        if (cacheEntry == null) {
            return "";
        }

        return cacheEntry.getData();
    }
}
