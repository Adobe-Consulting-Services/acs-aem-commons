package com.adobe.acs.commons.httpcache.util;

import com.adobe.acs.commons.httpcache.keys.CacheKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Utilties tied to caching keys / values.
 */
public class CacheUtils {
    private static final Logger log = LoggerFactory.getLogger(CacheUtils.class);

    private CacheUtils() {
        throw new Error(CacheUtils.class.getName() + " is not meant to be instantiated.");
    }

    /**
     * Create a temporary file for taking copy of servlet response stream.
     *
     * @param cacheKey
     * @return
     */
    public static File createTemporaryCacheFile(CacheKey cacheKey) throws IOException {
        // Create a file in Java temp directory with cacheKey.toSting() as file name.

        File file = File.createTempFile(cacheKey.toString(), ".tmp");
        if (null != file) {
            log.debug("Temp file created with the name - {}", cacheKey.toString());
        }
        return file;
    }
}
