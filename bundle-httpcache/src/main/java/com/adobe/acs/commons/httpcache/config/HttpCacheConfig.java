package com.adobe.acs.commons.httpcache.config;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Configuration for Http cache. Multiple configs can be supplied. Request uri, authentication details, aem user group
 * details, cache store details and invalidation JCR path details are captured through configs. <ul><li>Blacklisted URIs
 * evaluated after evaluating the request URIs and hence have an overriding effect.</li><li>User groups are applied only
 * for configs that need authenticated requests. </li><li>User group list represents logical OR condition and at least
 * one of the groups should be present in the request.</li><li>If 2 cache configs with the same url pattern and
 * different list of groups present, one with high match score of groups with request would be considered. In case of
 * levelled score, cache will be rejected.</li><li>Cache invalidation path is the JCR path expressed in REGEX
 * pattern.</li></ul>
 */
public interface HttpCacheConfig {
    /**
     * Get the request URIs set for this config.
     *
     * @return List of URIs expressed in REGEX.
     */
    List<String> getRequestURIs();

    /**
     * Get the request URIs set for this config in the form of regular expression patterns.
     *
     * @return
     */
    List<Pattern> getRequestURIsAsRegEx();

    /**
     * Get the blacklisted URIs.
     *
     * @return List of URIs expressed in REGEX.
     */
    List<String> getBlacklistedURIs();

    /**
     * Get the blacklisted request URIs set for this config in the form of regular expression patterns.
     *
     * @return
     */
    List<Pattern> getBlacklistedURIsAsRegEx();

    /**
     * Get the authentication requirement set for this config.
     *
     * @return One of the constants defined in {@link AuthenticationStatusConfigConstants}
     */
    String getAuthenticationRequirement();


    /**
     * Get the configured AEM user groups in which at least one of them must be present in the request user's group
     * list.
     *
     * @return
     */
    List<String> getUserGroupNames();

    /**
     * Name of the configured cache store.
     *
     * @return
     */
    String getCacheStoreName();

    /**
     * Get the configured JCR path patterns (REGEX) for which this cache will be invalidated.
     *
     * @return
     */
    List<String> getCacheInvalidationPaths();

    /**
     * Get the configured JCR path patterns (REGEX) for which this cache will be invalidated as regular expression
     * pattern.
     *
     * @return
     */
    List<Pattern> getCacheInvalidationPathsAsRegEx();
}

