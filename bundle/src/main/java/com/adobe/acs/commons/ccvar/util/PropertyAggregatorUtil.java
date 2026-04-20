/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.ccvar.util;

import com.adobe.acs.commons.ccvar.PropertyConfigService;

import java.util.Map;
import java.util.Set;

/**
 * Util class with helpers defined to add properties to the {@link Map} being constructed.
 */
public class PropertyAggregatorUtil {

    private PropertyAggregatorUtil() {
    }

    /**
     * Add the properties provided to the given map.  Excluded properties are found in the {@link PropertyConfigService}
     * service.
     *
     * @param map                   the map that should be updated with the properties and their values
     * @param entries               the Set of entries in a map to be added
     * @param prefix                the prefix to apply to the
     * @param shouldOverride        if true then properties added can be overridden/replaced with other values
     * @param propertyConfigService the {@link PropertyConfigService} used to check type and exclusion
     */
    public static void addPropertiesToMap(Map<String, Object> map, Set<Map.Entry<String, Object>> entries,
                                          String prefix, boolean shouldOverride, PropertyConfigService propertyConfigService) {
        for (Map.Entry<String, Object> entry : entries) {
            if (propertyConfigService.isAllowed(entry.getKey())
                    && propertyConfigService.isAllowedType(entry.getValue())) {
                String propertyName = prefix + "." + entry.getKey();
                if (shouldOverride || !map.containsKey(propertyName)) {
                    map.put(propertyName, entry.getValue());
                }
            }
        }
    }
}
