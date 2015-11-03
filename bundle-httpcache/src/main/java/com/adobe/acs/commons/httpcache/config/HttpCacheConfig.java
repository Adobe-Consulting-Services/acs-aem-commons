package com.adobe.acs.commons.httpcache.config;

import java.util.List;

/**
 * Configuration for Http cache. Multiple configs can be supplied. Each config has an one on one mapping with request
 * pattern that has to be cached.
 */
public interface HttpCacheConfig {
    /**
     * Get the request URI set for this config.
     *
     * @return Generally this is expressed in REGEX.
     */
    String getRequestUri();

    /**
     * Get configured response mime type to be cached.
     *
     * @return Example application/json.
     */
    String getResponseMimeType();

    // TODO - Mandatory and optional list of user groups may not be required. Cached item shall be a combination of
    // request uri and the set of configured groups.

    /**
     * Get the list of configured AEM user groups which must be present in the request user's group list.
     *
     * @return
     */
    List<String> getMandatoryUserGroupNames();

    /**
     * Get the list of configured AEM user groups which could be optionally present in the request user's group list.
     *
     * @return
     */
    List<String> getOptionalUserGroupNames();

    /**
     * Name of the configured cache store.
     *
     * @return
     */
    String getCacheStoreName();

    /**
     * Get the list of configured JCR paths for which this cache will be invalidated.
     *
     * @return
     */
    List<String> getCacheInvalidationPaths();
}


