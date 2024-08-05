/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.httpcache.rule.impl;

import com.adobe.acs.commons.httpcache.rule.AbstractHttpCacheHandlingRule;
import com.adobe.acs.commons.httpcache.rule.HttpCacheHandlingRule;
import org.apache.sling.api.SlingHttpServletRequest;
import org.osgi.service.component.annotations.Component;

/**
 * ACS AEM Commons - HTTP Cache - Rule: Cache only request http method is GET.
 * <p>
 * Process only Http GET requests.
 */
@Component(service = {HttpCacheHandlingRule.class})
public class CacheOnlyGetRequest extends AbstractHttpCacheHandlingRule {
    private static final String HTTP_GET_METHOD = "GET";

    @Override
    public boolean onRequestReceive(SlingHttpServletRequest request) {
        // Return true only if Http method is GET.
        return HTTP_GET_METHOD.equals(request.getMethod());
    }
}
