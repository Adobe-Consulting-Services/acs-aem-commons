/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
package com.adobe.acs.commons.dam.impl;

import org.apache.sling.commons.testing.sling.MockSlingHttpServletRequest;
import org.apache.sling.commons.testing.sling.MockSlingHttpServletResponse;
import org.junit.Rule;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import io.wcm.testing.mock.aem.junit.AemContext;

import java.util.Collections;

public class CustomComponentActivatorListServletTest {

    private static final String DEFAULT_RESULT = "{\"components\":[{\"propertyName\":\"xmpMM:History\",\"componentPath\":\"/apps/acs-commons/dam/content/admin/history\"},"
            + "{\"propertyName\":\"xmpTPg:Fonts\",\"componentPath\":\"/apps/acs-commons/dam/content/admin/fonts\"},"
            + "{\"propertyName\":\"xmpTPg:Colorants\",\"componentPath\":\"/apps/acs-commons/dam/content/admin/color-swatches\"},"
            + "{\"propertyName\":\"location\",\"componentPath\":\"/apps/acs-commons/dam/content/admin/asset-location-map\"}]}";
    
    @Rule
    public AemContext context = new AemContext();

    @Test
    public void testDefault() throws Exception {
        CustomComponentActivatorListServlet servlet = new CustomComponentActivatorListServlet();
        context.registerInjectActivateService(servlet, Collections.emptyMap());
        MockSlingHttpServletRequest request = new MockSlingHttpServletRequest(null, null, null, null, null);
        MockSlingHttpServletResponse response = new MockSlingHttpServletResponse();
        servlet.doGet(request, response);
        String result = response.getOutput().toString();
        JSONAssert.assertEquals(DEFAULT_RESULT, result, JSONCompareMode.NON_EXTENSIBLE);
    }


    @Test
    public void testCustom() throws Exception {
        CustomComponentActivatorListServlet servlet = new CustomComponentActivatorListServlet();
        context.registerInjectActivateService(servlet, Collections.singletonMap("components", new String[] { "test=my/test/component"}));
        MockSlingHttpServletRequest request = new MockSlingHttpServletRequest(null, null, null, null, null);
        MockSlingHttpServletResponse response = new MockSlingHttpServletResponse();
        servlet.doGet(request, response);
        String result = response.getOutput().toString();
        JSONAssert.assertEquals("{\"components\":[{\"propertyName\":\"test\",\"componentPath\":\"my/test/component\"}]}", result, JSONCompareMode.NON_EXTENSIBLE);
    }
}
