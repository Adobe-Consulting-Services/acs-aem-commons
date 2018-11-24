package com.adobe.acs.commons.httpcache.store.caffeine;

import com.adobe.acs.commons.functions.SupplierWithException;
import com.adobe.acs.commons.httpcache.store.AbstractHttpCacheStoreRegistrationServiceImpl;
import com.adobe.acs.commons.httpcache.store.caffeine.impl.CaffeineMemHttpCacheStoreImpl;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;

import static com.adobe.acs.commons.httpcache.store.HttpCacheStore.PN_MAXSIZE;
import static com.adobe.acs.commons.httpcache.store.HttpCacheStore.PN_TTL;


@Component(metatype = true, label = "ACS AEM Commons - HTTP Cache - Caffeine cache store.",
        description = "Cache data store implementation for in-memory storage.", configurationPid = "com.adobe.acs.commons.httpcache.store.caffeine.impl.CaffeineMemHttpCacheStoreImpl")
public class CaffeineStoreRegisterer extends AbstractHttpCacheStoreRegistrationServiceImpl<CaffeineMemHttpCacheStoreImpl> {

    private static final long DEFAULT_TTL = -1L; // Defaults to -1 meaning no TTL.
    @Property(label = "TTL",
            description = "TTL for all entries in this cache in seconds. Default to -1 meaning no TTL.",
            longValue = DEFAULT_TTL)
    private static final String PROP_TTL = PN_TTL;
    private long ttl;

    private static final long DEFAULT_MAX_SIZE_IN_MB = 10L; // Defaults to 10MB.
    @Property(label = "Maximum size of this store in MB",
            description = "Default to 10MB. If cache size goes beyond this size, least used entry will be evicted "
                    + "from the cache",
            longValue = DEFAULT_MAX_SIZE_IN_MB)
    private static final String PROP_MAX_SIZE_IN_MB = PN_MAXSIZE;
    private long maxSizeInMb;

    @Override
    protected String getJMXName() {
        return "EHCache In Memory HTTP Cache Store";
    }

    @Override
    protected SupplierWithException<CaffeineMemHttpCacheStoreImpl> getStoreCreateSupplier() {
        return () -> new CaffeineMemHttpCacheStoreImpl(ttl, maxSizeInMb);
    }

}
