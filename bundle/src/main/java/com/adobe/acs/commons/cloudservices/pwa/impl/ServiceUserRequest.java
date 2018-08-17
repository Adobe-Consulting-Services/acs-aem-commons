package com.adobe.acs.commons.cloudservices.pwa.impl;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;

class ServiceUserRequest extends SlingHttpServletRequestWrapper {
    private final ResourceResolver resourceResolver;

    public ServiceUserRequest(final SlingHttpServletRequest request, final ResourceResolver serviceResourceResolver) {
        super(request);
        resourceResolver = serviceResourceResolver;
    }

    public ResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    public Resource getResource() {
        return getResourceResolver().getResource(super.getResource().getPath());
    }
}
