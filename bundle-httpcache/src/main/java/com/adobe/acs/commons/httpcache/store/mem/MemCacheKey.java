package com.adobe.acs.commons.httpcache.store.mem;

import com.adobe.acs.commons.httpcache.keys.CacheKey;

public interface MemCacheKey extends CacheKey {
    MemCacheKey asMemCacheKey();
}
