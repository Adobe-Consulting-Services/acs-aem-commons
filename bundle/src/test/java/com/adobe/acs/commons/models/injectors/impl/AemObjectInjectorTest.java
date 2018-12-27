/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2014 Adobe
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
package com.adobe.acs.commons.models.injectors.impl;

import com.adobe.acs.commons.models.injectors.annotation.AemObject;
import com.adobe.acs.commons.models.injectors.annotation.impl.AemObjectAnnotationProcessorFactory;
import com.adobe.acs.commons.models.injectors.impl.model.TestResourceModel;
import com.adobe.acs.commons.models.injectors.impl.model.impl.TestResourceModelImpl;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.spi.Injector;
import org.apache.sling.models.spi.injectorspecific.StaticInjectAnnotationProcessorFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

@RunWith(MockitoJUnitRunner.class)
public class AemObjectInjectorTest {

    private static final String CURRENT_PAGE_PATH = "/content/we-retail/language-masters/en/experience";
    private static final String RESOURCE_PAGE_PATH = "/content/we-retail/language-masters/en/experience/arctic-surfing-in-lofoten";
    @Rule
    public final AemContext context = InjectorAEMContext.provide();

    @InjectMocks
    private AemObjectInjector injector;
    private TestResourceModel adapted;

    @Before
    public void setUp() throws Exception {
        context.currentPage(CURRENT_PAGE_PATH);
        context.currentResource(RESOURCE_PAGE_PATH + "/jcr:content/root");

        context.registerService(Injector.class, injector);
        context.registerService(StaticInjectAnnotationProcessorFactory.class, new AemObjectAnnotationProcessorFactory());
        context.addModelsForClasses(TestResourceModelImpl.class);

        SlingHttpServletRequest adaptable = context.request();
        adapted = adaptable.adaptTo(TestResourceModel.class);
    }

    @Test
    public void test_name() {
        assertEquals(AemObject.SOURCE, injector.getName());
    }

    @Test
    public void test() {
        assertNotNull(adapted);

        assertSame(context.pageManager(), adapted.getPageManager());
        assertSame(context.resourceResolver(), adapted.getResourceResolver());
        assertEquals(RESOURCE_PAGE_PATH, adapted.getResourcePage().getPath());
        assertEquals(CURRENT_PAGE_PATH, adapted.getCurrentPage().getPath());

        assertNotNull(adapted.getCurrentStyle());
        assertNotNull(adapted.getDesigner());
        assertNotNull(adapted.getComponentContext());
        assertNotNull(adapted.getResourceDesign());
    }

}
