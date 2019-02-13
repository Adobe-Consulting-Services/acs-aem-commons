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
package com.adobe.acs.commons.httpcache.rule.impl;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.engine.CacheContent;
import com.adobe.acs.commons.httpcache.rule.AbstractHttpCacheHandlingRule;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;

/**
 * ACS AEM Commons - HTTP Cache - Rule: Cache only response status 200.
 *
 * Cache only Http response status for the request is 200.
 */
@Component
@Service
public class CacheOnlyResponse200 extends AbstractHttpCacheHandlingRule {
    private static final int HTTP_SUCCESS_RESPONSE_STATUS = 200;

    @Override
    public boolean onResponseCache(SlingHttpServletRequest request, SlingHttpServletResponse response,
                                   HttpCacheConfig cacheConfig, CacheContent cacheContent) {
        // Continue only if the response status is 200.
        if (HTTP_SUCCESS_RESPONSE_STATUS != response.getStatus()) {
            return false;
        }

        return true;
    }
}
