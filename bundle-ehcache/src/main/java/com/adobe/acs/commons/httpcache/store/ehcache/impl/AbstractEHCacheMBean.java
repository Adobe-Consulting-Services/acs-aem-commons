package com.adobe.acs.commons.httpcache.store.ehcache.impl;

import com.adobe.acs.commons.util.AbstractCacheMBean;
import com.google.common.cache.CacheStats;
import org.ehcache.Cache;
import org.ehcache.core.statistics.CacheStatistics;

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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * WHAT IS IT ???
 * <p>
 * WHAT PURPOSE THAT IT HAS ???
 * </p>
 *
 * @author niek.raaijkmakers@external.cybercon.de
 * @since 2018-07-12
 */
public abstract class AbstractEHCacheMBean<K, V> extends AbstractCacheMBean<K,V> {

    public <T> AbstractEHCacheMBean(T implementation, Class<T> mbeanInterface) throws NotCompliantMBeanException {
        super(implementation, mbeanInterface);
    }

    protected AbstractEHCacheMBean(Class<?> mbeanInterface) throws NotCompliantMBeanException {
        super(mbeanInterface);
    }

    protected abstract Cache<K, V> getCache();

    @Override
    public final void clearCache() {
        getCache().clear();
    }

    @Override protected final Map<K, V> getCacheAsMap()
    {
        Set<K> collectedKeys = new HashSet<K>();
        getCache().iterator().forEachRemaining((entry) -> collectedKeys.add(entry.getKey()));
        return getCache().getAll(collectedKeys);
    }

    protected abstract CacheStatistics getStatistics();

    @Override
    @SuppressWarnings("squid:S1192")
    public final TabularData getCacheStats() throws OpenDataException {
        // Exposing all google guava stats.
        final CompositeType cacheEntryType = new CompositeType(JMX_PN_CACHESTATS, JMX_PN_CACHESTATS,
                new String[] { JMX_PN_STAT, JMX_PN_VALUE }, new String[] { JMX_PN_STAT, JMX_PN_VALUE },
                new OpenType[] { SimpleType.STRING, SimpleType.STRING });

        final TabularDataSupport tabularData = new TabularDataSupport(
                new TabularType(JMX_PN_CACHESTATS, JMX_PN_CACHESTATS, cacheEntryType, new String[] { JMX_PN_STAT }));

        CacheStatistics cacheStats = getStatistics();

        final Map<String, Object> row = new HashMap<String, Object>();

        row.put(JMX_PN_STAT, "Request Count");
        row.put(JMX_PN_VALUE, String.valueOf(cacheStats.getTierStatistics().get("OnHeap").getMappings()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put(JMX_PN_STAT, "Hit Count");
        row.put(JMX_PN_VALUE, String.valueOf(cacheStats.getTierStatistics().get("OnHeap").getMappings()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put(JMX_PN_STAT, "Hit Rate");
        row.put(JMX_PN_VALUE, String.valueOf(cacheStats.getTierStatistics().get("OnHeap").getMappings()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put(JMX_PN_STAT, "Miss Count");
        row.put(JMX_PN_VALUE, String.valueOf(cacheStats.getTierStatistics().get("OnHeap").getMappings()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put(JMX_PN_STAT, "Miss Rate");
        row.put(JMX_PN_VALUE, String.valueOf(cacheStats.getTierStatistics().get("OnHeap").getMappings()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put(JMX_PN_STAT, "Eviction Count");
        row.put(JMX_PN_VALUE, String.valueOf(cacheStats.getTierStatistics().get("OnHeap").getMappings()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put(JMX_PN_STAT, "Load Count");
        row.put(JMX_PN_VALUE, String.valueOf(cacheStats.getTierStatistics().get("OnHeap").getMappings()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put(JMX_PN_STAT, "Load Exception Count");
        row.put(JMX_PN_VALUE, String.valueOf(cacheStats.getTierStatistics().get("OnHeap").getMappings()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put(JMX_PN_STAT, "Load Exception Rate");
        row.put(JMX_PN_VALUE, String.valueOf(cacheStats.getTierStatistics().get("OnHeap").getMappings()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put(JMX_PN_STAT, "Load Success Count");
        row.put(JMX_PN_VALUE, String.valueOf(cacheStats.getTierStatistics().get("OnHeap").getMappings()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put(JMX_PN_STAT, "Average Load Penalty");
        row.put(JMX_PN_VALUE, String.valueOf(cacheStats.getTierStatistics().get("OnHeap").getMappings()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put(JMX_PN_STAT, "Total Load Time");
        row.put(JMX_PN_VALUE, String.valueOf(cacheStats.getTierStatistics().get("OnHeap").getMappings()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        return tabularData;
    }

}
