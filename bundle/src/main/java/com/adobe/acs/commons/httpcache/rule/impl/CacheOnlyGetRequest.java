package com.adobe.acs.commons.httpcache.rule.impl;

import com.adobe.acs.commons.httpcache.rule.AbstractHttpCacheHandlingRule;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;

/**
 * ACS AEM Commons - HTTP Cache - Rule: Cache only request http method is GET.
 *
 * Process only Http GET requests.
 */
@Component
@Service
public class CacheOnlyGetRequest extends AbstractHttpCacheHandlingRule {
    private static final String HTTP_GET_METHOD = "GET";

    @Override
    public boolean onRequestReceive(SlingHttpServletRequest request) {
        // Return true only if Http method is GET.
        return HTTP_GET_METHOD.equals(request.getMethod());
    }
}
