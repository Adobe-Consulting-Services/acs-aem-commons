/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.adobe.acs.commons.models.injectors.impl.model.impl;

import com.adobe.acs.commons.models.injectors.annotation.ChildResourceFromRequest;
import com.adobe.acs.commons.models.injectors.impl.model.TestModelChildResourceFromRequest;
import com.adobe.acs.commons.models.injectors.impl.model.TestModelChildResourceFromRequestChild;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;

import java.util.List;

@Model(adapters = TestModelChildResourceFromRequest.class, adaptables = {Resource.class, SlingHttpServletRequest.class})
public class TestModelChildResourceFromRequestImpl implements TestModelChildResourceFromRequest {
    @ChildResourceFromRequest
    private TestModelChildResourceFromRequestChild child;

    @ChildResourceFromRequest
    private List<TestModelChildResourceFromRequestChild> childList;

    @ChildResourceFromRequest(name = "child")
    private Resource childResource;

    @ChildResourceFromRequest(name = "childList")
    private List<Resource> childResourceList;

    @Override
    public TestModelChildResourceFromRequestChild getChildModel() {
        return child;
    }

    @Override
    public List<TestModelChildResourceFromRequestChild> getChildModelList() {
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
