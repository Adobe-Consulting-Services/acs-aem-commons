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
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;
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
        final CompositeType cacheEntryType = new CompositeType(JMX_PN_CACHESTATS, JMX_PN_CACHESTATS,
                new String[] { JMX_PN_STAT, JMX_PN_VALUE }, new String[] { JMX_PN_STAT, JMX_PN_VALUE },
                new OpenType[] { SimpleType.STRING, SimpleType.STRING });

        final TabularDataSupport tabularData = new TabularDataSupport(
                new TabularType(JMX_PN_CACHESTATS, JMX_PN_CACHESTATS, cacheEntryType, new String[] { JMX_PN_STAT }));

        CacheStats cacheStats = getCache().stats();

        final Map<String, Object> row = new HashMap<String, Object>();

        row.put(JMX_PN_STAT, "Request Count");
        row.put(JMX_PN_VALUE, String.valueOf(cacheStats.requestCount()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put(JMX_PN_STAT, "Hit Count");
        row.put(JMX_PN_VALUE, String.valueOf(cacheStats.hitCount()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put(JMX_PN_STAT, "Hit Rate");
        row.put(JMX_PN_VALUE, String.format("%.0f%%", cacheStats.hitRate() * 100));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put(JMX_PN_STAT, "Miss Count");
        row.put(JMX_PN_VALUE, String.valueOf(cacheStats.missCount()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put(JMX_PN_STAT, "Miss Rate");
        row.put(JMX_PN_VALUE, String.format("%.0f%%", cacheStats.missRate() * 100));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put(JMX_PN_STAT, "Eviction Count");
        row.put(JMX_PN_VALUE, String.valueOf(cacheStats.evictionCount()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put(JMX_PN_STAT, "Load Count");
        row.put(JMX_PN_VALUE, String.valueOf(cacheStats.loadCount()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put(JMX_PN_STAT, "Load Exception Count");
        row.put(JMX_PN_VALUE, String.valueOf(cacheStats.loadExceptionCount()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put(JMX_PN_STAT, "Load Exception Rate");
        row.put(JMX_PN_VALUE, String.format("%.0f%%", cacheStats.loadExceptionRate() * 100));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put(JMX_PN_STAT, "Load Success Count");
        row.put(JMX_PN_VALUE, String.valueOf(cacheStats.loadSuccessCount()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put(JMX_PN_STAT, "Average Load Penalty");
        row.put(JMX_PN_VALUE, String.valueOf(cacheStats.averageLoadPenalty()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put(JMX_PN_STAT, "Total Load Time");
        row.put(JMX_PN_VALUE, String.valueOf(cacheStats.totalLoadTime()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        return tabularData;
    }
}
