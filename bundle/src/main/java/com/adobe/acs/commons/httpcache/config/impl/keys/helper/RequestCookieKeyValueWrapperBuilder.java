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

import javax.servlet.http.Cookie;
import java.util.Map;
import java.util.Set;

/**
 /**
 * Builds a KeyValueMapWrapperBuilder wrapper based on request cookies
 */
public class RequestCookieKeyValueWrapperBuilder implements KeyValueMapWrapperBuilder {

    private final Set<String> allowedKeys;
    private final Map<String, String> cookieKeyValues;
    private final Set<Cookie> presentCookies;
    private final KeyValueMapWrapper keyValueMap = new KeyValueMapWrapper(RequestCookieHttpCacheConfigExtension.KEY_STRING_REPRENSENTATION);

    public RequestCookieKeyValueWrapperBuilder(Set<String> allowedKeys, Map<String, String> cookieKeyValues, Set<Cookie> presentCookies) {

        this.allowedKeys = allowedKeys;
        this.cookieKeyValues = cookieKeyValues;
        this.presentCookies = presentCookies;
    }


    @Override
    public KeyValueMapWrapper build() {

        presentCookies.stream()
                .filter(cookie -> {
                    final String key = cookie.getName();

                    if (cookieKeyValues.containsKey(key)) {
                        String[] values = cookieKeyValues.get(key).split("\\|");
                        for (String value : values) {
                            if (value.equalsIgnoreCase(cookie.getValue())) {
                                return true;
                            }
                        }
                        return false;
                    } else {
                        return allowedKeys.contains(key);
                    }
                })
                .forEach(cookie -> keyValueMap.put(cookie.getName(), cookie.getValue()));

        return keyValueMap;
    }

}
