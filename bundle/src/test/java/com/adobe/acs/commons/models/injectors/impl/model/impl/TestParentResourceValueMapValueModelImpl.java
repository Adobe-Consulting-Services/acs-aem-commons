/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2020 Adobe
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

import com.adobe.acs.commons.models.injectors.annotation.ParentResourceValueMapValue;
import com.adobe.acs.commons.models.injectors.impl.model.TestParentResourceValueMapValueModel;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;

@Model(adapters = TestParentResourceValueMapValueModel.class,
        adaptables = {Resource.class, SlingHttpServletRequest.class},
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class TestParentResourceValueMapValueModelImpl implements TestParentResourceValueMapValueModel {

    @ParentResourceValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL, maxLevel = 1)
    private String stringProperty;

    @ParentResourceValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL, maxLevel = 1)
    private String stringLevel2Property;

    @ParentResourceValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL, maxLevel = 2, name = "jcr:title")
    private String title;

    @ParentResourceValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    private Boolean booleanProperty;

    @ParentResourceValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    private String[] stringProperties;

    @Override
    public String getStringProperty() {
        return stringProperty;
    }

    @Override
    public String getStringLevel2Property() {
        return stringLevel2Property;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public Boolean getBooleanProperty() {
        return booleanProperty;
    }

    @Override
    public String[] getStringProperties() {
        return stringProperties;
    }
}
