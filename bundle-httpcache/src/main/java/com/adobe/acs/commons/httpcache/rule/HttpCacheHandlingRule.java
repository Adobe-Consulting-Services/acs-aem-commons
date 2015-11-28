package com.adobe.acs.commons.httpcache.rule;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.engine.CacheContent;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;

/**
 * Rules which impacts the behavior of http cache. Concrete implementation of this interface provides developers hooks
 * to supply custom behavior for key events in http cache. <p> Each method represents a hook to the http cache event.In
 * case if a method is not applicable for a custom rule, make it return true indicating the cache engine to continue
 * processing the next rule. </p>
 */
public interface HttpCacheHandlingRule {

    /**
     * Hook to supply custom behavior on {@link com.adobe.acs.commons.httpcache.engine.HttpCacheEngine} receiving the
     * request.
     *
     * @param request
     * @return True represents success and cache handling rules will be continued. False represents failure with cache
     * handling rules being stopped and fallback action will be taken.
     */
    boolean onRequestReceive(SlingHttpServletRequest request);

    /**
     * Hook to supply custom behavior on {@link com.adobe.acs.commons.httpcache.engine.HttpCacheEngine} about to cache a
     * response.
     *
     * @param request
     * @param response
     * @param cacheConfig
     * @param cacheContent Object carring data to be cached.
     * @return True represents success and cache handling rules will be continued. False represents failure with cache
     * handling rules being stopped and fallback action will be taken.
     */
    boolean onResponseCache(SlingHttpServletRequest request, SlingHttpServletResponse response, HttpCacheConfig
            cacheConfig, CacheContent cacheContent);

    /**
     * Hook to supply custom behavior on {@link com.adobe.acs.commons.httpcache.engine.HttpCacheEngine} delivering cache
     * content after being read from cache store.
     *
     * @param request
     * @param response
     * @param cacheConfig
     * @param cacheContent Object carrying data to be delivered.
     * @return True represents success and cache handling rules will be continued. False represents failure with cache
     * handling rules being stopped and fallback action will be taken.
     */
    boolean onCacheDeliver(SlingHttpServletRequest request, SlingHttpServletResponse response, HttpCacheConfig
            cacheConfig, CacheContent cacheContent);

    /**
     * Hook to supply custom behavior on {@link com.adobe.acs.commons.httpcache.engine.HttpCacheEngine} invalidating
     * cache for the changes in the given JCR repository path.
     *
     * @param path JCR repository path
     * @return True represents success and cache handling rules will be continued. False represents failure with cache
     * handling rules being stopped and fallback action will be taken.
     */
    boolean onCacheInvalidate(String path);
}

