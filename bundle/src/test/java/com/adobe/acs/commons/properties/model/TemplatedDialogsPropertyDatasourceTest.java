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
package com.adobe.acs.commons.properties.model;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.servlet.MockRequestPathInfo;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import io.wcm.testing.mock.aem.junit.AemContext;

import static com.adobe.acs.commons.properties.TemplatedDialogTestUtil.defaultService;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
public class TemplatedDialogsPropertyDatasourceTest {
    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.JCR_OAK);

    private MockSlingHttpServletRequest request;

    @Before
    public void setup() {
        context.load().json(getClass().getResourceAsStream("TemplatedDialogsPropertyDatasource.json"), "/content/we-retail/language-masters/en/experience");
        defaultService(context);
        context.addModelsForClasses(TemplatedDialogsPropertyDatasource.class);

        request = new MockSlingHttpServletRequest(context.resourceResolver(), context.bundleContext());
    }

    @Test
    public void testValidJson() {
        MockRequestPathInfo mockRequestPathInfo = (MockRequestPathInfo) request.getRequestPathInfo();
        mockRequestPathInfo.setSuffix("/content/we-retail/language-masters/en/experience/arctic-surfing-in-lofoten/jcr:content/root/responsivegrid/text");
        TemplatedDialogsPropertyDatasource templatedDialogsPropertyDatasource = request.adaptTo(TemplatedDialogsPropertyDatasource.class);
        assertNotNull(templatedDialogsPropertyDatasource);
        String jsonString = templatedDialogsPropertyDatasource.getJson();
        String expected = StringEscapeUtils.escapeHtml4("{\"inherited_page_properties.inheritedProperty\":\"inheritedValue\",\"page_properties.jcr:primaryType\":\"cq:PageContent\",\"page_properties.jcr:title\":\"Arctic Surfing In Lofoten\",\"page_properties.sling:resourceType\":\"weretail/components/structure/page\",\"page_properties.jcr:createdBy\":\"admin\"}");
        assertEquals(expected, jsonString);
    }

    @Test
    public void testInvalidResource() {
        MockRequestPathInfo mockRequestPathInfo = (MockRequestPathInfo) request.getRequestPathInfo();
        mockRequestPathInfo.setSuffix("/non-existing");
        TemplatedDialogsPropertyDatasource templatedDialogsPropertyDatasource = request.adaptTo(TemplatedDialogsPropertyDatasource.class);
        assertNotNull(templatedDialogsPropertyDatasource);
        String jsonString = templatedDialogsPropertyDatasource.getJson();
        String expected = "{}";
        assertEquals(expected, jsonString);
    }

}
