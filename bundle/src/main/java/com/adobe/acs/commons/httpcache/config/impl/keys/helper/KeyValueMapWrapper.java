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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * KeyValueMapWrapper
 * <p>
 * Basically a HashMap with a nice toString function for the CacheKey to hold key / values into.
 * </p>
 */
public class KeyValueMapWrapper extends HashMap<String,Object> {

    public static final String NULL = "NULL";
    public static final String SEPERATOR = "\\|";
    private final String toStringKey;


    public KeyValueMapWrapper(String toStringKey) {
        super();

        this.toStringKey = toStringKey;
    }

    @Override
    public String toString() {

        StringBuilder result = new StringBuilder("[" + toStringKey +":");

        Iterator<Map.Entry<String,Object>> entries = entrySet().iterator();

        if(!isEmpty()){
            while (entries.hasNext()) {

                Entry<String, Object> entry = entries.next();
                String key = entry.getKey();

                Object valueObject = entry.getValue();

                String value = (valueObject != null) ? valueObject.toString() : NULL;

                if (isNotEmpty(value)) {
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
