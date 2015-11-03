package com.adobe.acs.commons.httpcache.store.mem.impl;

import com.adobe.granite.jmx.annotation.Description;

import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.TabularData;

/**
 * JMX MBean for MEM cache store.
 */
@Description("ACS AEM Commons - Http Cache - Mem Cache")
public interface MemCacheMBean {

    @Description("Clear entire cache")
    void clearCache();

    @Description("Number of entries in the cache")
    long getCacheEntriesCount();

    @Description("Size of cache in KB")
    int getCacheSizeInKB();

    @Description("Cache TTL in Seconds. -1 value represent no TTL.")
    long getTtl();

    @Description("Available cache stats.")
    TabularData getCacheStats();


    @Description("Keys in cache")
    TabularData getCacheKeys();

}
