package com.adobe.acs.commons.httpcache.store.impl;

import com.adobe.acs.commons.httpcache.store.HttpCacheStore;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;

/**
 * In-memory cache store implementation.
 */
// TODO - Make the below immediate false.
@Component(label = "ACS AEM Commons - HTTP Cache - In-Memory cache store.",
           description = "Cache data store implementation for in-memory storage.",
           metatype = true,
           immediate = true
)
@Service
@Property(name = HttpCacheStore.KEY_CACHE_STORE_TYPE,
          value = HttpCacheStore.VALUE_MEM_CACHE_STORE_TYPE,
          propertyPrivate = true
)
public class MemHttpCacheStoreImpl implements HttpCacheStore {
}
