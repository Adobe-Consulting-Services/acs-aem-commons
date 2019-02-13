package com.adobe.acs.commons.httpcache.store.caffeine.impl;

import com.adobe.acs.commons.util.impl.CacheMBean;
import com.adobe.granite.jmx.annotation.Description;

@Description("ACS AEM Commons - Http Cache - Caffeine Cache")
public interface CaffeineCacheMBean extends CacheMBean {

    @Description("Cache TTL in Seconds. -1 value represent no TTL.")
    long getTtl();
}



