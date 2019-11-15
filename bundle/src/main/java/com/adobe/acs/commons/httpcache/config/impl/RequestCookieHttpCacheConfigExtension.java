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
import com.adobe.acs.commons.util.CookieUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.Cookie;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * RequestCookieHttpCacheConfigExtension
 * <p>
 * This extension on the HTTP cache allows for specific HTTP Request Cookie combinations to create separate cache entries.
 * <br>
 * This so we can present a different header based on cookie values, which tell us if a user is logged in and what type of user it is.
 * </p>
 */
@Component(
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        service = {HttpCacheConfigExtension.class, CacheKeyFactory.class},
        property = {Constants.SERVICE_RANKING + ":Integer=30" }
)
@Designate(
        ocd = RequestCookieHttpCacheConfigExtension.Config.class,
        factory = true
)
public class RequestCookieHttpCacheConfigExtension extends AbstractKeyValueExtension implements HttpCacheConfigExtension, CacheKeyFactory {
    private static final Logger log = LoggerFactory.getLogger(RequestCookieHttpCacheConfigExtension.class);

    @ObjectClassDefinition(
            name = "ACS AEM Commons - HTTP Cache - Extension - Cookies",
            description = "Defines Cookie names / values that will be allowed for this extension (HttpCacheConfig and CacheKeyFactory)."
    )
    public @interface Config {
        @AttributeDefinition(
                name = "Allowed Cookie",
                description = "The HTTP Request Cookie name to check."
        )
        String httpcache_config_extension_cookie() default "";

        @AttributeDefinition(
                name = "Allowed Cookie Values",
                description = "This request is only accepted for caching when its named cookie (above) contains one of these values. Leave blank for any value."
        )
        String[] httpcache_config_extension_cookie_values() default {};

        @AttributeDefinition(name = "Config Name")
        String config_name() default StringUtils.EMPTY;

        @AttributeDefinition
        String webconsole_configurationFactory_nameHint() default "Cookie name: [ {httpcache.config.extension.cookie} ] Cookie values: [  {httpcache.config.extension.cookie.values} ] Config name: [ {config.name} ]";
    }

    private Map<String, String[]> allowedCookies;

    private String cacheKeyId;

    @Override
    public Map<String, String[]> getAllowedKeyValues() {
        return allowedCookies;
    }

    @Override
    public boolean accepts(SlingHttpServletRequest request, HttpCacheConfig cacheConfig, Map<String, String[]> allowedKeyValues) {
        for (final Map.Entry<String, String[]> entry : allowedKeyValues.entrySet()) {
            final Cookie cookie = CookieUtil.getCookie(request, entry.getKey());

            if (cookie != null) {
                if (ArrayUtils.isEmpty(entry.getValue())) {
                    // If no values were specified, then assume ANY and ALL values are acceptable, and were are merely looking for the existence of the cookie
                    log.debug("Accepting as cacheable due to existence of Cookie [ {} ]", entry.getKey());
                    return true;
                } else if (ArrayUtils.contains(entry.getValue(), cookie.getValue())) {
                    // The cookies value matched one of the allowed values
                    log.debug("Accepting as cacheable due to existence of Cookie [ {} ] with value [ {} ]", entry.getKey(), cookie.getValue());
                    return true;
                }
                // No matches found for this row; continue looking through the allowed list
            }
        }

        // No valid cookies could be found.
        log.debug("Could not find any valid Cookie matches for HTTP Cache");
        return false;
    }
    
    @Override
    protected String getActualValue(String key, SlingHttpServletRequest request) {
        Cookie cookie =  CookieUtil.getCookie(request, key);
        
        if(cookie != null){
            return cookie.getValue();
        }
        return null;
    }
    
    @Override
    public String getCacheKeyId() {
        return "[Cookie: " + cacheKeyId + "]";
    }

    @Activate
    @Modified
    protected void activate(Config config) {
        allowedCookies = new HashMap<>();
        allowedCookies.put(config.httpcache_config_extension_cookie(), config.httpcache_config_extension_cookie_values());

        cacheKeyId = UUID.randomUUID().toString();
    }
}
