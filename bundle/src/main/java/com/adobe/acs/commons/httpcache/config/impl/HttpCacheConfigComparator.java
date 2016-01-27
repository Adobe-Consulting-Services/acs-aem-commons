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

import java.io.Serializable;
import java.util.Comparator;

/**
 * Used to sort HttpCacheConfig @Components by their order (ascending)
 */
public class HttpCacheConfigComparator implements Comparator<HttpCacheConfig>, Serializable {

    @Override
    public int compare(final HttpCacheConfig cacheConfig1, final HttpCacheConfig cacheConfig2) {

        Integer order1 = cacheConfig1.getOrder();
        Integer order2 = cacheConfig2.getOrder();

        return order1.compareTo(order2);
    }
}