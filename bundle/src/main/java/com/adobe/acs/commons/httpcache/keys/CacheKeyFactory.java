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
package com.adobe.acs.commons.httpcache.keys;

import org.apache.sling.api.SlingHttpServletRequest;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.exception.HttpCacheKeyCreationException;

/**
 * CacheKeyFactory is a OSGi Service interface that allows for consumers to generate their own CacheKey's based on their
 * out use-cases.
 * This project will provide a GroupBased CacheKey factory.
 */
public interface CacheKeyFactory {
    /**
     * Build a cache key.
     *
     * @param request
     * @param cacheConfig
     * @return
     * @throws HttpCacheKeyCreationException
     */
    CacheKey build(SlingHttpServletRequest request, HttpCacheConfig cacheConfig) throws HttpCacheKeyCreationException;

    /**
     * Build a cache key.
     *
     * @param resourcePath
     * @param cacheConfig
     * @return
     * @throws HttpCacheKeyCreationException
     */
    CacheKey build(String resourcePath, HttpCacheConfig cacheConfig) throws HttpCacheKeyCreationException;

    /**
     * Does the Cache Key matches the Htt[ Cache Config.
     * @param key
     * @param cacheConfig
     * @return True if key and config match.
     * @throws HttpCacheKeyCreationException
     */
    boolean doesKeyMatchConfig(CacheKey key, HttpCacheConfig cacheConfig) throws HttpCacheKeyCreationException;
}
