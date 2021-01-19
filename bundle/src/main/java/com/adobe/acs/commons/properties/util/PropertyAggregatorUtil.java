/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2020 Adobe
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
package com.adobe.acs.commons.properties.util;

import com.adobe.acs.commons.properties.PropertyConfigService;
import com.day.cq.wcm.api.Page;
import org.apache.sling.api.resource.ValueMap;

import java.util.Map;
import java.util.Set;

public class PropertyAggregatorUtil {

    private PropertyAggregatorUtil() {
        throw new IllegalStateException("PropertyAggregatorUtil is a utility class, it should not be instantiated.");
    }

    /**
     * Add the properties of a page to the given map.  Excluded properties are found in the
     * {@link PropertyConfigService} service.
     *
     * @param map                   the map that should be updated with the properties and their values
     * @param page                  the page containing properties
     * @param prefix                the prefix to apply to the
     * @param propertyConfigService the {@link PropertyConfigService} used to check type and exclusion
     */
    public static void addPagePropertiesToMap(Map<String, Object> map, Page page, String prefix,
                                              PropertyConfigService propertyConfigService) {
        ValueMap pageProperties = page.getProperties();
        addPropertiesToMap(map, pageProperties.entrySet(), prefix, propertyConfigService);
    }

    /**
     * Add the properties provided to the given map.  Excluded properties are found in the {@link PropertyConfigService}
     * service.
     *
     * @param map                   the map that should be updated with the properties and their values
     * @param entries               the Set of entries in a map to be added
     * @param prefix                the prefix to apply to the
     * @param propertyConfigService the {@link PropertyConfigService} used to check type and exclusion
     */
    public static void addPropertiesToMap(Map<String, Object> map, Set<Map.Entry<String, Object>> entries,
                                          String prefix, PropertyConfigService propertyConfigService) {
        for (Map.Entry<String, Object> entry : entries) {
            if (propertyConfigService.isNotExcluded(entry.getKey())
                    && propertyConfigService.isAllowedType(entry.getValue())) {
                map.put(prefix + "." + entry.getKey(), entry.getValue());
            }
        }
    }
}
