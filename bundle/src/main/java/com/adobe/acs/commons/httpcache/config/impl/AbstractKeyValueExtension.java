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
package com.adobe.acs.commons.httpcache.config.impl;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.config.HttpCacheConfigExtension;
import com.adobe.acs.commons.httpcache.config.impl.keys.KeyValueCacheKey;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.keys.CacheKeyFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractKeyValueExtension implements HttpCacheConfigExtension, CacheKeyFactory {

    @Override
    public CacheKey build(SlingHttpServletRequest request, HttpCacheConfig cacheConfig) {
        return new KeyValueCacheKey(request, cacheConfig, getCacheKeyId(), getAllowedKeyValues(), getActualValues(request));
    }

    @Override
    public CacheKey build(String resourcePath, HttpCacheConfig cacheConfig) {
        return new KeyValueCacheKey(resourcePath, cacheConfig, getCacheKeyId(), getAllowedKeyValues());
    }

    @Override
    public boolean doesKeyMatchConfig(CacheKey key, HttpCacheConfig cacheConfig) {
        // Check if key is instance of GroupCacheKey.
        if (!(key instanceof KeyValueCacheKey)) {
            return false;
        }
        // Validate if key request uri can be constructed out of uri patterns in cache config.
        return new KeyValueCacheKey(key.getUri(), cacheConfig, getCacheKeyId(), getAllowedKeyValues()).equals(key);
    }

    @Override
    public boolean accepts(SlingHttpServletRequest request, HttpCacheConfig cacheConfig) {
        return accepts(request, cacheConfig, getAllowedKeyValues());
    }

    public abstract boolean accepts(SlingHttpServletRequest request, HttpCacheConfig cacheConfig, Map<String, String[]> allowedKeyValues);

    public abstract Map<String, String[]> getAllowedKeyValues();

    public abstract String getCacheKeyId();

    protected abstract String getActualValue(String key, SlingHttpServletRequest request);

    protected Map<String, String> getActualValues(SlingHttpServletRequest request) {

        HashMap<String,String> foundValues = new HashMap<>();
        for (final Map.Entry<String, String[]> entry : getAllowedKeyValues().entrySet()) {
            final String key = entry.getKey();
            final String value = getActualValue(key, request);

            if(StringUtils.isNotBlank(value)){
                foundValues.put(key, value);
            }
        }

        return foundValues;
    }

}
