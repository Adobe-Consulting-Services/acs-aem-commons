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

import com.adobe.acs.commons.models.injectors.annotation.PageProperty;
import com.adobe.acs.commons.models.injectors.impl.model.TestPagePropertiesModel;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;

@Model(
        adapters = TestPagePropertiesModel.class,
        adaptables = {SlingHttpServletRequest.class, Resource.class},
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class TestPagePropertiesModelModelImpl implements TestPagePropertiesModel {

    @PageProperty
    private String hierarchicalPagePropertyString;
    @PageProperty
    private String pagePropertyString;

    @Override
    public String getPagePropertyString() {
        return pagePropertyString;
    }

    @Override
    public String getHierarchicalPagePropertyString() {
        return hierarchicalPagePropertyString;
    }

}
