package com.adobe.acs.commons.models.injectors.impl.model.impl;

import com.adobe.acs.commons.models.injectors.impl.model.TestModelChildRequestChild;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

@Model(adapters = TestModelChildRequestChild.class, adaptables = {Resource.class, SlingHttpServletRequest.class})
public class TestModelChildRequestChildImpl implements TestModelChildRequestChild {
    @SlingObject(injectionStrategy = InjectionStrategy.OPTIONAL)
    protected SlingHttpServletRequest request;

    @SlingObject
    protected Resource resource;

    @ValueMapValue
    private String prop;

    @Override
    public SlingHttpServletRequest getRequest() {
        return request;
    }

    @Override
    public Resource getResource() {
        return resource;
    }

    @Override
    public String getProp() {
        return prop;
    }
}
