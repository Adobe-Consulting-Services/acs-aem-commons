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
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = PropertyAggregatorService.class, immediate = true)
@Designate(ocd = PropertyAggregatorServiceImpl.Config.class)
public class PropertyAggregatorServiceImpl implements PropertyAggregatorService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    // Set the max recursion depth in case of endless loop reading properties
    private final int MAX_PROPERTY_DEPTH = 10;
    private final String PAGE_PROP_PREFIX = "page_properties";
    private final String INHERITED_PAGE_PROP_PREFIX = "inherited_page_properties";

    private boolean enabled;
    private boolean recursionEnabled;
    private List<Pattern> exclusionList;
    private Map<String, String> additionalData;

    @Override
    public Map<String, Object> getProperties(final Resource resource) {
        Map<String, Object> map = new HashMap<>();

        if (enabled && resource != null) {
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

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * Recursively traverse the properties of a bean, as well as the potential beans of those properties until there are
     * either no properties other that 'class' or the max depth has been reached.  Store all property values in a map
     * with the key being the nested property names.
     * @param targetPropertyMap the map that should be updated with the properties and their values
     * @param key the current nested name for the property (each call will append the current property name to the parent's separated by a period)
     * @param beanValue the current property's value
     * @param depth keeps track of the recursion depth to not exceed MAX_PROPERTY_DEPTH
     */
    private void addBeanToMap(final Map<String,Object> targetPropertyMap, final String key, final Object beanValue, final int depth) {
        if (targetPropertyMap != null && StringUtils.isNotBlank(key) && beanValue != null && depth < MAX_PROPERTY_DEPTH) {
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
                            if (recursionEnabled) {
                                addBeanToMap(targetPropertyMap, newKey, propertyValue, depth + 1);
                            }
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
                        addBeanToMap(targetPropertyMap, prefix, model, 1);
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
        this.enabled = config.enabled();
        this.recursionEnabled = config.enable_recursion();
        this.additionalData = getMapFromArray(config.additional_data());
    }

    @ObjectClassDefinition(
            name = "Property Aggregator Service Configuration"
    )
    @interface Config {

        /**
         * The flag for enabling the service and rewriter.
         *
         * @return The enabled flag
         */
        @AttributeDefinition(
                name = "Enabled",
                description = "Check to enable the service and rewriter.",
                type = AttributeType.BOOLEAN
        )
        boolean enabled() default false;

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

        /**
         * The flag for enabling the recursive grabbing of properties in custom classes.
         *
         * @return The enabled flag
         */
        @AttributeDefinition(
                name = "Enable Recursion",
                description = "Check to enable the recursive gathering of properties in custom models." +
                        " Note: Custom dialog field JavaScript does not support recursive property " +
                        "aggregation.",
                type = AttributeType.BOOLEAN
        )
        boolean enable_recursion() default false;
    }
}
