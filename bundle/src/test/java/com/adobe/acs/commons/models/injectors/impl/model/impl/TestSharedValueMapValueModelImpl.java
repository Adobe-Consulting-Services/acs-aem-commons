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

import com.adobe.acs.commons.models.injectors.annotation.SharedValueMapValue;
import com.adobe.acs.commons.models.injectors.impl.SharedValueMapValueInjectorTest;
import com.adobe.acs.commons.models.injectors.impl.model.TestSharedValueMapValueModel;
import com.adobe.acs.commons.wcm.PageRootProvider;
import com.adobe.acs.commons.wcm.properties.shared.SharedComponentProperties;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import java.util.Collection;
import java.util.List;

@Model(adapters = TestSharedValueMapValueModel.class, adaptables = {Resource.class, SlingHttpServletRequest.class})
public class TestSharedValueMapValueModelImpl implements TestSharedValueMapValueModel {
    @SlingObject
    private Resource resource;

    @OSGiService
    private PageRootProvider prp;

    @SharedValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL, name = SharedValueMapValueInjectorTest.STRING_PROP, type = SharedComponentProperties.ValueTypes.GLOBAL)
    private String globalStringProp;

    @SharedValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL, name = SharedValueMapValueInjectorTest.STRING_PROP, type = SharedComponentProperties.ValueTypes.SHARED)
    private String sharedStringProp;

    @SharedValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL, name = SharedValueMapValueInjectorTest.STRING_PROP, type = SharedComponentProperties.ValueTypes.MERGED)
    private String mergedStringProp;

    @SharedValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    private String stringProp;

    @SharedValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    private String stringProp2;

    @SharedValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    private String stringProp3;

    @SharedValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    private Long longProp;

    @SharedValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL, name = SharedValueMapValueInjectorTest.LONG_PROP_STR)
    private Long longPropFromString;

    @SharedValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    private boolean boolPropTrue;

    @SharedValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    private boolean boolPropFalse;

    @SharedValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL, name = SharedValueMapValueInjectorTest.BOOL_PROP_TRUE_STR)
    private boolean boolPropTrueFromString;

    @SharedValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL, name = SharedValueMapValueInjectorTest.BOOL_PROP_FALSE_STR)
    private boolean boolPropFalseFromString;

    @SharedValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    private String[] stringArrayProp;

    @SharedValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL, name = SharedValueMapValueInjectorTest.STRING_ARRAY_PROP)
    private List<String> stringListProp;

    @SharedValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL, name = SharedValueMapValueInjectorTest.STRING_ARRAY_PROP)
    private Collection<String> stringCollectionProp;

    @SharedValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    private Long[] longArrayProp;

    @SharedValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL, name = SharedValueMapValueInjectorTest.LONG_ARRAY_PROP)
    private List<Long> longListProp;

    @SharedValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL, name = SharedValueMapValueInjectorTest.LONG_ARRAY_PROP)
    private Collection<Long> longCollectionProp;

    @SharedValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL, name = SharedValueMapValueInjectorTest.LONG_PROP)
    private Long[] longArrayPropFromNonArray;

    @SharedValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL, name = SharedValueMapValueInjectorTest.LONG_PROP)
    private List<Long> longListPropFromNonArray;

    @Override
    public String getGlobalStringProp() {
        return globalStringProp;
    }

    @Override
    public String getSharedStringProp() {
        return sharedStringProp;
    }

    @Override
    public String getMergedStringProp() {
        return mergedStringProp;
    }

    @Override
    public String getStringProp() {
        return stringProp;
    }

    @Override
    public String getStringProp2() {
        return stringProp2;
    }

    @Override
    public String getStringProp3() {
        return stringProp3;
    }

    @Override
    public Long getLongProp() {
        return longProp;
    }

    @Override
    public Long getLongPropFromString() {
        return longPropFromString;
    }

    @Override
    public boolean isBoolPropTrue() {
        return boolPropTrue;
    }

    @Override
    public boolean isBoolPropFalse() {
        return boolPropFalse;
    }

    @Override
    public boolean isBoolPropTrueFromString() {
        return boolPropTrueFromString;
    }

    @Override
    public boolean isBoolPropFalseFromString() {
        return boolPropFalseFromString;
    }


    @Override
    public String[] getStringArrayProp() {
        return stringArrayProp;
    }

    @Override
    public List<String> getStringListProp() {
        return stringListProp;
    }

    @Override
    public Collection<String> getStringCollectionProp() {
        return stringCollectionProp;
    }

    @Override
    public Long[] getLongArrayProp() {
        return longArrayProp;
    }

    @Override
    public List<Long> getLongListProp() {
        return longListProp;
    }

    @Override
    public Collection<Long> getLongCollectionProp() {
        return longCollectionProp;
    }

    @Override
    public Long[] getLongArrayPropFromNonArray() {
        return longArrayPropFromNonArray;
    }

    @Override
    public List<Long> getLongListPropFromNonArray() {
        return longListPropFromNonArray;
    }

}
