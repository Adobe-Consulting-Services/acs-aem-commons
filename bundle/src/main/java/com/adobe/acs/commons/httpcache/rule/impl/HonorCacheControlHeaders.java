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
 */package com.adobe.acs.commons.httpcache.rule.impl;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.engine.CacheContent;
import com.adobe.acs.commons.httpcache.rule.AbstractHttpCacheHandlingRule;
import org.apache.commons.collections.CollectionUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;

import java.util.Arrays;
import java.util.List;

/**
 * ACS AEM Commons - HTTP Cache - Rule: Honor cache control headers
 *
 * Cache only Http response status for the request is 200.
 * Do not cache the response when it's set with cache control headers marking it as not cacheable.
 */
@Component
@Service
public class HonorCacheControlHeaders extends AbstractHttpCacheHandlingRule {
    private static final String KEY_CACHE_CONTROL_HEADER = "Cache-Control"; // HTTP 1.1
    private static final String[] VALUES_CACHE_CONTROL = {"no-cache", "no-store", "must-revalidate"};
    private static final String KEY_PRAGMA = "Pragma"; // HTTP 1.0
    private static final String[] VALUES_PRAGMA = {"no-cache"};

    @Override
    public boolean onResponseCache(SlingHttpServletRequest request, SlingHttpServletResponse response, HttpCacheConfig cacheConfig, CacheContent cacheContent) {
        // Check cache control header
        if (cacheContent.getHeaders().containsKey(KEY_CACHE_CONTROL_HEADER)) {
            List<String> cacheControlValues = cacheContent.getHeaders().get(KEY_CACHE_CONTROL_HEADER);
            if (CollectionUtils.containsAny(cacheControlValues, Arrays.asList(VALUES_CACHE_CONTROL))) {
                return false;
            }
        }

        // Check Pragma.
        if (cacheContent.getHeaders().containsKey(KEY_PRAGMA)) {
            List<String> pragmaValues = cacheContent.getHeaders().get(KEY_PRAGMA);
            if (CollectionUtils.containsAny(pragmaValues, Arrays.asList(VALUES_PRAGMA))) {
                return false;
            }
        }

        return true;
    }
}
