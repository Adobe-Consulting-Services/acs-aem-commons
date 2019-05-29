/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2019 Adobe
 * %%
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
 * #L%
 */
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
