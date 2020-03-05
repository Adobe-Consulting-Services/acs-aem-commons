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
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * RequestParameterHttpCacheConfigExtension
 * <p>
 * This extension on the HTTP cache allows for specific HTTP Request parameter combinations to create separate cache entries.
 * </p>
 */
@Component(
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        service = {HttpCacheConfigExtension.class, CacheKeyFactory.class},
        property = {Constants.SERVICE_RANKING + ":Integer=10" }
)
@Designate(
        ocd = RequestParameterHttpCacheConfigExtension.Config.class,
        factory = true
)
public class RequestParameterHttpCacheConfigExtension extends AbstractKeyValueExtension implements CacheKeyFactory, HttpCacheConfigExtension {

    @ObjectClassDefinition(
            name = "ACS AEM Commons - HTTP Cache - Extension - Request Parameter",
            description = "Defined Request Parameters / Request Parameter values that will be allowed for this extension (HttpCacheConfig and CacheKeyFactory)."
    )
    public @interface Config {
        @AttributeDefinition(
                name = "Allow request headers",
                description = "The HTTP Request Parameter to check."
        )
        String httpcache_config_extension_requestparameter() default "";

        @AttributeDefinition(
                name = "Allowed request header values",
                description = "This request is only accepted for caching when its named request parameter (above) contains one of these values. Leave blank for any value."
        )
        String[] httpcache_config_extension_requestparameter_values() default {};

        @AttributeDefinition(
                name = "Config Name"
        )
        String config_name() default StringUtils.EMPTY;

        @AttributeDefinition
        String webconsole_configurationFactory_nameHint() default "Request Parameters: [ {httpcache.config.extension.requestparameter} ] Request Parameter values: [ {httpcache.config.extension.requestparameter.values} ] Config name: [ {config.name} ]";
    }

    private Map<String, String[]> allowedParameters;

    private String cacheKeyId;

    @Override
    public Map<String, String[]> getAllowedKeyValues() {
        return allowedParameters;
    }

    @Override
    public boolean accepts(SlingHttpServletRequest request, HttpCacheConfig cacheConfig, Map<String, String[]> allowedKeyValues) {
        for (final Map.Entry<String, String[]> entry : allowedKeyValues.entrySet()) {

            if (request.getParameterMap().keySet().contains(entry.getKey())) {
                final String[] parameterValues = request.getParameterMap().get(entry.getKey());

                if (ArrayUtils.isEmpty(entry.getValue()) || CollectionUtils.containsAny(Arrays.asList(entry.getValue()), Arrays.asList(parameterValues))) {
                    // If no values were specified, then assume ANY and ALL values are acceptable, and were are merely looking for the existence of the request parameter
                    return true;
                }
                // No matches found for this row; continue looking through the allowed list
            }
        }

        // No valid request parameter could be found.
        return false;
    }
    
    @Override
    protected String getActualValue(String key, SlingHttpServletRequest request) {
        return request.getParameter(key);
    }
    
    @Override
    public String getCacheKeyId() {
        return "[Request Parameter: " + cacheKeyId + "]";
    }

    @Activate
    public void activate(RequestParameterHttpCacheConfigExtension.Config config) {
        allowedParameters = new HashMap<>();
        allowedParameters.put(config.httpcache_config_extension_requestparameter(), config.httpcache_config_extension_requestparameter_values());

        cacheKeyId = UUID.randomUUID().toString();
    }
}
