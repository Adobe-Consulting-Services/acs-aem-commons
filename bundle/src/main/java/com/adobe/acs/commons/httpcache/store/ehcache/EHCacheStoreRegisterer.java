package com.adobe.acs.commons.httpcache.store.ehcache;

import com.adobe.acs.commons.functions.BiFunctionWithException;
import com.adobe.acs.commons.httpcache.store.AbstractHttpCacheStoreRegistrationServiceImpl;
import com.adobe.acs.commons.httpcache.store.ehcache.impl.EHCacheMemHttpCacheStoreImpl;
import org.apache.felix.scr.annotations.Component;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.ehcache.core.spi.service.StatisticsService;
import org.osgi.service.component.annotations.Reference;


@Component(metatype = false,label = "ACS AEM Commons - HTTP Cache - In-Memory cache store.",
        description = "Cache data store implementation for in-memory storage.", configurationPid = "com.adobe.acs.commons.httpcache.store.ehcache.impl.EHCacheMemHttpCacheStoreImpl")
public class EHCacheStoreRegisterer extends AbstractHttpCacheStoreRegistrationServiceImpl<EHCacheMemHttpCacheStoreImpl> {

    @Reference
    private DynamicClassLoaderManager dclm;
    @Reference
    private StatisticsService statisticsService;


    @Override
    protected String getJMXName() {
        return "EHCache HttpCache Store";
    }

    @Override
    protected BiFunctionWithException<Long, Long, EHCacheMemHttpCacheStoreImpl> getStoreCreateFunction() {
        return (ttl, maxSizeInMb) -> new EHCacheMemHttpCacheStoreImpl(dclm, statisticsService, ttl, maxSizeInMb);
    }
}
