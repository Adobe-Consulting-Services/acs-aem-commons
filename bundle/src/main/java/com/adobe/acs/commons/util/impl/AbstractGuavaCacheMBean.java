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
package com.adobe.acs.commons.util.impl;

import java.util.HashMap;
import java.util.Map;
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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import com.adobe.granite.jmx.annotation.AnnotatedStandardMBean;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheStats;

public abstract class AbstractGuavaCacheMBean<K, V> extends AnnotatedStandardMBean implements GenericCacheMBean {

    public <T> AbstractGuavaCacheMBean(T implementation, Class<T> mbeanInterface) throws NotCompliantMBeanException {
        super(implementation, mbeanInterface);
    }

    protected AbstractGuavaCacheMBean(Class<?> mbeanInterface) throws NotCompliantMBeanException {
        super(mbeanInterface);
    }

    protected abstract Cache<K, V> getCache();

    protected abstract long getBytesLength(V cacheObj);

    protected abstract void addCacheData(Map<String, Object> data, V cacheObj);

    protected abstract String toString(V cacheObj) throws Exception;

    protected abstract CompositeType getCacheEntryType() throws OpenDataException;

    @Override
    public final void clearCache() {
        getCache().invalidateAll();
    }

    @Override
    public final long getCacheEntriesCount() {
        return getCache().size();
    }

    @Override
    public final String getCacheSize() {
        // Iterate through the cache entries and compute the total size of byte array.
        long size = 0L;
        ConcurrentMap<K, V> cacheAsMap = getCache().asMap();
        for (final Map.Entry<K, V> entry : cacheAsMap.entrySet()) {
            size += getBytesLength(entry.getValue());
        }

        // Convert bytes to human-friendly format
        return FileUtils.byteCountToDisplaySize(size);
    }

    @Override
    public final TabularData getCacheStats() throws OpenDataException {
        // Exposing all google guava stats.
        final CompositeType cacheEntryType = new CompositeType("Cache Stats", "Cache Stats",
                new String[] { "Stat", "Value" }, new String[] { "Stat", "Value" },
                new OpenType[] { SimpleType.STRING, SimpleType.STRING });

        final TabularDataSupport tabularData = new TabularDataSupport(
                new TabularType("Cache Stats", "Cache Stats", cacheEntryType, new String[] { "Stat" }));

        CacheStats cacheStats = getCache().stats();

        final Map<String, Object> row = new HashMap<String, Object>();

        row.put("Stat", "Request Count");
        row.put("Value", String.valueOf(cacheStats.requestCount()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put("Stat", "Hit Count");
        row.put("Value", String.valueOf(cacheStats.hitCount()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put("Stat", "Hit Rate");
        row.put("Value", String.format("%.0f%%", cacheStats.hitRate() * 100));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put("Stat", "Miss Count");
        row.put("Value", String.valueOf(cacheStats.missCount()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put("Stat", "Miss Rate");
        row.put("Value", String.format("%.0f%%", cacheStats.missRate() * 100));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put("Stat", "Eviction Count");
        row.put("Value", String.valueOf(cacheStats.evictionCount()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put("Stat", "Load Count");
        row.put("Value", String.valueOf(cacheStats.loadCount()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put("Stat", "Load Exception Count");
        row.put("Value", String.valueOf(cacheStats.loadExceptionCount()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put("Stat", "Load Exception Rate");
        row.put("Value", String.format("%.0f%%", cacheStats.loadExceptionRate() * 100));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put("Stat", "Load Success Count");
        row.put("Value", String.valueOf(cacheStats.loadSuccessCount()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put("Stat", "Average Load Penalty");
        row.put("Value", String.valueOf(cacheStats.averageLoadPenalty()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put("Stat", "Total Load Time");
        row.put("Value", String.valueOf(cacheStats.totalLoadTime()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        return tabularData;
    }

    @Override
    public final TabularData getCacheKeys() throws OpenDataException {
        final CompositeType cacheEntryType = getCacheEntryType();

        final TabularDataSupport tabularData = new TabularDataSupport(
                new TabularType("Cache Entries", "Cache Entries", cacheEntryType, new String[] { "Cache Key" }));

        ConcurrentMap<K, V> cacheAsMap = getCache().asMap();
        for (final Map.Entry<K, V> entry : cacheAsMap.entrySet()) {
            final Map<String, Object> data = new HashMap<String, Object>();
            data.put("Cache Key", entry.getKey().toString());

            V cacheObj = entry.getValue();
            if (cacheObj != null) {
                addCacheData(data, cacheObj);
            }

            tabularData.put(new CompositeDataSupport(cacheEntryType, data));
        }

        return tabularData;
    }

    @Override
    public String getCacheEntry(String cacheKeyStr) throws Exception {
        K cacheKey = null;

        for (K cacheKeyTmp : getCache().asMap().keySet()) {
            if (StringUtils.equals(cacheKeyStr, cacheKeyTmp.toString())) {
                cacheKey = cacheKeyTmp;
                break;
            }
        }

        if (cacheKey != null) {
            V persistenceObject = getCache().getIfPresent(cacheKey);
            if (persistenceObject != null) {
                return toString(persistenceObject);
            }
        }

        return "Invalid cache key parameter.";
    }
}
