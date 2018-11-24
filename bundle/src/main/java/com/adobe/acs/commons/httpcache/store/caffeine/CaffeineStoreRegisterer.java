package com.adobe.acs.commons.httpcache.store.caffeine;

import com.adobe.acs.commons.functions.BiFunctionWithException;
import com.adobe.acs.commons.httpcache.store.AbstractHttpCacheStoreRegistrationServiceImpl;
import com.adobe.acs.commons.httpcache.store.caffeine.impl.CaffeineMemHttpCacheStoreImpl;
import org.apache.felix.scr.annotations.Component;


@Component(metatype = false, label = "ACS AEM Commons - HTTP Cache - Caffeine cache store.",
        description = "Cache data store implementation for in-memory storage.", configurationPid = "com.adobe.acs.commons.httpcache.store.caffeine.impl.CaffeineMemHttpCacheStoreImpl")
public class CaffeineStoreRegisterer extends AbstractHttpCacheStoreRegistrationServiceImpl<CaffeineMemHttpCacheStoreImpl> {

    @Override
    protected String getJMXName() {
        return "EHCache In Memory HTTP Cache Store";
    }

    @Override
    protected BiFunctionWithException<Long, Long, CaffeineMemHttpCacheStoreImpl> getStoreCreateFunction() {
        return (ttl, maxSizeInMb) -> new CaffeineMemHttpCacheStoreImpl(ttl, maxSizeInMb);
    }

}
