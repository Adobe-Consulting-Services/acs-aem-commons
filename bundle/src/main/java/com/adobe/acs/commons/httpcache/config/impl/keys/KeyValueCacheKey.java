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
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.sling.api.SlingHttpServletRequest;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;


public class KeyValueCacheKey extends AbstractCacheKey implements CacheKey, Serializable {
    private String cacheKeyId;
    private ImmutableMap<String, String> actualKeyValues;
    private ImmutableMap<String, String[]> allowedKeyValues;

    public KeyValueCacheKey(final SlingHttpServletRequest request, final HttpCacheConfig cacheConfig,
                            final String cacheKeyId, final Map<String, String[]> allowedKeyValues, Map<String, String> actualKeyValues) {
        super(request, cacheConfig);
        this.cacheKeyId = cacheKeyId;
        this.allowedKeyValues = ImmutableMap.copyOf(allowedKeyValues);
        this.actualKeyValues = ImmutableMap.copyOf(actualKeyValues);
    }

    public KeyValueCacheKey(final String uri, final HttpCacheConfig cacheConfig, final String cacheKeyId,
                            final Map<String, String[]> allowedKeyValues) {
        super(uri, cacheConfig);
        this.cacheKeyId = cacheKeyId;
        this.allowedKeyValues = ImmutableMap.copyOf(allowedKeyValues);
        this.actualKeyValues = ImmutableMap.copyOf(Collections.emptyMap());
    }
    
    Map<String, String> getActualKeyValues() {
        return actualKeyValues;
    }
    
    Map<String, String[]> getAllowedKeyValues() {
        return allowedKeyValues;
    }

    @Override
    public boolean equals(Object o) {
        if(o == this){
            return true;
        }
       
        if (!super.equals(o)) {
            return false;
        }

        KeyValueCacheKey that = (KeyValueCacheKey) o;

        if (that == null) {
            return false;
        }

        return new EqualsBuilder()
                .append(getUri(), that.getUri())
                .append(cacheKeyId, that.cacheKeyId)
                .append(getAuthenticationRequirement(), that.getAuthenticationRequirement())
                .appendSuper(getEqualForAllowedKeyValues(that))
                .isEquals();
    }

    private boolean getEqualForAllowedKeyValues(KeyValueCacheKey other) {
        if (this.allowedKeyValues.size() != other.allowedKeyValues.size()) {
            return false;
        }

        for (Map.Entry<String,String[]> entry: allowedKeyValues.entrySet()) {

            final String key = entry.getKey();
            final String[] val = entry.getValue();

                if(     !other.allowedKeyValues.containsKey(key)
                    ||  !Arrays.equals(val, other.allowedKeyValues.get(key))) {
                return false;
            }
        }
    
        if (this.actualKeyValues.size() != other.actualKeyValues.size()) {
            return false;
        }
    
        for (Map.Entry<String,String> entry: actualKeyValues.entrySet()) {
            final String key = entry.getKey();
            final String val = entry.getValue();
        
            if(     !other.getActualKeyValues().containsKey(key)
                    ||  !val.equals(other.getActualKeyValues().get(key))) {
                return false;
            }
        }

        return true;
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
        if(actualKeyValues != null){
            return String.format("%s%s%s", this.resourcePath, cacheKeyId, getKeyValueToStringRepresentation());
        }else{
            return String.format("%s%s", this.resourcePath, cacheKeyId);
        }
       
    }
    
    private String getKeyValueToStringRepresentation() {
    
        StringBuilder sb = new StringBuilder();
    
        for (Map.Entry<String,String[]> entry: allowedKeyValues.entrySet()) {
            String key = entry.getKey();
            sb.append(key);
            if(actualKeyValues != null){
                String value = actualKeyValues.get(key);
                if(StringUtils.isNotBlank(value)){
                    sb.append("=" + value);
                }
            }
            sb.append(";");
           
        }
        return sb.toString();
        
    }
    
    /** For Serialization **/
    private void writeObject(ObjectOutputStream o) throws IOException  {
        parentWriteObject(o);
        o.writeUTF(cacheKeyId);
        o.writeObject(new HashMap<>(allowedKeyValues));
        o.writeObject(new HashMap<>(actualKeyValues));
    }

    /** For De-serialization **/
    private void readObject(ObjectInputStream o) throws IOException, ClassNotFoundException {
        parentReadObject(o);
        cacheKeyId = o.readUTF();
        allowedKeyValues = ImmutableMap.copyOf((Map<String, String[]>) o.readObject());
        actualKeyValues = ImmutableMap.copyOf(((Map<String,String>) o.readObject()));
    }

}
