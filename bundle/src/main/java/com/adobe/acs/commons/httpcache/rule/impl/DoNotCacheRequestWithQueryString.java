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

import com.adobe.acs.commons.httpcache.rule.AbstractHttpCacheHandlingRule;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;

/**
 * ACS AEM Commons - HTTP Cache - Rule: Do not cache query string requests.
 *
 * Do not cache requests which has got query strings.
 */
@Component
@Service
public class DoNotCacheRequestWithQueryString extends AbstractHttpCacheHandlingRule {

    @Override
    public boolean onRequestReceive(SlingHttpServletRequest request) {
        // Return true only if the query string is absent.
        return StringUtils.isEmpty(request.getQueryString());
    }
}
