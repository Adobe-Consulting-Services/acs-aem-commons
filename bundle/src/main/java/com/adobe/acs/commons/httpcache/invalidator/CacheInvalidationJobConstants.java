package com.adobe.acs.commons.httpcache.invalidator;

/**
 * Constants for creating http cache invalidation jobs. Custom invalidation events could leverage these constants while
 * creating and starting an invalidation job.
 */
public class CacheInvalidationJobConstants {

    /** Topic for the http cache invalidation job */
    public static final String TOPIC_HTTP_CACHE_INVALIDATION_JOB = "com/adobe/acs/commons/httpcache/invalidator/job";

    /**
     * Path for which the data is changed. If the cache config invalidation path pattern matches with this path
     * and if there is a cache hit, that cache entry will be invalidated.
     */
    public static final String PAYLOAD_KEY_DATA_CHANGE_PATH = "path";

    private CacheInvalidationJobConstants() {
        throw new Error("This is not meant to be instantiated.");
    }
}
