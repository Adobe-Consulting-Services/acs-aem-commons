package com.adobe.acs.commons.httpcache.config;

/**
 * Configuration for Http cache. Multiple configs can be supplied. Each config has an one on one mapping with request
 * pattern that has to be cached.
 */
public interface HttpCacheConfig {
    // TODO Add methods exposing config parameters.

    /**
     * Get the request URI set for this config.
     * @return Generally this is expressed in REGEX.
     */
    String getRequestUri();
}
