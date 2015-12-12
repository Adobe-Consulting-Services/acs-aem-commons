package com.adobe.acs.commons.httpcache.rule.impl;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.engine.CacheContent;
import com.adobe.acs.commons.httpcache.rule.AbstractHttpCacheHandlingRule;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;

/**
 * ACS AEM Commons - HTTP Cache - Rule: Do not cache response which is size zero. Cancel the caching of response when it
 * has no bytes.
 */
@Component
@Service
public class DoNotCacheZeroSizeResponse extends AbstractHttpCacheHandlingRule {

    @Override
    public boolean onResponseCache(SlingHttpServletRequest request, SlingHttpServletResponse response,
                                   HttpCacheConfig cacheConfig, CacheContent cacheContent) {

        // Cancel the caching if no bytes in the sink.
        if ((null != cacheContent.getTempSink()) && (0 == cacheContent.getTempSink().length())) {
            return false;
        }
        return true;
    }
}
