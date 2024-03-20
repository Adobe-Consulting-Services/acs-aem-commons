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

import com.adobe.acs.commons.models.injectors.annotation.I18N;
import com.adobe.acs.commons.models.injectors.impl.model.TestModelI18nValue;
import com.day.cq.i18n.I18n;
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

    @I18N(value = "com.acs.commmons.test.resource", localeIgnoreContent = true, forceRetrievalFromUnderlyingResource = true)
    private String validI18nFieldResource;

    @I18N
    private String invalidI18nField;

    @I18N
    private String anotherValidI18nField;

    @I18N(localeIgnoreContent = true, forceRetrievalFromUnderlyingResource = true)
    private String anotherValidI18nFieldResource;

    @Inject
    private I18n i18n;

    @I18N(forceRetrievalFromUnderlyingResource = true, localeIgnoreContent = true)
    private I18n alternateI18n;


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

    @Override
    public I18n getI18n() {
        return i18n;
    }

    @Override
    public String getValidI18nFieldResource() {
        return validI18nFieldResource;
    }

    @Override
    public String getAnotherValidI18nFieldResource() {
        return anotherValidI18nFieldResource;
    }

    @Override
    public I18n getAlternateI18n() {
        return alternateI18n;
    }
}
