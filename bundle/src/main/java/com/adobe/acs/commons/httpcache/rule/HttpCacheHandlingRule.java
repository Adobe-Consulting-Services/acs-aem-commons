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
package com.adobe.acs.commons.httpcache.rule;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.engine.CacheContent;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;

/**
 * Rules which impacts the behavior of http cache. Concrete implementation of this interface provides developers hooks
 * to supply custom behavior for key events in http cache. <p> Each method represents a hook to the http cache event.In
 * case if a method is not applicable for a custom rule, make it return true indicating the cache engine to continue
 * processing the next rule. </p>
 */
public interface HttpCacheHandlingRule {

    /**
     * Hook to supply custom behavior on {@link com.adobe.acs.commons.httpcache.engine.HttpCacheEngine} receiving the
     * request.
     *
     * @param request
     * @return True represents success and cache handling rules will be continued. False represents failure with cache
     * handling rules being stopped and fallback action will be taken.
     */
    boolean onRequestReceive(SlingHttpServletRequest request);

    /**
     * Hook to supply custom behavior on {@link com.adobe.acs.commons.httpcache.engine.HttpCacheEngine} about to cache a
     * response.
     *
     * @param request
     * @param response
     * @param cacheConfig
     * @param cacheContent Object carring data to be cached.
     * @return True represents success and cache handling rules will be continued. False represents failure with cache
     * handling rules being stopped and fallback action will be taken.
     */
    boolean onResponseCache(SlingHttpServletRequest request, SlingHttpServletResponse response, HttpCacheConfig
            cacheConfig, CacheContent cacheContent);

    /**
     * Hook to supply custom behavior on {@link com.adobe.acs.commons.httpcache.engine.HttpCacheEngine} delivering cache
     * content after being read from cache store.
     *
     * @param request
     * @param response
     * @param cacheConfig
     * @param cacheContent Object carrying data to be delivered.
     * @return True represents success and cache handling rules will be continued. False represents failure with cache
     * handling rules being stopped and fallback action will be taken.
     */
    boolean onCacheDeliver(SlingHttpServletRequest request, SlingHttpServletResponse response, HttpCacheConfig
            cacheConfig, CacheContent cacheContent);

    /**
     * Hook to supply custom behavior on {@link com.adobe.acs.commons.httpcache.engine.HttpCacheEngine} invalidating
     * cache for the changes in the given JCR repository path.
     *
     * @param path JCR repository path
     * @return True represents success and cache handling rules will be continued. False represents failure with cache
     * handling rules being stopped and fallback action will be taken.
     */
    boolean onCacheInvalidate(String path);
}

