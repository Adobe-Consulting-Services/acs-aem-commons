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
package com.adobe.acs.commons.httpcache.config.impl.keys;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.keys.AbstractCacheKey;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.day.cq.commons.PathInfo;
import com.google.common.base.Objects;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;

/**
 * RequestPathCacheKey. Not used currently.
 * <p>
 * Generated keys contain resource path, selector and extension.
 * </p>
 *
 * @author niek.raaijkmakers@external.cybercon.de
 * @since 2018-05-03
 */
public class RequestPathCacheKey extends AbstractCacheKey implements CacheKey {

    private static final long serialVersionUID = 1;

    private final String selector;
    private final String extension;

    public RequestPathCacheKey(SlingHttpServletRequest request, HttpCacheConfig cacheConfig) {
        super(request, cacheConfig);

        RequestPathInfo pathInfo = request.getRequestPathInfo();
        selector = pathInfo.getSelectorString();
        extension = pathInfo.getExtension();
    }

    public RequestPathCacheKey(String uri, HttpCacheConfig cacheConfig) {
        super(uri, cacheConfig);

        RequestPathInfo pathInfo = new PathInfo(uri);
        selector = pathInfo.getSelectorString();
        extension = pathInfo.getExtension();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RequestPathCacheKey that = (RequestPathCacheKey) o;
        return Objects.equal(getSelector(), that.getSelector())
                && Objects.equal(getExtension(), that.getExtension())
                && Objects.equal(getResourcePath(), that.getResourcePath());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getSelector(), getExtension(), getResourcePath());
    }

    @Override
    public String toString() {
        return resourcePath + "." + selector + "." + extension;
    }


    public String getSelector() {
        return selector;
    }

    public String getExtension() {
        return extension;
    }
}
