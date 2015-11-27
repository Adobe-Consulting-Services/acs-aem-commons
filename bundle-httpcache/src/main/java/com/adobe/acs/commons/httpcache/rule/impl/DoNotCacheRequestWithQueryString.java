package com.adobe.acs.commons.httpcache.rule.impl;

import com.adobe.acs.commons.httpcache.rule.AbstractHttpCacheHandlingRule;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;

/**
 * Do not cache requests which has got query strings. <p>Need to supply sling:OSGiConfig node to get this active.</p>
 */
@Component(label = "ACS AEM Commons - HTTP Cache - Rule: Do not cache query string requests.",
           description = "Do not cache requests which has got query strings.",
           immediate = true,
           metatype = true,
           policy = ConfigurationPolicy.REQUIRE)
@Service
public class DoNotCacheRequestWithQueryString extends AbstractHttpCacheHandlingRule {

    @Override
    public boolean onRequestReceive(SlingHttpServletRequest request) {
        // Return true only if the query string is absent.
        return StringUtils.isEmpty(request.getQueryString());
    }
}
