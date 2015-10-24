package com.adobe.acs.commons.httpcache.rule;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;

/**
 * Utility abstract implementation providing default behavior for any cache handling rules.
 * <p>
 * All methods in this return true indicating that this does nothing other than instructing the {@link
 * com.adobe.acs.commons.httpcache.engine.HttpCacheEngine} to move on with next rule. Any custom implementation could
 * leverage this to facilitate overriding only the methods the custom rule is intended for.
 * </p>
 */
public abstract class AbstractHttpCacheHandlingRule implements HttpCacheHandlingRule {

    @Override
    public boolean onRequestReceive(SlingHttpServletRequest request) {
        return true;
    }

    @Override
    public boolean onResponseCache(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        return true;
    }

    @Override
    public boolean onCacheDeliver(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        return true;
    }

    @Override
    public boolean onCacheInvalidate(String path) {
        return true;
    }
}
