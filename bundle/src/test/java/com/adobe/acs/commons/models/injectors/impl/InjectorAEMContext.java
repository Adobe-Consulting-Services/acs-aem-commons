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
package com.adobe.acs.commons.models.injectors.impl;

import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

import java.io.InputStream;


public class InjectorAEMContext {

    private static AemContext CONTEXT = new AemContext(new InjectorAEMContext.SetUpCallback());

    public static AemContext provide() {
        return CONTEXT;
    }

    private static final class SetUpCallback implements AemContextCallback {

        @Override
        public void execute(AemContext context) {
            // application-specific services for unit tests
            context.registerService(new AemObjectInjector());
            // import sample content
            InputStream inputStream = getClass().getResourceAsStream("we-retail-pages.json");
            context.load().json(inputStream, "/content/we-retail/language-masters/en");

            // set default current page
            context.currentPage("/content/we-retail/language-masters/en/about-us");

            //set the current resource page
            context.currentResource("/content/we-retail/language-masters/en/about-us");


        }
    }

}
