/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2018 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.adobe.acs.commons.httpcache.store.caffeine.impl;


import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import static com.adobe.acs.commons.httpcache.store.HttpCacheStore.PN_MAXSIZE;
import static com.adobe.acs.commons.httpcache.store.HttpCacheStore.PN_TTL;

@ObjectClassDefinition(name = "ACS AEM Commons - HTTP Cache - Caffeine In Mem cache store",
        description = "Caffeine Cache data store implementation for in-memory storage. Supports custom TTL per cache config.")
public @interface Config {

    String PROP_TTL = PN_TTL;
    String PROP_MAX_SIZE_IN_MB = PN_MAXSIZE;

    long DEFAULT_TTL = -1L; // Defaults to -1 meaning no TTL.

    long DEFAULT_MAX_SIZE_IN_MB = 10L;

    @AttributeDefinition(name = "TTL",
            description = "TTL for all entries in this cache in seconds. Default to -1 meaning no TTL.",
            defaultValue = "" + DEFAULT_TTL)
    long httpcache_cachestore_caffeinecache_ttl() default DEFAULT_TTL;

    @AttributeDefinition(name = "Maximum size of this store in MB",
            description = "Default to 10MB. If cache size goes beyond this size, least used entry will be evicted "
                    + "from the cache",
            defaultValue = "" + DEFAULT_MAX_SIZE_IN_MB)
    long httpcache_cachestore_caffeinecache_maxsize() default DEFAULT_MAX_SIZE_IN_MB;

}
