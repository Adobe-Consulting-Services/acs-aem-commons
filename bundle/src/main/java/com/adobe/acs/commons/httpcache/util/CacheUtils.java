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

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utilties tied to caching keys / values.
 */
public class CacheUtils {
    private static final Logger log = LoggerFactory.getLogger(CacheUtils.class);

    static final String COOKIEPREFIX_HOST = "__Host-";
    static final String COOKIEPREFIX_SECURE = "__Secure-";
    static final String HEADERKEY_COOKIE = "Set-Cookie";

    private CacheUtils() {
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
            log.debug("Temp file created with the name - {}", cacheKey);
        }
        return file;
    }

    public static Map<String, List<String>> extractHeaders(Collection<Pattern> excludedHeaderRegexList, Collection<String> excludedCookieKeyList, SlingHttpServletResponse response, HttpCacheConfig cacheConfig) {

        List<Pattern> excludedHeaders = Stream.concat(excludedHeaderRegexList.stream(), cacheConfig.getExcludedResponseHeaderPatterns().stream())
                .collect(Collectors.toList());

        List<String> excludedCookieKeys = Stream.concat(excludedCookieKeyList.stream(), cacheConfig.getExcludedCookieKeys().stream())
                .collect(Collectors.toList());

        return response.getHeaderNames().stream()
                .filter(headerName -> excludedHeaders.stream()
                        .noneMatch(pattern -> pattern.matcher(headerName).matches())
                ).collect(
                        Collectors.toMap(headerName -> headerName, headerName -> filterCookieHeaders(response, excludedCookieKeys, headerName)
                        ));
    }

    public static List<String> filterCookieHeaders(SlingHttpServletResponse response, List<String> excludedCookieKeys, String headerName) {
        if (!headerName.equals(HEADERKEY_COOKIE)) {
            return new ArrayList<>(response.getHeaders(headerName));
        }
        //for set-cookie we apply another exclusion filter.
        return new ArrayList<>(response.getHeaders(headerName)).stream().filter(
                header -> {
                    String key;
                    if (header.startsWith(COOKIEPREFIX_HOST)) {
                        key = StringUtils.removeStart(header, COOKIEPREFIX_HOST);
                    } else if (header.startsWith(COOKIEPREFIX_SECURE)) {
                        key = StringUtils.removeStart(header, COOKIEPREFIX_SECURE);
                    } else {
                        key = header;
                    }
                    key = StringUtils.substringBefore(key, "=");

                    return !excludedCookieKeys.contains(key);
                }
        ).collect(Collectors.toList());
    }
}
