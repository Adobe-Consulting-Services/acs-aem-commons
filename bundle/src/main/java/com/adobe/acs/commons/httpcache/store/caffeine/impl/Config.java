package com.adobe.acs.commons.httpcache.store.caffeine.impl;


import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "ACS AEM Commons - HTTP Cache - Caffeine In Mem cache store.",
        description = "Caffeine Cache data store implementation for in-memory storage. Supports custom TTL per cache config.")
public @interface Config {

    long DEFAULT_TTL = -1L; // Defaults to -1 meaning no TTL.

    long DEFAULT_MAX_SIZE_IN_MB = 10L;

    @AttributeDefinition(name = "TTL",
            description = "TTL for all entries in this cache in seconds. Default to -1 meaning no TTL.",
            defaultValue = ""+DEFAULT_TTL)
    long httpcache_cachestore_caffeinecache_ttl();

    @AttributeDefinition(name = "Maximum size of this store in MB",
            description = "Default to 10MB. If cache size goes beyond this size, least used entry will be evicted "
                    + "from the cache",
            defaultValue = ""+ DEFAULT_MAX_SIZE_IN_MB)
    long httpcache_cachestore_caffeinecache_maxsize();

}
