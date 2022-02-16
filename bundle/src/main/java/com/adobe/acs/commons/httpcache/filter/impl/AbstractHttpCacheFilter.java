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
import com.adobe.acs.commons.httpcache.exception.HttpCacheException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

public abstract class AbstractHttpCacheFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(AbstractHttpCacheFilter.class);

    @Override
    public abstract void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException;

    @SuppressWarnings("squid:S3776")
    protected void doFilter(ServletRequest request, ServletResponse response, FilterChain chain,
                            HttpCacheEngine cacheEngine, HttpCacheConfig.FilterScope filterScope) throws IOException, ServletException {
        log.trace("In HttpCache filter.");

        final long start = System.currentTimeMillis();

        final SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;
        SlingHttpServletResponse slingResponse = (SlingHttpServletResponse) response;

        HttpCacheConfig cacheConfig = null;
        boolean isResponseCacheable = false;

        try {
            // Get the first accepting cache config, or null if no accepting cacheConfigs can be found.
            cacheConfig = cacheEngine.getCacheConfig(slingRequest, filterScope);

            // Check if the url is cache-able as per configs and rules.
            // An accepting cacheConfig must exist and all cache rules must be met.
            if (cacheConfig != null && cacheEngine.isRequestCacheable(slingRequest, cacheConfig)) {
                // Check if cached response available for this request.
                if (cacheEngine.isCacheHit(slingRequest, cacheConfig)) {
                    // Deliver the response from cache.
                    if (cacheEngine.deliverCacheContent(slingRequest, slingResponse, cacheConfig)) {
                        if (log.isDebugEnabled()) {
                            log.debug("Delivered cached request [ {} ] in {} ms", slingRequest.getResource().getPath(),
                                    System.currentTimeMillis() - start);
                        }
                        return;
                    }
                } else {
                    // Mark the request as cacheable once processed.
                    isResponseCacheable = true;
                    // Wrap the response
                    slingResponse = cacheEngine.wrapResponse(slingRequest, slingResponse, cacheConfig);
                }
            }
        } catch (HttpCacheException e) {
            log.error("HttpCache exception while dealing with request. Passed on the control to filter chain.", e);
        }

        // Pass on the request to filter chain.
        chain.doFilter(request, slingResponse);

        try {
            // If the request has the attribute marked, cache the response.
            if (isResponseCacheable) {
                cacheEngine.cacheResponse(slingRequest, slingResponse, cacheConfig);
            }

            if (log.isTraceEnabled()) {
                log.trace("Delivered un-cached request [ {} ] in {} ms",  slingRequest.getResource().getPath(),
                        System.currentTimeMillis() - start);
            }
        } catch (HttpCacheException e) {
            log.error("HttpCache exception while dealing with response. Returned the filter chain response", e);
        }
    }

    //---------------<Do nothing methods. Just to satisfy interface contract>
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void destroy() {
    }
}
