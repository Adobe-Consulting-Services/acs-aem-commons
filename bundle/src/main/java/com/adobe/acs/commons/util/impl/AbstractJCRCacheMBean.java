package com.adobe.acs.commons.util.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.NotCompliantMBeanException;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

public abstract class AbstractJCRCacheMBean<K,V> extends AbstractCacheMBean<K,V> implements JcrCacheMBean
{
    //statistics
    private final AtomicLong totalLoadTime         = new AtomicLong(),
            loadCount          = new AtomicLong(),
            loadSuccessCount   = new AtomicLong(),
            loadExceptionCount = new AtomicLong(),
            hitCount           = new AtomicLong(),
            missCount          = new AtomicLong(),
            requestCount       = new AtomicLong(),
            evictionCount      = new AtomicLong(),
            totalLookupTime    = new AtomicLong();

    public <T> AbstractJCRCacheMBean(T implementation, Class<T> mbeanInterface) throws NotCompliantMBeanException
    {
        super(implementation, mbeanInterface);
    }

    protected AbstractJCRCacheMBean(Class<?> mbeanInterface) throws NotCompliantMBeanException
    {
        super(mbeanInterface);
    }

    @Override public TabularData getCacheStats() throws OpenDataException
    {
        // Exposing all google guava stats.
        final CompositeType cacheEntryType = new CompositeType("Cache Stats", "Cache Stats",
                new String[] { "Stat", "Value" }, new String[] { "Stat", "Value" },
                new OpenType[] { SimpleType.STRING, SimpleType.STRING });

        final TabularDataSupport tabularData = new TabularDataSupport(
                new TabularType("Cache Stats", "Cache Stats", cacheEntryType, new String[] { "Stat" }));


        final Map<String, Object> row = new HashMap<String, Object>();

        row.put("Stat", "Request Count");
        row.put("Value", String.valueOf(requestCount.get()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put("Stat", "Average Lookup Time");
        row.put("Value", String.valueOf(getAvgLookupTime()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put("Stat", "Total Lookup Time");
        row.put("Value", String.valueOf(totalLookupTime.get()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put("Stat", "Hit Count");
        row.put("Value", String.valueOf(hitCount.get()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put("Stat", "Hit Rate");
        row.put("Value", String.valueOf(getHitRate()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put("Stat", "Miss Count");
        row.put("Value", String.valueOf(missCount.get()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put("Stat", "Miss Rate");
        row.put("Value", String.valueOf(getMissRate()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put("Stat", "Eviction Count");
        row.put("Value", String.valueOf(evictionCount.get()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put("Stat", "Load Count");
        row.put("Value", String.valueOf(loadCount.get()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put("Stat", "Load Exception Count");
        row.put("Value", String.valueOf(loadExceptionCount.get()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put("Stat", "Load Exception Rate");
        row.put("Value", String.valueOf(getLoadExceptionRate()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put("Stat", "Load Success Count");
        row.put("Value", String.valueOf(loadSuccessCount.get()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put("Stat", "Load Success Rate");
        row.put("Value", String.valueOf(getLoadSuccessRate()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put("Stat", "Average Load Penalty");
        row.put("Value", String.valueOf(getAvgLoadPenalty()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put("Stat", "Total Load Time");
        row.put("Value", String.valueOf(totalLoadTime.get()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        return tabularData;
    }

    @Override
    public void resetCacheStats(){
        totalLoadTime.getAndSet(0);
        loadCount.getAndSet(0);
        loadSuccessCount.getAndSet(0);
        loadExceptionCount.getAndSet(0);
        hitCount.getAndSet(0);
        missCount.getAndSet(0);
        requestCount.getAndSet(0);
        evictionCount.getAndSet(0);
        totalLookupTime.getAndSet(0);
    }

    private long getAvgLoadPenalty()
    {
        return (loadCount.get() > 0) ?  totalLoadTime.get() / loadCount.get() : 0;
    }

    private long getAvgLookupTime()
    {
        return (requestCount.get() > 0 ) ?  totalLookupTime.get() / requestCount.get() : 0;
    }

    private long getLoadSuccessRate()
    {
        return (loadCount.get() > 0 ) ? loadSuccessCount.get() / loadCount.get() * 100 : 0;
    }

    private long getLoadExceptionRate()
    {
        return (loadCount.get() > 0) ? loadExceptionCount.get() / loadCount.get() * 100 : 0;
    }

    private long getMissRate()
    {
        return (requestCount.get() > 0 ) ? missCount.get() / requestCount.get() * 100 : 0;
    }

    private long getHitRate()
    {
        return (hitCount.get() > 0) ? hitCount.get() / requestCount.get() * 100 : 0;
    }


    protected final void incrementLoadCount(){
        loadCount.incrementAndGet();
    }

    protected final void incrementLoadSuccessCount(){
        loadSuccessCount.incrementAndGet();
    }

    protected final void incrementLoadExceptionCount(){
        loadExceptionCount.incrementAndGet();
    }

    protected final void incrementHitCount(){
        hitCount.incrementAndGet();
    }

    protected final void incrementMissCount(){
        missCount.incrementAndGet();
    }

    protected final void incrementRequestCount(){
        requestCount.incrementAndGet();
    }

    protected final void incrementEvictionCount(long delta){
        evictionCount.addAndGet(delta);
    }

    protected final void incrementTotalLookupTime(long delta){
        totalLookupTime.addAndGet(delta);
    }

    protected final void incrementTotalLoadTime(long delta){
        totalLoadTime.addAndGet(delta);
    }



}
