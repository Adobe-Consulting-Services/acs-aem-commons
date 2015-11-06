package com.adobe.acs.commons.httpcache.config;

import java.util.List;

/**
 * Configuration for Http cache. Multiple configs can be supplied. Each config has an one on one mapping with request
 * pattern that has to be cached. Apart from request uri, authentication details, aem user group details, cache store
 * details and invalidation JCR path details are captured through configs. <ul><li>User groups are applied only for
 * configs that need authenticated requests. </li><li>User group list represents logical OR condition and at least one
 * of the groups should be present in the request.</li><li>If 2 cache configs with the same url pattern and different
 * list of groups present, one with high match score of groups with request would be considered. In case of levelled
 * score, cache will be rejected.</li><li>Cache invalidation path is the JCR path expressed in REGEX pattern.</li></ul>
 */
public interface HttpCacheConfig {
    /**
     * Get the request URI set for this config.
     *
     * @return Generally this is expressed in REGEX.
     */
    String getRequestUri();

    /**
     * Check if this config applies for authenticated requests.
     *
     * @return True if yes.
     */
    boolean isRequestAuthenticationRequired();

    /**
     * Get the list of configured AEM user groups in which at least one of them must be present in the request user's
     * group list.
     *
     * @return
     */
    List<String> geUserGroupNames();

    /**
     * Name of the configured cache store.
     *
     * @return
     */
    String getCacheStoreName();

    /**
     * Get the list of configured JCR path patterns (REGEX for which this cache will be invalidated.
     *
     * @return
     */
    List<String> getCacheInvalidationPaths();
}


