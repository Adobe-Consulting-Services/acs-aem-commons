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

import com.adobe.acs.commons.httpcache.config.impl.KeyValueConfig;
import com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;

/**
 * Helper class for the KeyValueMapWrapperBuilders.
 */
public class KeyValueConfigHelper {

    public static final String SEPARATOR = "=";

    private KeyValueConfigHelper(){
        //static class
    }

    /**
     * Converts the OCD allowedValues to a Map using a string delimiter, for easier usage
     * @param config
     * @return
     */
    public static Map<String,String> convertAllowedValues(KeyValueConfig config){
        HashMap<String,String> map = new HashMap<>(config.allowedValues().length);
        for(String allowedValue: config.allowedValues()){
            if(allowedValue.contains(SEPARATOR)){
                String key = substringBefore(allowedValue, SEPARATOR);
                String value = substringAfter(allowedValue, SEPARATOR);
                if(isNotBlank(key) & isNotBlank(value)){
                    map.put(key,value);
                }
            }
        }
        return ImmutableMap.copyOf(map);
    }
}
