package com.adobe.acs.commons.util.impl;

import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.TabularData;

import com.adobe.acs.commons.util.impl.exception.CacheMBeanException;
import com.adobe.granite.jmx.annotation.Description;
import com.adobe.granite.jmx.annotation.Name;

public interface CacheMBean
{
    @Description("Clear entire cache")
    void clearCache();

    @Description("Number of entries in the cache")
    long getCacheEntriesCount();

    @Description("Size of cache")
    String getCacheSize();

    @Description("Available cache stats.")
    TabularData getCacheStats() throws OpenDataException;

    @Description("Cache entry contents by key.")
    String getCacheEntry(@Name(value="Cache Key") String cacheKeyStr) throws CacheMBeanException;

    @Description("Contents of cache")
    TabularData getCacheContents() throws Exception;
}
