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
package com.adobe.acs.commons.httpcache.config.impl;


import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.config.HttpCacheConfigExtension;
import com.adobe.acs.commons.httpcache.config.impl.keys.KeyValueHttpCacheKey;
import com.adobe.acs.commons.httpcache.config.impl.keys.helper.KeyValueConfigHelper;
import com.adobe.acs.commons.httpcache.config.impl.keys.helper.KeyValueMapWrapper;
import com.adobe.acs.commons.httpcache.config.impl.keys.helper.KeyValueMapWrapperBuilder;
import com.adobe.acs.commons.httpcache.config.impl.keys.helper.ValueMapKeyValueWrapperBuilder;
import com.adobe.acs.commons.httpcache.exception.HttpCacheKeyCreationException;
import com.adobe.acs.commons.httpcache.exception.HttpCacheRepositoryAccessException;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.keys.CacheKeyFactory;
import com.google.common.collect.ImmutableSet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Activate;

import java.util.Map;
import java.util.Set;

public abstract class AbstractKeyValueExtension implements HttpCacheConfigExtension, CacheKeyFactory {

    private boolean emptyAllowed;
    private Set<String> valueMapKeys;
    private Map<String, String> allowedValues;
    private String configName;

    @Override
    public boolean accepts(SlingHttpServletRequest request, HttpCacheConfig cacheConfig) throws HttpCacheRepositoryAccessException {
        if(emptyAllowed){
            return true;
        }

        KeyValueMapWrapper keyValueMapWrapper = getBuilder(request, valueMapKeys, allowedValues).build();
        return !keyValueMapWrapper.isEmpty();
    }

    @Override
    public CacheKey build(SlingHttpServletRequest request, HttpCacheConfig cacheConfig) throws HttpCacheKeyCreationException {

        KeyValueMapWrapper keyValueMapWrapper = getBuilder(request, valueMapKeys, allowedValues).build();
        return new KeyValueHttpCacheKey(request, cacheConfig, keyValueMapWrapper);
    }

    @Override
    public CacheKey build(String resourcePath, HttpCacheConfig cacheConfig) throws HttpCacheKeyCreationException {
        return new KeyValueHttpCacheKey(resourcePath, cacheConfig, new KeyValueMapWrapper(getKeyToStringRepresentation()));
    }


    protected abstract String getKeyToStringRepresentation();

    @Override
    public boolean doesKeyMatchConfig(CacheKey key, HttpCacheConfig cacheConfig) throws HttpCacheKeyCreationException {
        // Check if key is instance of GroupCacheKey.
        if (!(key instanceof KeyValueHttpCacheKey)) {
            return false;
        }

        KeyValueHttpCacheKey thatKey = (KeyValueHttpCacheKey) key;

        return new KeyValueHttpCacheKey(thatKey.getUri(), cacheConfig, thatKey.getKeyValueMap()).equals(key);

    }

    protected abstract KeyValueMapWrapperBuilder getBuilder(SlingHttpServletRequest request, Set<String> allowedKeys, Map<String,String> allowedValues);

    @Activate
    protected void activate(KeyValueConfig config){
        this.emptyAllowed = config.emptyAllowed();
        this.valueMapKeys = ImmutableSet.copyOf(config.allowedKeys());
        this.configName = config.configName();
        this.allowedValues = KeyValueConfigHelper.convertAllowedValues(config);
    }

}
