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
 * Utility abstract implementation providing default behavior for any cache handling rules. <p> All methods in this
 * return true indicating that this does nothing other than instructing the {@link
 * com.adobe.acs.commons.httpcache.engine.HttpCacheEngine} to move on with next rule. Any custom implementation could
 * leverage this to facilitate overriding only the methods the custom rule is intended for. </p>
 */
public class AbstractHttpCacheHandlingRule implements HttpCacheHandlingRule {


    @Override
    public boolean onRequestReceive(SlingHttpServletRequest request) {
        return true;
    }

    @Override
    public boolean onResponseCache(SlingHttpServletRequest request, SlingHttpServletResponse response,
                                   HttpCacheConfig cacheConfig, CacheContent cacheContent) {
        return true;
    }

    @Override
    public boolean onCacheDeliver(SlingHttpServletRequest request, SlingHttpServletResponse response, HttpCacheConfig
            cacheConfig, CacheContent cacheContent) {
        return true;
    }

    @Override
    public boolean onCacheInvalidate(String path) {
        return true;
    }
}