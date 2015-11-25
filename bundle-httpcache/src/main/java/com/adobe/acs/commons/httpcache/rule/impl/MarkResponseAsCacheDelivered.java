package com.adobe.acs.commons.httpcache.rule.impl;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.engine.CacheContent;
import com.adobe.acs.commons.httpcache.rule.AbstractHttpCacheHandlingRule;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;

/**
 * Attach k,v to response header marking it as delivered from cache. Useful while developing.
 */
@Component(label = "ACS AEM Commons - HTTP Cache - Rule: Mark response as cache delivered.",
           description = "Attach k,v to response header marking it as delivered from cache.",
           immediate = true,
           policy = ConfigurationPolicy.REQUIRE)
public class MarkResponseAsCacheDelivered extends AbstractHttpCacheHandlingRule {
    private static final String KEY_HTTPCACHE_MARK = "acs-commons-httpcache";
    private static final String VALUE_HTTPCACHE_MARK = "cache-delivered";

    @Override
    public boolean onCacheDeliver(SlingHttpServletRequest request, SlingHttpServletResponse response, HttpCacheConfig
            cacheConfig, CacheContent cacheContent) {

        response.addHeader(KEY_HTTPCACHE_MARK, VALUE_HTTPCACHE_MARK);
        
        return true;
    }
}
