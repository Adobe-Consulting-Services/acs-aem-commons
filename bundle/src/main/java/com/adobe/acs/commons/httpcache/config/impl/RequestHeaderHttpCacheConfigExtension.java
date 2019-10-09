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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * RequestHeaderHttpCacheConfigExtension
 * <p>
 * This extension on the HTTP cache allows for specific HTTP Request Header combinations to create separate cache entries.
 * </p>
 */
@Component(
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        service = {HttpCacheConfigExtension.class, CacheKeyFactory.class},
        property = {Constants.SERVICE_RANKING + ":Integer=20" }
)
@Designate(
        ocd = RequestHeaderHttpCacheConfigExtension.Config.class,
        factory = true
)
public class RequestHeaderHttpCacheConfigExtension extends AbstractKeyValueExtension implements CacheKeyFactory, HttpCacheConfigExtension {

    @ObjectClassDefinition(
            name = "ACS AEM Commons - HTTP Cache - Extension - Request Header",
            description = "Defined Request headers / Request header values that will be allowed for this extension (HttpCacheConfig and CacheKeyFactory)."
    )
    public @interface Config {
        @AttributeDefinition(
                name = "Allowed Request Header",
                description = "The HTTP Request Header to check."
        )
        String httpcache_config_extension_requestheader() default "";

        @AttributeDefinition(
                name = "Allowed Request Header values",
                description = "This request is only accepted for caching when its named request header (above) contains one of these values. Leave blank for any value."
        )
        String[] httpcache_config_extension_requestheader_values() default {};

        @AttributeDefinition(
                name = "Config Name"
        )
        String config_name() default StringUtils.EMPTY;

        @AttributeDefinition
        String webconsole_configurationFactory_nameHint() default "Request Headers: [ {httpcache.config.extension.requestheader} ] Request Header values: [ {httpcache.config.extension.requestheader.values} ] Config name: [ {config.name} ]";
    }

    private Map<String, String[]> allowedHeaders;

    private String cacheKeyId;

    @Override
    public Map<String, String[]> getAllowedKeyValues() {
        return allowedHeaders;
    }

    @Override
    public boolean accepts(SlingHttpServletRequest request, HttpCacheConfig cacheConfig, Map<String, String[]> allowedKeyValues) {
        for (final Map.Entry<String, String[]> entry : allowedKeyValues.entrySet()) {
            final String header = request.getHeader(entry.getKey());

            if (header != null && (ArrayUtils.isEmpty(entry.getValue()) || ArrayUtils.contains(entry.getValue(), header))) {
               return true;
                // No matches found for this row; continue looking through the allowed list
            }
        }

        // No valid request headers could be found.
        return false;
    }
    
    @Override
    protected String getActualValue(String key, SlingHttpServletRequest request) {
        return request.getHeader(key);
    }
    
    
    @Override
    public String getCacheKeyId() {
        return "[Request Header: " + cacheKeyId + "]";
    }

    @Activate
    public void activate(Config config) {
        allowedHeaders = new HashMap<>();
        allowedHeaders.put(config.httpcache_config_extension_requestheader(), config.httpcache_config_extension_requestheader_values());

        cacheKeyId = UUID.randomUUID().toString();
    }
}
