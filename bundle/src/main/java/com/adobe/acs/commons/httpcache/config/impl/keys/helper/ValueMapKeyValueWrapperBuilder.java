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
package com.adobe.acs.commons.httpcache.config.impl.keys.helper;

import com.adobe.acs.commons.httpcache.config.impl.ValueMapValueHttpCacheConfigExtension;
import com.adobe.acs.commons.util.impl.ReflectionUtil;
import org.apache.sling.api.resource.ValueMap;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static com.adobe.acs.commons.httpcache.config.impl.keys.helper.KeyValueMapWrapper.SEPERATOR;
import static org.apache.commons.lang.StringUtils.EMPTY;

/**
 * Builds a KeyValueMapWrapperBuilder wrapper based on value map values
 */
public class ValueMapKeyValueWrapperBuilder implements KeyValueMapWrapperBuilder {

    private final Set<String> allowedKeys;
    private final Map<String, String> allowedValues;
    private final ValueMap valueMap;
    private final KeyValueMapWrapper keyValueMapWrapper = new KeyValueMapWrapper(ValueMapValueHttpCacheConfigExtension.KEY_STRING_REPRENSENTATION);

    public ValueMapKeyValueWrapperBuilder(Set<String> allowedKeys, Map<String, String> allowedValues, ValueMap actualResourceValueMap) {

        this.allowedKeys = allowedKeys;
        this.allowedValues = allowedValues;
        this.valueMap = actualResourceValueMap;
    }

    @Override
    public KeyValueMapWrapper build() {

        for(Iterator<Map.Entry<String,Object>> iterator = valueMap.entrySet().iterator(); iterator.hasNext();){
            Map.Entry<String,Object> entry = iterator.next();
            String key = entry.getKey();
            Object value = entry.getValue();

            if (allowedValues.containsKey(key) && valueMap.containsKey(key)) {
                String[] specificAllowedValues = allowedValues.get(key).split(SEPERATOR);
                for (String allowedValue : specificAllowedValues) {
                    Object castedValue = ReflectionUtil.castStringValue(allowedValue);
                    if (castedValue.equals(value)) {
                        keyValueMapWrapper.put(key, value);
                    }
                }
            } else if(allowedKeys.contains(key)){
                keyValueMapWrapper.put(key, EMPTY);
            }

        }
        return keyValueMapWrapper;
    }

}

