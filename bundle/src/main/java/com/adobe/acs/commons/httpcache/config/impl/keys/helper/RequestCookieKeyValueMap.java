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

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Iterator;

/**
 * RequestCookieKeyValueMap
 * <p>
 * Basically a HashMap with a nice toString function for the CookieCacheKey to hold cookies into.
 * </p>
 */
public class RequestCookieKeyValueMap extends HashMap<String,String> {

    @Override
    public String toString() {

        StringBuilder result = new StringBuilder("[CookieKeyValues:");

        Iterator<Entry<String,String>> entries = entrySet().iterator();

        if(!isEmpty()){
            while (entries.hasNext()) {

                Entry<String, String> entry = entries.next();
                String key = entry.getKey();
               String value = entry.getValue();

                if (StringUtils.isNotEmpty(value)) {
                    result.append(key + "=" + value);
                } else {
                    //cookie is only present, but no value.
                    result.append(key);
                }

                if (entries.hasNext()) {
                    result.append(',');
                }

            }
        }

        result.append(']');
        return result.toString();
    }
}
