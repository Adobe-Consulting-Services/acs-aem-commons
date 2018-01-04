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
package com.adobe.acs.commons.httpcache.filter.impl;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.engine.HttpCacheEngine;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.sling.SlingFilter;
import org.apache.felix.scr.annotations.sling.SlingFilterScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

/**
 * ACS AEM Commons - Http Cache - Intercepting include filter for dealing with cache. Intercepting sling request filter
 * to introduce caching layer. Works with {@link com.adobe.acs.commons.httpcache .engine.HttpCacheEngine} to deal with
 * caching aspects.
 */
@SlingFilter(
        generateComponent = true,
        generateService = true,
        order = 0,
        scope = { SlingFilterScope.INCLUDE })
public class HttpCacheIncludeFilter extends AbstractHttpCacheFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(HttpCacheIncludeFilter.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private HttpCacheEngine cacheEngine;

    // Only instantiate this Filter if there is at least 1 INCLUE-based cache config
    @Reference(
            cardinality = ReferenceCardinality.MANDATORY_UNARY,
            target = "(httpcache.config.filter-scope=INCLUDE)"
    )
    private HttpCacheConfig inludeScopeCacheConfigs;


    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        log.trace("In HttpCache Include filter.");
        doFilter(request, response, chain, cacheEngine, HttpCacheConfig.FilterScope.INCLUDE);
    }

    //---------------<Do nothing methods. Just to satisfy interface contract>
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // no-op
    }

    @Override
    public void destroy() {
        // no-op
    }
}
