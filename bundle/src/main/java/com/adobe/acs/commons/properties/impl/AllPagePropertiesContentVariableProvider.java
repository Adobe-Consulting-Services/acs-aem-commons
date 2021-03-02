/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2021 Adobe
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
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.adobe.acs.commons.properties.util.PropertyAggregatorUtil.addPropertiesToMap;

@Component(service = ContentVariableProvider.class)
public class AllPagePropertiesContentVariableProvider implements ContentVariableProvider {

    private static final Logger log = LoggerFactory.getLogger(AllPagePropertiesContentVariableProvider.class);

    public static final String PAGE_PROP_PREFIX = "page_properties";
    private static final String INHERITED_PAGE_PROP_PREFIX = "inherited_page_properties";

    @Reference
    private PropertyConfigService propertyConfigService;

    @Override
    public void addProperties(Map<String, Object> map, SlingHttpServletRequest request) {
        Resource resource = request.getResource();

        PageManager pageManager = resource.getResourceResolver().adaptTo(PageManager.class);
        if (pageManager == null) {
            log.warn("PageManager was null, skipping properties.");
            return;
        }
        Page page = pageManager.getContainingPage(resource);

        if (page == null) {
            log.warn("No containing page found for resource at {}", resource.getPath());
            return;
        }

        // Add current page properties
        addPagePropertiesToMap(map, page, PAGE_PROP_PREFIX, propertyConfigService);

        // Add inherited page properties
        Page parent = page.getParent();
        while (parent != null) {
            Map<String, Object> inheritedMap = new HashMap<>();
            addPagePropertiesToMap(inheritedMap, parent, INHERITED_PAGE_PROP_PREFIX, propertyConfigService);
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

    /**
     * Add the properties of a page to the given map.  Excluded properties are found in the
     * {@link PropertyConfigService} service.
     *
     * @param map                   the map that should be updated with the properties and their values
     * @param page                  the page containing properties
     * @param prefix                the prefix to apply to the
     * @param propertyConfigService the {@link PropertyConfigService} used to check type and exclusion
     */
    private void addPagePropertiesToMap(Map<String, Object> map, Page page, String prefix,
                                              PropertyConfigService propertyConfigService) {
        ValueMap pageProperties = page.getProperties();
        addPropertiesToMap(map, pageProperties.entrySet(), prefix, propertyConfigService);
    }

    @Override
    public boolean accepts(SlingHttpServletRequest request) {
        PageManager pageManager = request.getResourceResolver().adaptTo(PageManager.class);
        if (pageManager == null) {
            log.warn("PageManager is null, not accepting this request.");
            return false;
        }
        Page page = pageManager.getContainingPage(request.getResource());
        return page != null && page.getPath().startsWith("/content");
    }
}
