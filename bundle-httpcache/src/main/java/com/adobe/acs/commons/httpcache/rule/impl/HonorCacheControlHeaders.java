package com.adobe.acs.commons.httpcache.rule.impl;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.engine.CacheContent;
import com.adobe.acs.commons.httpcache.rule.AbstractHttpCacheHandlingRule;
import org.apache.commons.collections.CollectionUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;

import java.util.Arrays;
import java.util.List;

/**
 * Cache only Http response status for the request is 200.
 */
@Component(label = "ACS AEM Commons - HTTP Cache - Rule: Honor cache control headers",
           description = "Do not cache the response when it's set with cache control headers marking it as not " +
                   "cacheable.",
           immediate = true)
public class HonorCacheControlHeaders extends AbstractHttpCacheHandlingRule {
    private static final String KEY_CACHE_CONTROL_HEADER = "Cache-Control"; // HTTP 1.1
    private static final String[] VALUES_CACHE_CONTROL = {"no-cache, no-store, must-revalidate"};
    private static final String KEY_PRAGMA = "Pragma"; // HTTP 1.0
    private static final String[] VALUES_PRAGMA = {"no-cache"};

    @Override
    public boolean onResponseCache(SlingHttpServletRequest request, SlingHttpServletResponse response, HttpCacheConfig cacheConfig, CacheContent cacheContent) {
        // Check cache control header
        if (cacheContent.getHeaders().containsKey(KEY_CACHE_CONTROL_HEADER)) {
            List<String> cacheControlValues = cacheContent.getHeaders().get(KEY_CACHE_CONTROL_HEADER);
            if (CollectionUtils.containsAny(cacheControlValues, Arrays.asList(VALUES_CACHE_CONTROL))) {
                return false;
            }
        }

        // Check Pragma.
        if (cacheContent.getHeaders().containsKey(KEY_PRAGMA)) {
            List<String> pragmaValues = cacheContent.getHeaders().get(KEY_PRAGMA);
            if (CollectionUtils.containsAny(pragmaValues, Arrays.asList(VALUES_PRAGMA))) {
                return false;
            }
        }

        return true;
    }
}
