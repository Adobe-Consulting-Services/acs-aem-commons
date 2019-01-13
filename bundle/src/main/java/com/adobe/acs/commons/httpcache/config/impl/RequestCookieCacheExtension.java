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
import com.adobe.acs.commons.httpcache.config.impl.keys.RequestCookieCacheKey;
import com.adobe.acs.commons.httpcache.config.impl.keys.helper.RequestCookieKeyValueMap;
import com.adobe.acs.commons.httpcache.config.impl.keys.helper.RequestCookieKeyValueMapBuilder;
import com.adobe.acs.commons.httpcache.exception.HttpCacheKeyCreationException;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.keys.CacheKeyFactory;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.Cookie;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;

/**
 * RequestCookieCacheExtension
 * <p>
 * This extension on the HTTP cache allows for specific cookie combinations to create seperated cache entries.
 * This so we can present a different header based on cookie values, which tell us if a user is logged in and what type of user it is.
 * </p>
 *
 */
@Component(configurationPolicy = ConfigurationPolicy.REQUIRE, service = {HttpCacheConfigExtension.class, CacheKeyFactory.class})
@Designate(ocd = RequestCookieCacheExtension.Config.class, factory = true)
public class RequestCookieCacheExtension implements HttpCacheConfigExtension, CacheKeyFactory {

    @ObjectClassDefinition(
            name = "Config - Configuration OCD object for the CookiecCacheExtension",
            description = "Extension for the ACS commons HTTP Cache. Leverages cookies."
    )
    public @interface Config {

        @AttributeDefinition(
                name = "Configuration Name",
                description = "The unique identifier of this extension"
        )
        String configName() default "";

        @AttributeDefinition(
                name = "Allowed Cookies",
                description = "Cookie keys that will used to generate a cache key."
        )
        String[] allowedCookieKeys() default {};

        @AttributeDefinition(
                name = "AllowedValues",
                description = "If set, narrows down specified keys to specified values."
        )
        String[] allowedCookieKeyValues() default {};

        @AttributeDefinition(
                name = "Empty is allowed",
                description = "Cookie keys that will used to generate a cache key."
        )
        boolean emptyAllowed() default false;

    }

    private static final Logger LOG = LoggerFactory.getLogger(RequestCookieCacheExtension.class);
    public static final String SEPARATOR = "=";

    private boolean emptyAllowed;
    private String configName;
    private Set<String> cookieKeys;
    private Map<String, String> cookieKeyValues;

    //-------------------------<HttpCacheConfigExtension methods>

    @Override
    public boolean accepts(SlingHttpServletRequest request, HttpCacheConfig cacheConfig) {

        if (emptyAllowed) {
            return true;
        } else {
            Set<Cookie> presentCookies = ImmutableSet.copyOf(request.getCookies());
            return containsAtLeastOneMatch(presentCookies);
        }
    }

    private boolean containsAtLeastOneMatch(Set<Cookie> presentCookies) {
        RequestCookieKeyValueMapBuilder builder = new RequestCookieKeyValueMapBuilder(cookieKeys,cookieKeyValues, presentCookies);
        RequestCookieKeyValueMap map = builder.build();
        return !map.isEmpty();
    }

    //-------------------------<CacheKeyFactory methods>

    @Override
    public CacheKey build(final SlingHttpServletRequest slingHttpServletRequest, final HttpCacheConfig cacheConfig)
            throws HttpCacheKeyCreationException {

        ImmutableSet<Cookie> presentCookies = ImmutableSet.copyOf(slingHttpServletRequest.getCookies());
        RequestCookieKeyValueMapBuilder builder = new RequestCookieKeyValueMapBuilder(cookieKeys, cookieKeyValues, presentCookies);
        return new RequestCookieCacheKey(slingHttpServletRequest, cacheConfig, builder.build());
    }


    public CacheKey build(String resourcePath, HttpCacheConfig httpCacheConfig) throws HttpCacheKeyCreationException {
        return new RequestCookieCacheKey(resourcePath, httpCacheConfig, new RequestCookieKeyValueMap());
    }

    @Override
    public boolean doesKeyMatchConfig(CacheKey key, HttpCacheConfig cacheConfig) throws HttpCacheKeyCreationException {

        // Check if key is instance of GroupCacheKey.
        if (!(key instanceof RequestCookieCacheKey)) {
            return false;
        }

        RequestCookieCacheKey thatKey = (RequestCookieCacheKey) key;

        return new RequestCookieCacheKey(thatKey.getUri(), cacheConfig, thatKey.getKeyValueMap()).equals(key);
    }

    //-------------------------<OSGi Component methods>

    @Activate
    public void activate(Config config) {
        this.cookieKeys = ImmutableSet.copyOf(config.allowedCookieKeys());
        this.configName = config.configName();
        this.emptyAllowed = config.emptyAllowed();

        HashMap<String,String> map = new HashMap<>(config.allowedCookieKeyValues().length);
        for(String allowedValue: config.allowedCookieKeyValues()){
            if(allowedValue.contains(SEPARATOR)){
                String key = substringBefore(allowedValue, SEPARATOR);
                String value = substringAfter(allowedValue, SEPARATOR);
                if(isNotBlank(key) & isNotBlank(value)){
                    map.put(key,value);
                }
            }
        }
        this.cookieKeyValues = ImmutableMap.copyOf(map);
        LOG.info("GroupHttpCacheConfigExtension activated/modified.");
    }
}
