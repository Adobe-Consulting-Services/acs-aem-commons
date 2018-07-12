package com.adobe.acs.commons.httpcache.store.ehcache.impl;

import com.adobe.acs.commons.util.AbstractCacheMBean;
import org.ehcache.Cache;
import org.ehcache.core.statistics.CacheStatistics;
import org.ehcache.core.statistics.TierStatistics;

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
 * AbstractEHCacheMBean
 * Contains common logic for EHCache MBean purposes, for exposing the content stores.
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

        final CacheStatistics cacheStats = getStatistics();
        final TierStatistics tierStatistics = cacheStats.getTierStatistics().get("OnHeap");
        final Map<String, Object> row = new HashMap<>();
        

        row.put(JMX_PN_STAT, "Request Count");
        row.put(JMX_PN_VALUE, String.valueOf(tierStatistics.getMappings()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put(JMX_PN_STAT, "Hit Count");
        row.put(JMX_PN_VALUE, String.valueOf(tierStatistics.getHits()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put(JMX_PN_STAT, "Miss Count");
        row.put(JMX_PN_VALUE, String.valueOf(tierStatistics.getMisses()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put(JMX_PN_STAT, "Eviction Count");
        row.put(JMX_PN_VALUE, String.valueOf(tierStatistics.getEvictions()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put(JMX_PN_STAT, "Load Count");
        row.put(JMX_PN_VALUE, String.valueOf(tierStatistics.getPuts()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put(JMX_PN_STAT, "Explicit flushes");
        row.put(JMX_PN_VALUE, String.valueOf(tierStatistics.getRemovals()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put(JMX_PN_STAT, "Total allocated in bytes");
        row.put(JMX_PN_VALUE, String.valueOf(tierStatistics.getAllocatedByteSize()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put(JMX_PN_STAT, "Total occupied in bytes");
        row.put(JMX_PN_VALUE, String.valueOf(tierStatistics.getOccupiedByteSize()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        return tabularData;
    }

}
