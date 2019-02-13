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
package com.adobe.acs.commons.httpcache.config.impl.keys;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.keys.AbstractCacheKey;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.sling.api.SlingHttpServletRequest;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;


public class KeyValueCacheKey extends AbstractCacheKey implements CacheKey, Serializable {
    private final String cacheKeyId;
    private Map<String, String[]> allowedKeyValues;

    public KeyValueCacheKey(final SlingHttpServletRequest request, final HttpCacheConfig cacheConfig,
                            final String cacheKeyId, final Map<String, String[]> allowedKeyValues) {
        super(request, cacheConfig);
        this.cacheKeyId = cacheKeyId;
        this.allowedKeyValues = allowedKeyValues;
    }

    public KeyValueCacheKey(final String uri, final HttpCacheConfig cacheConfig, final String cacheKeyId,
                            final Map<String, String[]> allowedKeyValues) {
        super(uri, cacheConfig);
        this.cacheKeyId = cacheKeyId;
        this.allowedKeyValues = allowedKeyValues;
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) {
            return false;
        }

        KeyValueCacheKey that = (KeyValueCacheKey) o;

        if(that == null){
            return false;
        }

        return new EqualsBuilder()
                .append(getUri(), that.getUri())
                .append(cacheKeyId, that.cacheKeyId)
                .append(getAuthenticationRequirement(), that.getAuthenticationRequirement())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getUri())
                .append(cacheKeyId)
                .append(getAuthenticationRequirement()).toHashCode();
    }

    @Override
    public String toString() {
        StringBuilder formattedString = new StringBuilder(this.uri);
        formattedString.append(cacheKeyId);
        formattedString.append("[AUTH_REQ:" + getAuthenticationRequirement() + "]");

        return formattedString.toString();
    }

    /** For Serialization **/
    private void writeObject(ObjectOutputStream o) throws IOException  {
        parentWriteObject(o);
        o.writeObject(allowedKeyValues);
    }

    /** For De-serialization **/
    private void readObject(ObjectInputStream o) throws IOException, ClassNotFoundException {
        parentReadObject(o);
        allowedKeyValues = (Map<String, String[]>) o.readObject();
    }

}
