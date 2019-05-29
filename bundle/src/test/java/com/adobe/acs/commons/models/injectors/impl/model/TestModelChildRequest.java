package com.adobe.acs.commons.models.injectors.impl.model;

import com.adobe.acs.commons.models.injectors.impl.model.impl.TestModelChildRequestImpl;
import org.apache.sling.api.resource.Resource;

import java.util.List;

public interface TestModelChildRequest {
    TestModelChildRequestChild getChildModel();

    List<TestModelChildRequestChild> getChildModelList();

    Resource getChildResource();

    List<Resource> getChildResourceList();
}
