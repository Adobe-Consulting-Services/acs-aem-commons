package com.adobe.acs.commons.models.injectors.impl.model;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;

public interface TestModelChildRequestChild {
    SlingHttpServletRequest getRequest();

    Resource getResource();

    String getProp();
}
