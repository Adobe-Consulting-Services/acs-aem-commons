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
import com.adobe.acs.commons.httpcache.exception.HttpCacheKeyCreationException;
import com.adobe.acs.commons.httpcache.keys.AbstractCacheKey;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.sling.api.SlingHttpServletRequest;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Cachekey that differentiates based on:
 * <p>Resource path</p>
 * <p>Authentication requirement</p>
 */
public class ResourcePathCacheKey extends AbstractCacheKey implements CacheKey, Serializable
{
    public ResourcePathCacheKey(SlingHttpServletRequest request, HttpCacheConfig cacheConfig) {
        super(request, cacheConfig);
    }

    public ResourcePathCacheKey(String uri, HttpCacheConfig cacheConfig) {
        super(uri, cacheConfig);
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) {
            return false;
        }

        if (o == null) {
            return false;
        }

        ResourcePathCacheKey that = (ResourcePathCacheKey) o;
        return new EqualsBuilder()
                .append(getUri(), that.getUri())
                .append(getResourcePath(), that.getResourcePath())
                .append(getAuthenticationRequirement(), that.getAuthenticationRequirement())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getUri())
                .append(getAuthenticationRequirement()).toHashCode();
    }

    @Override
    public String toString() {
        return this.resourcePath + " [AUTH_REQ:" + getAuthenticationRequirement() + "]";
    }

    @Override
    public String getUri() {
        return this.resourcePath;
    }

    /** For Serialization **/
    private void writeObject(ObjectOutputStream o) throws IOException {
        parentWriteObject(o);
    }

    /** For De serialization **/
    private void readObject(ObjectInputStream o) throws IOException, ClassNotFoundException {
        parentReadObject(o);
    }
}
