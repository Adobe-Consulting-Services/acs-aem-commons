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

import com.adobe.acs.commons.models.injectors.annotation.impl.JsonValueMapValueAnnotationProcessorFactory;
import com.adobe.acs.commons.models.injectors.impl.model.TestJsonObjectInjection;
import com.adobe.acs.commons.models.injectors.impl.model.impl.TestJsonObjectInjectionImpl;
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

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;


@RunWith(MockitoJUnitRunner.class)
public class JsonValueMapValueInjectorTest {

    @Rule
    public final AemContext context = InjectorAEMContext.provide();

    @InjectMocks
    private JsonValueMapValueInjector jsonValueMapValueInjector;
    private TestJsonObjectInjection adapted;

    @Before
    public void setUp() throws Exception {
        context.currentResource("/content/we-retail/language-masters/en/jcr:content/jsonTest");
        context.registerService(Injector.class, jsonValueMapValueInjector);
        context.registerService(StaticInjectAnnotationProcessorFactory.class, new JsonValueMapValueAnnotationProcessorFactory());
        context.addModelsForClasses(TestJsonObjectInjectionImpl.class);

        Resource adaptable = context.request().getResource();
        adapted = adaptable.adaptTo(TestJsonObjectInjection.class);
    }

    @Test
    public void test_single() {
        assertNotNull(adapted);
        assertEquals("value1", adapted.getTestJsonObject().getProperty1());
        assertEquals("value2", adapted.getTestJsonObject().getProperty2());
        assertEquals(22, adapted.getTestJsonObject().getProperty3());
    }

    @Test
    public void test_collections() {
        assertEquals(4, adapted.getTestJsonObjectArray().length);
        assertEquals(4, adapted.getTestJsonObjectList().size());
        assertEquals(3, adapted.getTestJsonObjectSet().size());
    }

    @Test
    public void test_empty_collections_are_null() {
        assertNull(adapted.getTestJsonObjectArrayEmpty());
        assertNull(adapted.getTestJsonObjectSetEmpty());
        assertNull(adapted.getTestJsonObjectListEmpty());
    }
}
