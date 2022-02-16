/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
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

package com.adobe.acs.commons.errorpagehandler.cache.impl;

import com.adobe.granite.jmx.annotation.Description;

import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.TabularData;

@Description("ACS AEM Commons - Error Page Handler Cache")
public interface ErrorPageCacheMBean {
    @Description("Cache TTL in Seconds")
    int getTtlInSeconds();

    @Description("Total number of requests to the cache")
    int getTotalCacheRequests();

    @Description("Total cache misses")
    int getTotalMisses();

    @Description("Total cache hits")
    int getTotalHits();

    @Description("Total cache misses")
    int getCacheEntriesCount();

    @Description("Total cache size in KB")
    @SuppressWarnings("checkstyle:abbreviationaswordinname")
    long getCacheSizeInKB();

    @Description("Details for each cache entry")
    TabularData getCacheEntries() throws OpenDataException;

    /* Operations */
    @Description("Clear entire cache")
    void clearCache();

    @Description("Get the cached data for a specific Error Page. (Ex. getCacheData('/content/site/error/404.html'))")
    String getCacheData(String errorPage);
}
