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

import com.adobe.acs.commons.models.injectors.annotation.JsonValueMapValue;
import com.adobe.acs.commons.models.injectors.impl.model.TestJsonObjectInjection;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;

import java.util.List;
import java.util.Set;


@Model(
        adapters = TestJsonObjectInjection.class,
        adaptables = {SlingHttpServletRequest.class, Resource.class}, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class TestJsonObjectInjectionImpl implements TestJsonObjectInjection {

    @JsonValueMapValue(name = "single")
    private TestJsonObject testJsonObject;

    @JsonValueMapValue(name = "multiple")
    private List<TestJsonObject> testJsonObjectList;

    @JsonValueMapValue(name = "multiple")
    private Set<TestJsonObject> testJsonObjectSet;

    @JsonValueMapValue(name = "multiple")
    private TestJsonObject[] testJsonObjectArray;

    @JsonValueMapValue(name = "nonExistingProp")
    private TestJsonObject testJsonObjectEmpty;

    @JsonValueMapValue(name = "nonExistingProp")
    private List<TestJsonObject> testJsonObjectListEmpty;

    @JsonValueMapValue(name = "nonExistingProp")
    private Set<TestJsonObject> testJsonObjectSetEmpty;

    @JsonValueMapValue(name = "nonExistingProp")
    private TestJsonObject[] testJsonObjectArrayEmpty;


    @Override
    public TestJsonObject getTestJsonObject() {
        return testJsonObject;
    }

    @Override
    public List<TestJsonObject> getTestJsonObjectList() {
        return testJsonObjectList;
    }

    @Override
    public Set<TestJsonObject> getTestJsonObjectSet() {
        return testJsonObjectSet;
    }

    @Override
    public TestJsonObject[] getTestJsonObjectArray() {
        return testJsonObjectArray;
    }

    @Override
    public TestJsonObject getTestJsonObjectEmpty() {
        return testJsonObjectEmpty;
    }

    @Override
    public List<TestJsonObject> getTestJsonObjectListEmpty() {
        return testJsonObjectListEmpty;
    }

    @Override
    public Set<TestJsonObject> getTestJsonObjectSetEmpty() {
        return testJsonObjectSetEmpty;
    }

    @Override
    public TestJsonObject[] getTestJsonObjectArrayEmpty() {
        return testJsonObjectArrayEmpty;
    }
}
