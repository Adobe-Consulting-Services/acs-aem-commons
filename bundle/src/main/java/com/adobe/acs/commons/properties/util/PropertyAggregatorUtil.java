package com.adobe.acs.commons.properties.util;

import com.adobe.acs.commons.properties.PropertyConfigService;
import com.day.cq.wcm.api.Page;
import org.apache.sling.api.resource.ValueMap;

import java.util.Map;
import java.util.Set;

public class PropertyAggregatorUtil {

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
