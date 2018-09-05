/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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

import com.adobe.granite.jmx.annotation.Description;

@Description("ACS AEM Commons - Http Cache - JCR Cache")
public interface JcrCacheMBean extends CacheMBean
{
    @Description("Cache TTL in Seconds. -1 value represent no TTL.")
    long getTtl();

    @Description("Force scheduled purge run")
    void purgeExpiredEntries();

    @Description("Reset to cache statistics to 0")
    void resetCacheStats();
}
