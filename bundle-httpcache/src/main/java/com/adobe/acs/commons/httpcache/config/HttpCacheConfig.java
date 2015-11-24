package com.adobe.acs.commons.httpcache.config;

import com.adobe.acs.commons.httpcache.exception.HttpCacheKeyCreationException;
import com.adobe.acs.commons.httpcache.exception.HttpCacheReposityAccessException;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import org.apache.sling.api.SlingHttpServletRequest;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Configuration for Http cache. Multiple configs can be supplied. Request uri, authentication details, aem user group
 * details, cache store details and invalidation JCR path details are captured through configs. <ul><li>Blacklisted URIs
 * evaluated after evaluating the request URIs and hence have an overriding effect.</li><li>User groups are applied only
 * for configs that need authenticated requests. </li><li>User group list represents logical OR condition and at least
 * one of the groups should be present in the request.</li><li>If 2 cache configs with the same url pattern and
 * different list of groups present, one with high match score of groups with request would be considered. In case of
 * levelled score, cache will be rejected.</li><li>Cache invalidation path is the JCR path expressed in REGEX.
 * pattern.</li></ul>
 */
public interface HttpCacheConfig {
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
    List<Pattern> getJCRInvalidationPaths();

    /**
     * Get the list of custom attributes captured through configuration which is supposed to be supplied to the
     * extensions.
     *
     * @return
     */
    List<String> getCustomConfigAttributes();

    /**
     * Determine if this cache config is applicable for the given request. Calls <code>HttpCacheConfigExtension
     * .accept()</code> for providing share of control to the custom code.
     *
     * @param request the request
     * @return true if the response should be cached, false if it should not be cached.
     */
    boolean accepts(SlingHttpServletRequest request) throws HttpCacheReposityAccessException;

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
}
