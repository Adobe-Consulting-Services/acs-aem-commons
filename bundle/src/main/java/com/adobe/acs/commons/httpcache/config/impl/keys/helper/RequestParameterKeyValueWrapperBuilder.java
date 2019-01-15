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

import com.adobe.acs.commons.httpcache.config.impl.RequestParameterHttpCacheConfigExtension;
import com.adobe.acs.commons.util.impl.ReflectionUtil;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Builds a KeyValueMapWrapperBuilder wrapper based on request parameters
 */
public class RequestParameterKeyValueWrapperBuilder implements KeyValueMapWrapperBuilder {

    private final Set<String> allowedKeys;
    private final Map<String, String> allowedValues;
    private final Map<String, String[]> parameterMap;
    private final KeyValueMapWrapper keyValueMapWrapper = new KeyValueMapWrapper(RequestParameterHttpCacheConfigExtension.KEY_TOSTRING_REPRESENTATION);

    public RequestParameterKeyValueWrapperBuilder(Set<String> allowedKeys, Map<String, String> allowedValues, Map<String, String[]> parameterMap){

        this.allowedKeys = allowedKeys;
        this.allowedValues = allowedValues;
        this.parameterMap = parameterMap;
    }


    @Override
    public KeyValueMapWrapper build() {
        for(Iterator<Map.Entry<String,String[]>> iterator = parameterMap.entrySet().iterator(); iterator.hasNext();){
            Map.Entry<String,String[]> entry = iterator.next();
            String key = entry.getKey();
            String[] value = entry.getValue();

            if (allowedValues.containsKey(key)) {
                putKeyAndValue(key, value);
            } else if(allowedKeys.contains(key)){
                putKeyOnly(key, value);
            }
        }

        return keyValueMapWrapper;
    }

    private void putKeyOnly(String key, String[] value) {
        keyValueMapWrapper.put(key, value);
    }

    private void putKeyAndValue(String key, String[] value) {
        String[] specificAllowedValues  = allowedValues.get(key).split("\\|");

        for (String allowedValue : specificAllowedValues) {
            Object castedValue = ReflectionUtil.castStringValue(allowedValue);
            for(int i = 0;i<value.length;i++){
                if (castedValue.equals(value[i])) {
                    keyValueMapWrapper.put(key+ "[" + i+ "]", value[i]);
                }
            }

        }
    }
}
