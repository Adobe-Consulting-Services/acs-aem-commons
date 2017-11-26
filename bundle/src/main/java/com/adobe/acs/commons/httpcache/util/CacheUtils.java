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

    private CacheUtils() {}

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
