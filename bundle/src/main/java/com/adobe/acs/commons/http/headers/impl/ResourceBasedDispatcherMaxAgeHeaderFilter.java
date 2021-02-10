package com.adobe.acs.commons.http.headers.impl;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ResourceBasedDispatcherMaxAgeHeaderFilter extends DispatcherMaxAgeHeaderFilter {

    private static final Logger log = LoggerFactory.getLogger(ResourceBasedDispatcherMaxAgeHeaderFilter.class);

    protected Resource getResource(SlingHttpServletRequest slingRequest) {
        if (slingRequest.getResource().isResourceType("cq:Page")) {
           log.trace("Found page resource, checking page content resource type");
            return slingRequest.getResource().getChild(JcrConstants.JCR_CONTENT);
        }
        log.trace("Found non-page resource, checking request resource type");
        return slingRequest.getResource();
    }

}
