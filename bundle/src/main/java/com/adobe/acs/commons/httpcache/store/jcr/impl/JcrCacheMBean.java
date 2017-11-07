package com.adobe.acs.commons.httpcache.store.jcr.impl;

import com.adobe.acs.commons.util.impl.GenericCacheMBean;
import com.adobe.granite.jmx.annotation.Description;

@Description("ACS AEM Commons - Http Cache - JCR Cache")
public interface JcrCacheMBean extends GenericCacheMBean
{
    @Description("Cache TTL in Seconds. -1 value represent no TTL.")
    long getTtl();
}
