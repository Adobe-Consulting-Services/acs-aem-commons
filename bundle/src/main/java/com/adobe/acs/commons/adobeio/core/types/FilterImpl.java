/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2018 Adobe
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
package com.adobe.acs.commons.adobeio.core.types;

import java.util.Map;

import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.StringUtils;

import com.drew.lang.annotations.NotNull;

public class FilterImpl implements Filter {

    private final Map<String, String> filter = new HashedMap();

    /**
     * Create new Filter
     * @param key Key
     * @param value Value
     */
    public FilterImpl(@NotNull String key, @NotNull String value) {
        filter.put(key, value);
    }

    @Override
    public String getFilter() {

        String result = StringUtils.EMPTY;

        for(Map.Entry<String, String> entry: filter.entrySet()) {
            result = entry.getKey() + "=" + entry.getValue();
        }

        return result;
    }
}
