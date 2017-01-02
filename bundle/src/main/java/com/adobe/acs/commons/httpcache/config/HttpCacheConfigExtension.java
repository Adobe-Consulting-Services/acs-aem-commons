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
package com.adobe.acs.commons.httpcache.config;

import com.adobe.acs.commons.httpcache.exception.HttpCacheRepositoryAccessException;
import org.apache.sling.api.SlingHttpServletRequest;

/**
 * Hook to supply custom extension to the {@link HttpCacheConfig}.
 */
public interface HttpCacheConfigExtension {
    /**
     * Examine if this extension accepts the request. <p> Implementation of <code>HttpCacheConfig.accept
     * (SlingHttpServletRequest)</code> method invokes this to check if the given cache config custom attributes matches
     * with the given request.</p>
     *
     * @param request
     * @param cacheConfig
     * @return True if this cache config extension accepts the cache config custom attributes.
     * @throws HttpCacheRepositoryAccessException
     */
    boolean accepts(SlingHttpServletRequest request, HttpCacheConfig cacheConfig) throws HttpCacheRepositoryAccessException;
}
