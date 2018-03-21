package com.adobe.acs.commons.util.impl;

import java.util.HashMap;
import java.util.Map;

import javax.management.NotCompliantMBeanException;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

import com.adobe.acs.commons.util.impl.exception.CacheMBeanException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import com.adobe.granite.jmx.annotation.AnnotatedStandardMBean;

public abstract class AbstractCacheMBean<K,V> extends AnnotatedStandardMBean implements  CacheMBean
{
    /** jmx labels **/
    protected static final String JMX_PN_CACHESTATS = "Cache Stats";
    protected static final String JMX_PN_STAT = "Stat";
    protected static final String JMX_PN_VALUE = "Value";
    protected static final String JMX_PN_CACHEKEY = "Cache Key";
    protected static final String JMX_PN_CACHEENTRY = "Cache Entry";
    protected static final String JMX_PN_CONTENTTYPE = "Content Type";
    protected static final String JMX_PN_STATUS = "Status";
    protected static final String JMX_PN_CHARENCODING = "Character Encoding";
    protected static final String JMX_PN_SIZE = "Size";
    protected static final String JMX_PN_HITS = "Hits";
    protected static final String JMX_PN_TOTALSIZESERVED = "Total Size Served from Cache";




    public <T> AbstractCacheMBean(T implementation, Class<T> mbeanInterface) throws NotCompliantMBeanException
    {
        super(implementation, mbeanInterface);
    }

    protected AbstractCacheMBean(Class<?> mbeanInterface) throws NotCompliantMBeanException
    {
        super(mbeanInterface);
    }

    protected abstract Map<K,V> getCacheAsMap();

    protected abstract long getBytesLength(V cacheObj);

    protected abstract void addCacheData(Map<String, Object> data, V cacheObj);

    protected abstract String toString(V cacheObj) throws CacheMBeanException;

    protected abstract CompositeType getCacheEntryType() throws OpenDataException;

    @Override
    public final long getCacheEntriesCount() {
        return getCacheAsMap().size();
    }

    @Override
    public final TabularData getCacheContents() throws CacheMBeanException {
        try{
            final CompositeType cacheEntryType = getCacheEntryType();

            final TabularDataSupport tabularData = new TabularDataSupport(
                    new TabularType("Cache Entries", "Cache Entries", cacheEntryType, new String[] { JMX_PN_CACHEKEY }));

            Map<K, V> cacheAsMap = getCacheAsMap();
            for (final Map.Entry<K, V> entry : cacheAsMap.entrySet()) {
                final Map<String, Object> data = new HashMap<String, Object>();
                data.put(JMX_PN_CACHEKEY, entry.getKey().toString());

                V cacheObj = entry.getValue();
                if (cacheObj != null) {
                    addCacheData(data, cacheObj);
                }

                tabularData.put(new CompositeDataSupport(cacheEntryType, data));
            }

            return tabularData;
        }catch(OpenDataException ex){
            throw new CacheMBeanException("Error getting the cache contents", ex);
        }
    }

    @Override
    public final String getCacheSize() {
        // Iterate through the cache entries and compute the total size of byte array.
        long size = 0L;
        final Map<K,V> map = getCacheAsMap();

        for (final Map.Entry<K, V> entry : map.entrySet()) {
            size += getBytesLength(entry.getValue());
        }

        // Convert bytes to human-friendly format
        return FileUtils.byteCountToDisplaySize(size);
    }

    public String getCacheEntry(String cacheKeyStr) throws CacheMBeanException {
        K cacheKey = null;

        final Map<K,V> map = getCacheAsMap();

        for (K cacheKeyTmp : map.keySet()) {
            if (StringUtils.equals(cacheKeyStr, cacheKeyTmp.toString())) {
                cacheKey = cacheKeyTmp;
                break;
            }
        }

        if (cacheKey != null) {
            V persistenceObject = map.get(cacheKey);
            if (persistenceObject != null) {
                return toString(persistenceObject);
            }
        }

        return "Invalid cache key parameter.";
    }
}
