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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheStats;

import javax.management.NotCompliantMBeanException;
import javax.management.openmbean.*;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractGuavaCacheMBean<K, V> extends AbstractCacheMBean<K,V> implements CacheMBean {

    public <T> AbstractGuavaCacheMBean(T implementation, Class<T> mbeanInterface) throws NotCompliantMBeanException {
        super(implementation, mbeanInterface);
    }

    protected AbstractGuavaCacheMBean(Class<?> mbeanInterface) throws NotCompliantMBeanException {
        super(mbeanInterface);
    }

    protected abstract Cache<K, V> getCache();

    @Override
    public final void clearCache() {
        getCache().invalidateAll();
    }

    @Override protected final Map<K, V> getCacheAsMap()
    {
        return getCache().asMap();
    }

    @Override
    @SuppressWarnings("squid:S1192")
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
}
