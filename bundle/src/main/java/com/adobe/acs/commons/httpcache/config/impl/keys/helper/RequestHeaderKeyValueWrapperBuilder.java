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
import com.adobe.acs.commons.httpcache.config.impl.RequestHeaderHttpCacheConfigExtension;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;

import java.util.Enumeration;
import java.util.Map;
import java.util.Set;

import static com.adobe.acs.commons.httpcache.config.impl.keys.helper.KeyValueMapWrapper.SEPERATOR;
import static org.apache.commons.lang3.StringUtils.EMPTY;

/**
 * Builds a KeyValueMapWrapperBuilder wrapper based on request headers
 */
public class RequestHeaderKeyValueWrapperBuilder implements KeyValueMapWrapperBuilder{

    private final Set<String> allowedKeys;
    private final Map<String, String> allowedValues;
    private final SlingHttpServletRequest request;
    private final KeyValueMapWrapper keyValueMapWrapper = new KeyValueMapWrapper(RequestHeaderHttpCacheConfigExtension.KEY_TOSTRING_REPRESENTATION);

    public RequestHeaderKeyValueWrapperBuilder(Set<String> allowedKeys, Map<String, String> allowedValues, SlingHttpServletRequest request) {
        this.allowedKeys = allowedKeys;
        this.allowedValues = allowedValues;
        this.request = request;
    }

    @Override
    public KeyValueMapWrapper build() {

        Enumeration<String> headerNames = request.getHeaderNames();
        while(headerNames.hasMoreElements()){

            String key = headerNames.nextElement();
            String value = request.getHeader(key);

            if (allowedValues.containsKey(key) && StringUtils.isNotBlank(value)) {
                String[] specificAllowedValues = allowedValues.get(key).split(SEPERATOR);
                for (String allowedValue : specificAllowedValues) {
                    if (allowedValue.equals(value)) {
                        keyValueMapWrapper.put(key, value);
                    }
                }
            } else if(allowedKeys.contains(key)){
                keyValueMapWrapper.put(key, EMPTY);
            }

        }
        return keyValueMapWrapper;
    }
}
