/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.day.cq.commons.PathInfo;
import org.apache.sling.api.SlingHttpServletRequest;

public abstract class AbstractCacheKey {

    protected String authenticationRequirement;
    protected String uri;
    protected String resourcePath;

    public AbstractCacheKey(SlingHttpServletRequest request, HttpCacheConfig cacheConfig) {
        this.authenticationRequirement = cacheConfig.getAuthenticationRequirement();
        this.uri = request.getRequestURI();
        this.resourcePath = request.getResource().getPath();
    }


    public AbstractCacheKey(String uri, HttpCacheConfig cacheConfig) {
        this.authenticationRequirement = cacheConfig.getAuthenticationRequirement();
        this.uri = uri;
        this.resourcePath = new PathInfo(uri).getResourcePath();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        return true;
    }

    public String getAuthenticationRequirement() {
        return authenticationRequirement;
    }

    public String getUri() {
        return uri;
    }
}
