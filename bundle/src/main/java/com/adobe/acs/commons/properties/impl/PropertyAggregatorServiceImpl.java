/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2019 Adobe
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

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.adobe.acs.commons.properties.PropertyAggregatorService;
import com.day.cq.wcm.api.NameConstants;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = PropertyAggregatorService.class,
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = PropertyAggregatorServiceImpl.Config.class)
public class PropertyAggregatorServiceImpl implements PropertyAggregatorService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final String PAGE_PROP_PREFIX = "page_properties";
    private final String INHERITED_PAGE_PROP_PREFIX = "inherited_page_properties";

    private List<Pattern> exclusionList;
    private Map<String, String> additionalData;

    @Override
    public Map<String, Object> getProperties(final Resource resource) {
        Map<String, Object> map = new HashMap<>();

        if (resource != null) {
            PageManager pageManager = resource.getResourceResolver().adaptTo(PageManager.class);
            if (pageManager != null) {
                Page currentPage = pageManager.getContainingPage(resource);

                addPagePropertiesToMap(map, currentPage, PAGE_PROP_PREFIX);
                addInheritedPagePropertiesToMap(map, currentPage);
                addAdditionalDataToMap(map, currentPage);
            }
        }

        return map;
    }

    @Override
    public Map<String, Object> getProperties(final Page page) {
        return getProperties(page.getContentResource());
    }

    /**
     * Iterate over the properties of a bean and add the property values (retrieved by invoking the
     * method) to a map with the 'prefix.property' as the key.
     * @param targetPropertyMap the map that should be updated with the properties and their values
     * @param key the current nested name for the property (each call will append the current property name to the parent's separated by a period)
     * @param beanValue the current property's value
     */
    private void addBeanToMap(final Map<String,Object> targetPropertyMap, final String key, final Object beanValue) {
        if (targetPropertyMap != null && StringUtils.isNotBlank(key) && beanValue != null) {
            try {
                Class<?> beanClass = Class.forName(beanValue.getClass().getName());
                BeanInfo beanInfo = Introspector.getBeanInfo(beanClass);
                PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();

                for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                    if (!"class".equalsIgnoreCase(propertyDescriptor.getName())) {
                        String newKey = key + "." + propertyDescriptor.getName();
                        Object propertyValue = propertyDescriptor.getReadMethod().invoke(beanValue);
                        if (propertyValue != null) {
                            targetPropertyMap.put(newKey, propertyValue);
                        }
                    }
                }

            } catch (ClassNotFoundException e) {
                log.error("Error mapping to class " + beanValue.getClass().getName(), e);
            } catch (IntrospectionException e) {
                log.error("IntrospectionException", e);
            } catch (IllegalAccessException e) {
                log.error("IllegalAccessException", e);
            } catch (InvocationTargetException e) {
                log.error("InvocationTargetException", e);
            }
        }
    }

    /**
     * Add the properties of a page to the given map.  Excluded properties are found in the configuration for this service.
     * @param targetPropertyMap the map that should be updated with the properties and their values
     * @param page the page containing properties
     */
    private void addPagePropertiesToMap(final Map<String,Object> targetPropertyMap, final Page page, final String prefix) {
        ValueMap pageProperties = page.getProperties();
        Set<String> propertyNames = pageProperties.keySet();
        for (String propertyName : propertyNames) {
            if (notInMap(targetPropertyMap, propertyName)
                    && isNotExcluded(propertyName)
                    && isAllowedType(pageProperties.get(propertyName))) {
                targetPropertyMap.put(prefix(prefix, propertyName), pageProperties.get(propertyName));
            }
        }
    }

    /**
     * Recursively traverse the properties of a page and it's parents until that parent is not a page or is null, which
     * means it's the root, and add them to the given map.  The traversing will stop when the parent is a node that is
     * not a page, such as a folder.  This is necessary to support things such as editable templates under `/conf`.
     *
     * @param targetPropertyMap the map that should be updated with the properties and their values
     * @param page the page containing properties
     */
    private void addInheritedPagePropertiesToMap(final Map<String,Object> targetPropertyMap, final Page page) {

        addPagePropertiesToMap(targetPropertyMap, page, INHERITED_PAGE_PROP_PREFIX);

        Resource pageResource = page.adaptTo(Resource.class);
        if (pageResource != null) {
            Resource parentResource = pageResource.getParent();

            if (parentResource != null && NameConstants.NT_PAGE.equalsIgnoreCase(parentResource.getResourceType())) {
                addInheritedPagePropertiesToMap(targetPropertyMap, parentResource.adaptTo(Page.class));
            }
        }
    }

    /**
     * Add additional data from custom Java beans that are defined in the configuration for this service to the given map.
     * @param targetPropertyMap the map that should be updated with the properties and their values
     * @param page the page for the context of the bean properties
     */
    private void addAdditionalDataToMap(final Map<String,Object> targetPropertyMap, final Page page) {
        if (!additionalData.isEmpty()) {
            for (String prefix : additionalData.keySet()) {
                try {
                    Resource contentResource = page.getContentResource();
                    Class<?> modelClass = Class.forName(additionalData.get(prefix));
                    Object model = contentResource.adaptTo(modelClass);
                    if (model != null) {
                        addBeanToMap(targetPropertyMap, prefix, model);
                    }
                } catch (ClassNotFoundException e) {
                    log.error("Error mapping to class " + additionalData.get(prefix), e);
                } catch (Exception e) {
                    log.error("Unknown error", e);
                }
            }
        }
    }

    private boolean notInMap(final Map<String, Object> map, final String propertyName) {
        return !map.containsKey(prefix(PAGE_PROP_PREFIX, propertyName))
                && !map.containsKey(prefix(INHERITED_PAGE_PROP_PREFIX, propertyName));
    }

    private boolean isNotExcluded(final String propertyName) {
        for (Pattern pattern : exclusionList) {
            if (pattern.matcher(propertyName).matches()) {
                return false;
            }
        }
        return true;
    }

    private String prefix(final String prefix, final String propertyName) {
        return prefix + "." + propertyName;
    }

    private boolean isAllowedType(Object object) {
        return String.class.equals(object.getClass()) || Long.class.equals(object.getClass());
    }

    /**
     * Creates a map of configuration values based on the array and delimiter passed with the option
     * to swap the key and value from the split string.
     *
     * @param configs The String array containing the delimited config values
     * @return The map of config values
     */
    private Map<String, String> getMapFromArray(final String[] configs) {
        Map<String, String> splitConfigs = new HashMap<>();
        for (String config : configs) {
            if (config.contains("|")) {
                String[] parts = config.split(Pattern.quote("|"));
                if (parts.length == 2) {
                    splitConfigs.put(parts[0], parts[1]);
                }
            }
        }
        return splitConfigs;
    }

    @Activate
    protected void activate(Config config) {
        List<Pattern> excludeList = new ArrayList<>();
        for (String exclude : config.exclude_list()) {
            excludeList.add(Pattern.compile(exclude));
        }
        this.exclusionList = excludeList;
        this.additionalData = getMapFromArray(config.additional_data());
    }

    @ObjectClassDefinition(
            name = "Property Aggregator Service Configuration"
    )
    @interface Config {

        /**
         * The list of patterns or strings to exclude from the property aggregation.
         *
         * @return The list of exclusions
         */
        @AttributeDefinition(
                name = "Exclude List",
                description = "List of properties to exclude, accepts regex.",
                type = AttributeType.STRING
        )
        String[] exclude_list() default {"cq:(.*)"};

        /**
         * The list of additional data sources to the map.
         *
         * @return The list of additional data
         */
        @AttributeDefinition(
                name = "Additional Data",
                description = "List of additional data sources. Each class provided should map to a"
                        + " Sling Model that adapts from the content resource of a page. "
                        + "Format is `prefix|className`",
                type = AttributeType.STRING
        )
        String[] additional_data() default {""};

    }
}
