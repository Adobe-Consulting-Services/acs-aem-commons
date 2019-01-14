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
import com.adobe.acs.commons.httpcache.config.impl.keys.helper.KeyValueMapWrapper;
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
 * KeyValueHttpCacheKey
 *
 * <p>CacheKey that differentiates based on key / values..</p>
 *
 * <p>Example with cookie key / values: Useful when cached content needs to be differentiated for logged in user groups.</p>
 * <p>Use your middleware / front-end to set a cookie with a user group, and create a configuration of this class with the key used.</p>
 *
 */
public class KeyValueHttpCacheKey extends AbstractCacheKey implements CacheKey, Serializable {


    private KeyValueMapWrapper keyValueMap;

    public KeyValueHttpCacheKey(SlingHttpServletRequest request, HttpCacheConfig cacheConfig, KeyValueMapWrapper keyValueMap) {

        super(request, cacheConfig);
        this.keyValueMap = keyValueMap;
    }

    public KeyValueHttpCacheKey(String uri, HttpCacheConfig cacheConfig, KeyValueMapWrapper keyValueMap) {
        super(uri, cacheConfig);

        this.keyValueMap = keyValueMap;
    }

    public KeyValueMapWrapper getKeyValueMap() {
        return keyValueMap;
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) {
            return false;
        }

        KeyValueHttpCacheKey that = (KeyValueHttpCacheKey) o;

        if(that == null){
            return false;
        }

        return new EqualsBuilder()
                .append(getUri(), that.getUri())
                .append(keyValueMap, that.keyValueMap)
                .append(getAuthenticationRequirement(), that.getAuthenticationRequirement())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getUri())
                .append(keyValueMap)
                .append(getAuthenticationRequirement()).toHashCode();
    }

    @Override
    public String toString() {
        StringBuilder formattedString = new StringBuilder(this.uri);
        formattedString.append(keyValueMap);
        formattedString.append("[AUTH_REQ:" + getAuthenticationRequirement() + "]");
        return formattedString.toString();
    }

    /** For Serialization **/
    private void writeObject(ObjectOutputStream o) throws IOException
    {
        parentWriteObject(o);
        o.writeObject(keyValueMap);
    }

    /** For De serialization **/
    private void readObject(ObjectInputStream o)
            throws IOException, ClassNotFoundException {

        parentReadObject(o);
        keyValueMap = (KeyValueMapWrapper) o.readObject();
    }
}
