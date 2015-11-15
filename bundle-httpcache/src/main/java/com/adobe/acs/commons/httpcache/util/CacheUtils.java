package com.adobe.acs.commons.httpcache.util;

import com.adobe.acs.commons.httpcache.keys.CacheKey;
import org.apache.commons.lang.NotImplementedException;

import java.io.File;

/**
 * Utilties tied to caching keys / values.
 */
public class CacheUtils {
    private CacheUtils() {
        throw new Error(CacheUtils.class.getName() + " is not meant to be instantiated.");
    }

    /**
     * Create a temporary file for taking copy of servlet response stream.
     *
     * @param cacheKey
     * @return
     */
    public static File createTemporaryCacheFile(CacheKey cacheKey) {
        // TODO - Provide implementation
        // Create a file in Java temp directory with cacheKey.hashCode() as file name.
        throw new NotImplementedException();
    }
}
