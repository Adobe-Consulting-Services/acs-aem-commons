package com.adobe.acs.commons.httpcache.rule.impl;

import com.adobe.acs.commons.httpcache.rule.AbstractHttpCacheHandlingRule;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;

/**
 * Process only Http GET requests.
 */
@Component(label = "ACS AEM Commons - HTTP Cache - Rule: Cache only request http method is GET.",
           description = "Process only Http GET requests.",
           immediate = true)
@Service
public class CacheOnlyGetRequest extends AbstractHttpCacheHandlingRule {
    private static final String HTTP_GET_METHOD = "GET";

    @Override
    public boolean onRequestReceive(SlingHttpServletRequest request) {
        // Return true only if Http method is GET.
        return HTTP_GET_METHOD.equals(request.getMethod());
    }
}
