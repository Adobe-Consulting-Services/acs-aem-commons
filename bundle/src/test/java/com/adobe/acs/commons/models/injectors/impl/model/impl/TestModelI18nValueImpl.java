/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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

import com.adobe.acs.commons.models.injectors.annotation.I18N;
import com.adobe.acs.commons.models.injectors.impl.model.TestModelI18nValue;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;

import javax.inject.Inject;

@Model(
        adapters = {TestModelI18nValue.class},
        adaptables = {SlingHttpServletRequest.class, Resource.class},
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class TestModelI18nValueImpl implements TestModelI18nValue {

    @I18N("com.acs.commmons.test")
    private String validI18nField;

    @I18N
    private String invalidI18nField;

    @I18N
    private String anotherValidI18nField;

    @Inject
    private String injectField;


    @Override
    public String getValidI18nField() {
        return validI18nField;
    }

    @Override
    public String getInvalidI18nField() {
        return invalidI18nField;
    }

    @Override
    public String getAnotherValidI18nField() {
        return anotherValidI18nField;
    }

    @Override
    public String getInjectField() {
        return injectField;
    }
}
