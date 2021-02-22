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
package com.adobe.acs.commons.properties.impl;

import com.adobe.acs.commons.properties.ContentVariableProvider;
import com.adobe.acs.commons.properties.PropertyConfigService;
import com.adobe.acs.commons.properties.util.PropertyAggregatorUtil;
import com.day.cq.wcm.api.Page;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component(service = ContentVariableProvider.class)
public class AllPagePropertiesContentVariableProvider implements ContentVariableProvider {

    public static final String PAGE_PROP_PREFIX = "page_properties";
    private static final String INHERITED_PAGE_PROP_PREFIX = "inherited_page_properties";

    @Reference
    private PropertyConfigService propertyConfigService;

    @Override
    public void addProperties(Map<String, Object> map, Page page) {
        // Add current page properties
        PropertyAggregatorUtil.addPagePropertiesToMap(map, page, PAGE_PROP_PREFIX, propertyConfigService);

        // Add inherited page properties
        Page parent = page.getParent();
        while (parent != null) {
            Map<String, Object> inheritedMap = new HashMap<>();
            PropertyAggregatorUtil.addPagePropertiesToMap(inheritedMap, parent, INHERITED_PAGE_PROP_PREFIX, propertyConfigService);
            Set<Map.Entry<String, Object>> entries = inheritedMap.entrySet();
            for (Map.Entry<String, Object> entry : entries) {
                if (shouldAddInherited(map, entry.getKey())) {
                    map.put(entry.getKey(), entry.getValue());
                }
            }
            parent = parent.getParent();
        }
    }

    /**
     * Check to see if the current inherited property key is already contained in the set of properties.
     *
     * @param map          current map of properties
     * @param propertyName current property name
     * @return whether the map contains a local page property or an inherited property
     */
    private boolean shouldAddInherited(Map<String, Object> map, String propertyName) {
        return !map.containsKey(propertyName.replace(INHERITED_PAGE_PROP_PREFIX, PAGE_PROP_PREFIX))
                && !map.containsKey(propertyName);
    }

    @Override
    public boolean accepts(Page page) {
        return page != null && page.getPath().startsWith("/content");
    }
}
