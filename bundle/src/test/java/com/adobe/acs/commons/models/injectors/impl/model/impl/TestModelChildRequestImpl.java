package com.adobe.acs.commons.models.injectors.impl.model.impl;

import com.adobe.acs.commons.models.injectors.annotation.ChildRequest;
import com.adobe.acs.commons.models.injectors.impl.model.TestModelChildRequest;
import com.adobe.acs.commons.models.injectors.impl.model.TestModelChildRequestChild;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ChildResource;

import java.util.List;

@Model(adapters = TestModelChildRequest.class, adaptables = {Resource.class, SlingHttpServletRequest.class})
public class TestModelChildRequestImpl implements TestModelChildRequest {
    @ChildRequest
    private TestModelChildRequestChild child;

    @ChildRequest
    private List<TestModelChildRequestChild> childList;

    @ChildRequest(name = "child")
    private Resource childResource;

    @ChildRequest(name = "childList")
    private List<Resource> childResourceList;

    @Override
    public TestModelChildRequestChild getChildModel() {
        return child;
    }

    @Override
    public List<TestModelChildRequestChild> getChildModelList() {
        return childList;
    }

    @Override
    public Resource getChildResource() {
        return childResource;
    }

    @Override
    public List<Resource> getChildResourceList() {
        return childResourceList;
    }
}
