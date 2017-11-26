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
package com.adobe.acs.commons.util.impl;

import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.TabularData;

import com.adobe.granite.jmx.annotation.Description;
import com.adobe.granite.jmx.annotation.Name;

@SuppressWarnings("squid:S00112")
public interface GenericCacheMBean {

    @Description("Clear entire cache")
    void clearCache();

    @Description("Number of entries in the cache")
    long getCacheEntriesCount();

    @Description("Size of cache")
    String getCacheSize();

    @Description("Available cache stats.")
    TabularData getCacheStats() throws OpenDataException;

    @Description("Cache entry contents by key.")
    String getCacheEntry(@Name(value="Cache Key") String cacheKeyStr) throws Exception;

    @Description("Conents of cache")
    TabularData getCacheContents() throws OpenDataException;

}
