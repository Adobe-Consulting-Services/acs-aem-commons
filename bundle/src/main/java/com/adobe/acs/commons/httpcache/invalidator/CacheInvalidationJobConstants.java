/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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
package com.adobe.acs.commons.httpcache.invalidator;

/**
 * Constants for creating http cache invalidation jobs. Custom invalidation events could leverage these constants while
 * creating and starting an invalidation job.
 */
public final class CacheInvalidationJobConstants {

    /** Topic for the http cache invalidation job */
    public static final String TOPIC_HTTP_CACHE_INVALIDATION_JOB = "com/adobe/acs/commons/httpcache/invalidator/job";

    /**
     * Path for which the data is changed. If the cache config invalidation path pattern matches with this path
     * and if there is a cache hit, that cache entry will be invalidated.
     */
    public static final String PAYLOAD_KEY_DATA_CHANGE_PATH = "path";

    private CacheInvalidationJobConstants() {
    }
}
