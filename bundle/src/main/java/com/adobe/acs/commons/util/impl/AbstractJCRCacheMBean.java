/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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
import java.util.concurrent.atomic.AtomicLong;

public abstract class AbstractJCRCacheMBean<K,V> extends AbstractCacheMBean<K,V> implements JcrCacheMBean
{
    //statistics
    private final AtomicLong    totalLoadTime      = new AtomicLong();
    private final AtomicLong    loadCount          = new AtomicLong();
    private final AtomicLong    loadSuccessCount   = new AtomicLong();
    private final AtomicLong    loadExceptionCount = new AtomicLong();
    private final AtomicLong    hitCount           = new AtomicLong();
    private final AtomicLong    missCount          = new AtomicLong();
    private final AtomicLong    requestCount       = new AtomicLong();
    private final AtomicLong    evictionCount      = new AtomicLong();
    private final AtomicLong    totalLookupTime    = new AtomicLong();

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
        final CompositeType cacheEntryType = new CompositeType(JMX_PN_CACHESTATS, JMX_PN_CACHESTATS,
                new String[] {JMX_PN_STAT, JMX_PN_VALUE }, new String[] {JMX_PN_STAT, JMX_PN_VALUE },
                new OpenType[] { SimpleType.STRING, SimpleType.STRING });

        final TabularDataSupport tabularData = new TabularDataSupport(
                new TabularType(JMX_PN_CACHESTATS, JMX_PN_CACHESTATS, cacheEntryType, new String[] {JMX_PN_STAT}));


        final Map<String, Object> row = new HashMap<String, Object>();

        row.put(JMX_PN_STAT, "Request Count");
        row.put(JMX_PN_VALUE, String.valueOf(requestCount.get()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put(JMX_PN_STAT, "Average Lookup Time");
        row.put(JMX_PN_VALUE, String.valueOf(getAvgLookupTime()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put(JMX_PN_STAT, "Total Lookup Time");
        row.put(JMX_PN_VALUE, String.valueOf(totalLookupTime.get()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put(JMX_PN_STAT, "Hit Count");
        row.put(JMX_PN_VALUE, String.valueOf(hitCount.get()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put(JMX_PN_STAT, "Hit Rate");
        row.put(JMX_PN_VALUE, String.valueOf(getHitRate()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put(JMX_PN_STAT, "Miss Count");
        row.put(JMX_PN_VALUE, String.valueOf(missCount.get()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put(JMX_PN_STAT, "Miss Rate");
        row.put(JMX_PN_VALUE, String.valueOf(getMissRate()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put(JMX_PN_STAT, "Eviction Count");
        row.put(JMX_PN_VALUE, String.valueOf(evictionCount.get()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put(JMX_PN_STAT, "Load Count");
        row.put(JMX_PN_VALUE, String.valueOf(loadCount.get()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put(JMX_PN_STAT, "Load Exception Count");
        row.put(JMX_PN_VALUE, String.valueOf(loadExceptionCount.get()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put(JMX_PN_STAT, "Load Exception Rate");
        row.put(JMX_PN_VALUE, String.valueOf(getLoadExceptionRate()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put(JMX_PN_STAT, "Load Success Count");
        row.put(JMX_PN_VALUE, String.valueOf(loadSuccessCount.get()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put(JMX_PN_STAT, "Load Success Rate");
        row.put(JMX_PN_VALUE, String.valueOf(getLoadSuccessRate()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put(JMX_PN_STAT, "Average Load Penalty");
        row.put(JMX_PN_VALUE, String.valueOf(getAvgLoadPenalty()));
        tabularData.put(new CompositeDataSupport(cacheEntryType, row));

        row.put(JMX_PN_STAT, "Total Load Time");
        row.put(JMX_PN_VALUE, String.valueOf(totalLoadTime.get()));
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
