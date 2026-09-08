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

import com.adobe.acs.commons.models.injectors.annotation.HierarchicalPageProperty;
import com.adobe.acs.commons.models.injectors.impl.model.TestHierarchicalPagePropertiesModel;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;

import java.util.List;
import java.util.Set;

@Model(
        adapters = TestHierarchicalPagePropertiesModel.class,
        adaptables = {SlingHttpServletRequest.class, Resource.class},
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class TestHierarchicalPagePropertiesModelModelImpl implements TestHierarchicalPagePropertiesModel {

    @HierarchicalPageProperty
    private String pagePropertyString;

    @HierarchicalPageProperty
    private String hierarchicalPagePropertyString;

    @HierarchicalPageProperty(useCurrentPage = true)
    private String currentPagePropertyString;

    @HierarchicalPageProperty(useCurrentPage = true)
    private String currentPageHierarchicalPagePropertyString;

    @HierarchicalPageProperty(traverseFromAbsoluteParent  = 3)
    private String skipLevelHierarchicalPagePropertyString;

    @HierarchicalPageProperty(value = "hierarchicalPagePropertyBoolean", inherit = false)
    private boolean hierarchicalPagePropertyBoolean;

    @HierarchicalPageProperty
    private int hierarchicalPagePropertyInteger;

    @HierarchicalPageProperty("hierarchicalPagePropertyMultiValueInteger")
    private int[] hierarchicalPagePropertyMultivaluePrimitiveInteger;

    @HierarchicalPageProperty("hierarchicalPagePropertyMultiValueInteger")
    private List<Integer> hierarchicalPagePropertyMultivalueWrappedIntegerList;

    @HierarchicalPageProperty
    private String[] hierarchicalPagePropertyMultivalueStringArray;

    @HierarchicalPageProperty
    private Set<Long> hierarchicalPagePropertyUnsupportedType;

    @HierarchicalPageProperty
    private String undefinedProperty;

    @HierarchicalPageProperty
    private Double[] hierarchicalPagePropertyMultivalueWrappedDoubleArray;


    @Override
    public String getPagePropertyString() {
        return pagePropertyString;
    }

    @Override
    public String getCurrentPagePropertyString() {
        return currentPagePropertyString;
    }

    @Override
    public String getHierarchicalPagePropertyString() {
        return hierarchicalPagePropertyString;
    }

    @Override
    public String getCurrentPageHierarchicalPagePropertyString() {
        return currentPageHierarchicalPagePropertyString;
    }

    @Override
    public boolean getPropertyBoolean() {
        return hierarchicalPagePropertyBoolean;
    }

    @Override
    public int getPropertyInteger() {
        return hierarchicalPagePropertyInteger;
    }

    @Override
    public int[] getPropertyMultivaluePrimitiveInteger() {
        return hierarchicalPagePropertyMultivaluePrimitiveInteger;
    }

    @Override
    public List<Integer> getPropertyMultivalueWrappedIntegerList() {
        return hierarchicalPagePropertyMultivalueWrappedIntegerList;
    }

    @Override
    public String[] getPropertyMultivalueStringArray() {
        return hierarchicalPagePropertyMultivalueStringArray;
    }

    @Override
    public Set<Long> getPropertyUnsupportedType() {
        return hierarchicalPagePropertyUnsupportedType;
    }

    @Override
    public String getUndefinedProperty() {
        return undefinedProperty;
    }

    @Override
    public Double[] getPropertyMultivalueWrappedDoubleArray() {
        return hierarchicalPagePropertyMultivalueWrappedDoubleArray;
    }

    @Override
    public String getSkipLevelHierarchicalPagePropertyString() {
        return skipLevelHierarchicalPagePropertyString;
    }
}
