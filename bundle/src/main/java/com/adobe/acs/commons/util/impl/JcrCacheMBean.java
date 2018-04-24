package com.adobe.acs.commons.util.impl;

import com.adobe.granite.jmx.annotation.Description;

@Description("ACS AEM Commons - Http Cache - JCR Cache")
public interface JcrCacheMBean extends CacheMBean
{
    @Description("Cache TTL in Seconds. -1 value represent no TTL.")
    long getTtl();

    @Description("Force scheduled purge run")
    void purgeExpiredEntries();

    @Description("Reset to cache statistics to 0")
    void resetCacheStats();
}
