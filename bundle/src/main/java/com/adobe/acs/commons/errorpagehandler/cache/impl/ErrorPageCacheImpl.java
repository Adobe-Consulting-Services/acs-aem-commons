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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.management.NotCompliantMBeanException;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.util.ResourceDataUtil;
import com.adobe.granite.jmx.annotation.AnnotatedStandardMBean;

public class ErrorPageCacheImpl extends AnnotatedStandardMBean implements ErrorPageCache, ErrorPageCacheMBean {
    private static final Logger log = LoggerFactory.getLogger(ErrorPageCacheImpl.class);

    private static final int KB_IN_BYTES = 1000;

    private final ConcurrentMap<String, CacheEntry> cache = new ConcurrentHashMap<String, CacheEntry>();

    private final int ttl;

    private final boolean serveAuthenticatedFromCache;


    public ErrorPageCacheImpl(int ttl, boolean serveAuthenticatedFromCache) throws NotCompliantMBeanException {
        super(ErrorPageCacheMBean.class);
        this.ttl = ttl;
        this.serveAuthenticatedFromCache = serveAuthenticatedFromCache;

        log.info("Starting ACS AEM Commons Error Page Handler Cache");
        log.info(" > TTL (in seconds): {}", ttl);
        log.info(" > Serve authenticated requests from cache: {}", serveAuthenticatedFromCache);
    }


    @Override
    public String get(final String path,
                            final SlingHttpServletRequest request,
                            final SlingHttpServletResponse response) {


        if (!serveAuthenticatedFromCache && !isAnonymousRequest(request)) {
            // For authenticated requests, don't return from cache
            return getIncludeAsString(path, request, response);
        }

        final long start = System.currentTimeMillis();
        CacheEntry cacheEntry = cache.get(path);
        final boolean newEntry = cacheEntry == null;

        if (newEntry || cacheEntry.isExpired(new Date())) {

            // Cache Miss
            String data = getIncludeAsString(path, request, response);

            if (data == null) {
                log.debug("Error page representation to cache is null. Setting to empty string.");
                data = "";
            }

            if (newEntry) {
                cacheEntry = new CacheEntry();
            }

            cacheEntry.setData(data);
            cacheEntry.setExpiresIn(ttl);
            cacheEntry.incrementMisses();

            if (newEntry) {
                // Add entry to cache
                cache.put(path, cacheEntry);
            }

            if (log.isDebugEnabled()) {
                final long time = System.currentTimeMillis() - start;
                log.debug("Served cache MISS for [ {} ] in [ {} ] ms", path, time);
            }

            return data;
        } else {
            // Cache Hit

            final String data = cacheEntry.getData();

            cacheEntry.incrementHits();
            cache.put(path, cacheEntry);

            if (log.isDebugEnabled()) {
                final long time = System.currentTimeMillis() - start;
                log.debug("Served cache HIT for [ {} ] in [ {} ] ms", path, time);
            }

            return data;
        }
    }

    private boolean isAnonymousRequest(final SlingHttpServletRequest request) {
        return (request.getAuthType() == null || request.getRemoteUser() == null);
    }

    /* MBean Attributes */

    @Override
    public final int getTtlInSeconds() {
        return ttl;
    }

    @Override
    public final int getTotalHits() {
        int hits = 0;

        for (final CacheEntry entry : this.cache.values()) {
            hits = hits + entry.getHits();
        }

        return hits;
    }

    @Override
    public final int getCacheEntriesCount() {
        return this.cache.size();
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
        return this.getTotalHits() + this.getTotalMisses();
    }

    @Override
    public final long getCacheSizeInKB() {
        long bytes = 0;

        for (final CacheEntry entry : this.cache.values()) {
            bytes = bytes + entry.getBytes();
        }

        return bytes / KB_IN_BYTES;
    }


    @SuppressWarnings("squid:S1192")
    public final TabularData getCacheEntries() throws OpenDataException {

        final CompositeType cacheEntryType = new CompositeType(
                "cacheEntry",
                "Cache Entry",
                new String[]{"errorPage", "hit", "miss", "hitRate", "missRate", "sizeInKB" },
                new String[]{"Error Page", "Hit", "Miss", "Hit Rate", "Miss Rate", "Size in KB" },
                new OpenType[]{SimpleType.STRING, SimpleType.INTEGER, SimpleType.INTEGER, SimpleType.FLOAT,
                        SimpleType.FLOAT, SimpleType.INTEGER }
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
            data.put("sizeInKB", cacheEntry.getBytes() / KB_IN_BYTES);

            tabularData.put(new CompositeDataSupport(cacheEntryType, data));
        }

        return tabularData;
    }

    /* MBean Operations */

    @Override
    public final void clearCache() {
        this.cache.clear();
    }

    @Override
    public final String getCacheData(final String errorPage) {
        final CacheEntry cacheEntry = this.cache.get(StringUtils.trim(errorPage));
        if (cacheEntry == null) {
            return "";
        }

        return cacheEntry.getData();
    }

    public String getIncludeAsString(final String path, final SlingHttpServletRequest slingRequest, final SlingHttpServletResponse slingResponse) {
       return ResourceDataUtil.getIncludeAsString(path, slingRequest, slingResponse);
    }
}
