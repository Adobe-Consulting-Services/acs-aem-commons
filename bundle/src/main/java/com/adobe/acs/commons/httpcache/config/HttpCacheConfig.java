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

import com.adobe.acs.commons.httpcache.exception.HttpCacheKeyCreationException;
import com.adobe.acs.commons.httpcache.exception.HttpCacheRepositoryAccessException;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import org.apache.sling.api.SlingHttpServletRequest;
import org.osgi.annotation.versioning.ProviderType;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Configuration for Http cache. Multiple configs can be supplied. Request uri, authentication details, cache store
 * details and invalidation JCR path details are captured through configs. Developer hook supplied for extension of
 * cache config via <code> HttpCacheConfigExtension</code>
 */

@ProviderType
public interface HttpCacheConfig {

    enum FilterScope {
        REQUEST,
        INCLUDE
    }

    /**
     * Name of the configured cache store.
     *
     * @return
     */
    String getCacheStoreName();

    /**
     * Get the authentication requirement for request set for this config.
     *
     * @return Uses the constants defined in {@link AuthenticationStatusConfigConstants}
     */
    String getAuthenticationRequirement();

    /**
     * Get the configured list of whitelisted request URIs.
     *
     * @return
     */
    List<Pattern> getRequestUriPatterns();

    /**
     * Get the configured list of blacklisted request URIs.
     *
     * @return
     */
    List<Pattern> getBlacklistedRequestUriPatterns();

    /**
     * Get the configured list of JCR paths that could unvalidate this config.
     *
     * @return
     */
    List<Pattern> getJCRInvalidationPathPatterns();

    /**
     * Get a list of headers (as regex pattern) that should NOT be put in the cached response, to be served to the output.
     *
     * @return
     */
    default List<Pattern> getExcludedResponseHeaderPatterns() {
        return Collections.emptyList();
    }

    /**
     * Get a list of excluded cookie keys (simple string) of cookies that should NOT be put in the cached response, to be served to the output.
     * This is useful for example with systems that put a login cookie in each response.
     *
     * @return
     */
    default List<String> getExcludedCookieKeys() {
        return Collections.emptyList();
    }

    /**
     * Determine if this cache config is applicable for the given request. Calls <code>HttpCacheConfigExtension
     * .accept()</code> for providing share of control to the custom code.
     *
     * @param request the request
     * @return true if the response should be cached, false if it should not be cached.
     */
    boolean accepts(SlingHttpServletRequest request) throws HttpCacheRepositoryAccessException;

    /**
     * @return true if this config is considered valid and processable by the HttpCacheEngine.
     */
    boolean isValid();

    /**
     * Creates the CacheKey object using the CacheKeyFactory associated with this HttpCacheConfig factory instance.
     *
     * @param request the request to create the CacheKey for
     * @return the CacheKey
     */
    CacheKey buildCacheKey(SlingHttpServletRequest request) throws HttpCacheKeyCreationException;

    /**
     * Creates the CacheKey object using the CacheKeyFactory associated with this HttpCacheConfig factory instance.
     *
     * @param resourcePath the resourcePath associated with the Cache Key
     * @return the CacheKey
     */
    CacheKey buildCacheKey(String resourcePath) throws HttpCacheKeyCreationException;


    /**
     * Determines if a JCR path is a candidate for invalidating this cache.
     *
     * @param path the jcr path
     * @return true if this config can be invalidated by a change to this path
     */
    boolean canInvalidate(String path);

    /**
     * Returns true if the key is generated using this cache config.
     *
     * @param key
     * @return
     */
    boolean knows(CacheKey key) throws HttpCacheKeyCreationException;

    /**
     * Gets the order the HttpCacheConfig should be executed in.
     *
     * @return
     */
    int getOrder();

    /**
     * Check if the cache config accepts the service pid of the cache handling rule.
     *
     * @param servicePid Service pid of HttpCacheHandlingRule.
     * @return True if it accepts.
     */
    boolean acceptsRule(String servicePid);

    /**
     * @return the filter scope this HttpCacheConfig should involve itself in.
     */
    FilterScope getFilterScope();

    /**
     * Returns a custom expiry for this config in miliseconds.
     * -1 means the entry will never expire itself.
     * 0 means the expiry is not set, and the default expiry will be used.
     * 1 or above is the expiry for entries produced by this config .
     *
     * @return
     */
    default long getExpiryOnCreate() {
        return -1L;
    }

    /**
     * Gets the expiry time for the cache entry access / read.
     * If set, it will refresh the expiry time when an entry is read with given value.
     * Value is in miliseconds.
     *
     * @return the expiry time
     */
    default long getExpiryForAccess() {
        return -1L;
    }

    /**
     * Gets the expiry time for the cache entry updated.
     * If set, it will refresh the expiry time when an entry is updated with given value.
     * Value is in miliseconds.
     *
     * @return the expiry time
     */
    default long getExpiryForUpdate() {
        return -1L;
    }
}
