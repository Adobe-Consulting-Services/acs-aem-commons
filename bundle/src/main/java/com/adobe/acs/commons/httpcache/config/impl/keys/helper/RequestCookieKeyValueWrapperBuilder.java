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
package com.adobe.acs.commons.httpcache.config.impl.keys.helper;

import com.adobe.acs.commons.httpcache.config.impl.RequestCookieHttpCacheConfigExtension;
import com.adobe.acs.commons.util.impl.ReflectionUtil;

import javax.servlet.http.Cookie;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static com.adobe.acs.commons.httpcache.config.impl.keys.helper.KeyValueMapWrapper.SEPERATOR;
import static org.apache.commons.lang.StringUtils.EMPTY;

/**
 /**
 * Builds a KeyValueMapWrapperBuilder wrapper based on request cookies
 */
public class RequestCookieKeyValueWrapperBuilder implements KeyValueMapWrapperBuilder {

    private final Set<String> allowedKeys;
    private final Map<String, String> cookieKeyValues;
    private final Set<Cookie> presentCookies;
    private final KeyValueMapWrapper keyValueMapWrapper = new KeyValueMapWrapper(RequestCookieHttpCacheConfigExtension.KEY_STRING_REPRENSENTATION);

    public RequestCookieKeyValueWrapperBuilder(Set<String> allowedKeys, Map<String, String> cookieKeyValues, Set<Cookie> presentCookies) {

        this.allowedKeys = allowedKeys;
        this.cookieKeyValues = cookieKeyValues;
        this.presentCookies = presentCookies;
    }


    @Override
    public KeyValueMapWrapper build() {

        for(Iterator<Cookie> iterator = presentCookies.iterator(); iterator.hasNext();){
            Cookie cookie = iterator.next();
            String key = cookie.getName();
            String value = cookie.getValue();

            if (allowedKeys.contains(key) && cookieKeyValues.containsKey(key)) {
                putKeyAndValue(key, value);
            } else if(allowedKeys.contains(key)){
                putKeyOnly(key);
            }
        }

        return keyValueMapWrapper;
    }

    private void putKeyOnly(String key) {
        keyValueMapWrapper.put(key, EMPTY);
    }

    private void putKeyAndValue(String key, String value) {
        String[] specificAllowedValues  = cookieKeyValues.get(key).split(SEPERATOR);
        for (String allowedValue : specificAllowedValues) {
            Object castedValue = ReflectionUtil.castStringValue(allowedValue);
            if (castedValue.equals(value)) {
                keyValueMapWrapper.put(key, value);
            }
        }
    }

}
