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
package com.adobe.acs.commons.httpcache.config.impl;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.config.HttpCacheConfigExtension;
import com.adobe.acs.commons.httpcache.keys.CacheKeyFactory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


/**
 * ResourcePropertiesHttpCacheConfigExtension
 * <p>
 * This extension on the HTTP cache allows for specific Resource Property combinations to create separate cache entries.
 * </p>
 */
@Component(
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        service = {HttpCacheConfigExtension.class, CacheKeyFactory.class},
        property = {Constants.SERVICE_RANKING + ":Integer=50" }
)
@Designate(
        ocd = ResourcePropertiesHttpCacheConfigExtension.Config.class,
        factory = true
)
public class ResourcePropertiesHttpCacheConfigExtension extends AbstractKeyValueExtension implements CacheKeyFactory, HttpCacheConfigExtension {

    @ObjectClassDefinition(
            name = "ACS AEM Commons - HTTP Cache - Extension - Resource Properties",
            description = "Defines Resource Property names / Resource Property values that will be allowed for this extension (HttpCacheConfig and CacheKeyFactory)."
    )
    public @interface Config {
        @AttributeDefinition(
                name = "Allowed Property names",
                description = "The Resource Property name to check."
        )
        String httpcache_config_extension_property() default "";

        @AttributeDefinition(
                name = "Allowed Property values",
                description = "This request is only accepted for caching when its named resource property (above) contains one of these values. Leave blank for any value."
        )
        String[] httpcache_config_extension_property_values() default {};

        @AttributeDefinition(
                name = "Config Name"
        )
        String config_name() default StringUtils.EMPTY;

        @AttributeDefinition
        String webconsole_configurationFactory_nameHint() default "Properties: [ {httpcache.config.extension.property} ] Property values: [ {httpcache.config.extension.property.values} ] Config name: [ {config.name} ]";
    }

    private static final Logger log = LoggerFactory.getLogger(ResourcePropertiesHttpCacheConfigExtension.class);

    private String configName;

    private Map<String, String[]> allowedProperties;

    private String cacheKeyId;

    @Override
    public Map<String, String[]> getAllowedKeyValues() {
        return allowedProperties;
    }

    @Override
    public boolean accepts(SlingHttpServletRequest request, HttpCacheConfig cacheConfig, Map<String, String[]> allowedKeyValues) {
        for (final Map.Entry<String, String[]> entry : allowedKeyValues.entrySet()) {
            final ValueMap properties = request.getResource().getValueMap();

            final String key = entry.getKey();
            if (properties.containsKey(key)) {
                log.debug("{} - passed contains key with key {} for resource {}", configName, key, request.getResource().getPath());
                final String[] propertyValues = properties.get(key, String[].class);

                if (ArrayUtils.isEmpty(propertyValues) || CollectionUtils.containsAny(Arrays.asList(entry.getValue()), Arrays.asList(propertyValues))) {
                    log.debug("{} - passed value check with value {} for resource {}", configName, entry.getValue(), request.getResource().getPath());
                    // If no values were specified, then assume ANY and ALL values are acceptable, and were are merely looking for the existence of the property
                    return true;
                }
                // No matches found for this row; continue looking through the allowed list
            }
        }

        // No valid resource property could be found.
        return false;
    }
    
    @Override
    protected String getActualValue(String key, SlingHttpServletRequest request) {
        return request.getResource().getValueMap().get(key, String.class);
    }
    
    @Override
    public String getCacheKeyId() {
        return "[Resource Property: " + cacheKeyId + "]";
    }

    @Activate
    public void activate(ResourcePropertiesHttpCacheConfigExtension.Config config) {
        allowedProperties = new HashMap<>();
        allowedProperties.put(config.httpcache_config_extension_property(), config.httpcache_config_extension_property_values());
        cacheKeyId = UUID.randomUUID().toString();
        configName = config.config_name();
    }
}
