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
package com.adobe.acs.commons.models.injectors.impl;

import com.adobe.acs.commons.models.injectors.annotation.HierarchicalPageProperty;
import com.adobe.acs.commons.models.injectors.annotation.impl.HierarchicalPagePropertyAnnotationProcessorFactory;
import com.adobe.acs.commons.models.injectors.impl.model.TestHierarchicalPagePropertiesModel;
import com.adobe.acs.commons.models.injectors.impl.model.impl.TestHierarchicalPagePropertiesModelModelImpl;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.spi.Injector;
import org.apache.sling.models.spi.injectorspecific.StaticInjectAnnotationProcessorFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
public class HierarchicalPagePropertyInjectorTest {

    @Rule
    public final AemContext context = InjectorAEMContext.provide();

    @InjectMocks
    private HierarchicalPagePropertyInjector injector;
    private TestHierarchicalPagePropertiesModel adapted;

    @Before
    public void setUp() throws Exception {
        context.currentPage("/content/we-retail/language-masters/en/experience/arctic-surfing-in-lofoten");
        context.currentResource("/content/we-retail/language-masters/en/experience/arctic-surfing-in-lofoten/jcr:content/root");

        context.registerService(Injector.class, injector);
        context.registerService(StaticInjectAnnotationProcessorFactory.class, new HierarchicalPagePropertyAnnotationProcessorFactory());
        context.addModelsForClasses(TestHierarchicalPagePropertiesModelModelImpl.class);

        Resource adaptable = context.request().getResource();
        adapted = adaptable.adaptTo(TestHierarchicalPagePropertiesModel.class);
    }

    @Test
    public void test_name() {
        assertEquals(HierarchicalPageProperty.SOURCE, injector.getName());
    }

    @Test
    public void test() {
        assertNotNull(adapted);
    }


}
