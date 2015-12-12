package com.adobe.acs.commons.httpcache.store.mem.impl;

import com.adobe.granite.jmx.annotation.Description;
import com.adobe.granite.jmx.annotation.Name;

import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.TabularData;
import java.io.IOException;

/**
 * JMX MBean for MEM cache store.
 */
@Description("ACS AEM Commons - Http Cache - Mem Cache")
public interface MemCacheMBean {

    @Description("Clear entire cache")
    void clearCache();

    @Description("Number of entries in the cache")
    long getCacheEntriesCount();

    @Description("Size of cache")
    String getCacheSize();

    @Description("Cache TTL in Seconds. -1 value represent no TTL.")
    long getTtl();

    @Description("Cache entry contents by key.")
    String getCacheEntry(@Name(value="Cache Key") String cacheKeyStr) throws IOException;

    @Description("Available cache stats.")
    TabularData getCacheStats() throws OpenDataException;

    @Description("Keys in cache")
    TabularData getCacheKeys() throws OpenDataException;
}
