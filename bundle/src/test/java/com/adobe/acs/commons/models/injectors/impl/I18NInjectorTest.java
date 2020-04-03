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


import com.adobe.acs.commons.i18n.I18nProvider;
import com.adobe.acs.commons.models.injectors.annotation.I18N;
import com.adobe.acs.commons.models.injectors.annotation.impl.I18NAnnotationProcessorFactory;
import com.adobe.acs.commons.models.injectors.impl.model.TestModelI18nValue;
import com.adobe.acs.commons.models.injectors.impl.model.impl.TestModelI18nValueImpl;
import com.day.cq.i18n.I18n;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.adapter.Adaptable;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.spi.Injector;
import org.apache.sling.models.spi.injectorspecific.StaticInjectAnnotationProcessorFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class I18NInjectorTest {

    @Rule
    public final AemContext context = InjectorAEMContext.provide();

    @Mock
    private I18nProvider i18nService;

    @InjectMocks
    private final I18nInjector i18nInjector = new I18nInjector();

    private SlingHttpServletRequest slingHttpServletRequest;

    @Mock
    private I18n i18n;

    @Before
    public void setUp() throws Exception {

        context.currentResource("/content/we-retail/language-masters/en/jcr:content/root");
        slingHttpServletRequest = context.request();

        context.registerService(Injector.class, i18nInjector);
        context.registerService(StaticInjectAnnotationProcessorFactory.class, new I18NAnnotationProcessorFactory());
        context.addModelsForClasses(TestModelI18nValueImpl.class);
        when(i18nService.translate("com.acs.commmons.test", context.currentResource())).thenReturn("Translated from english");
        when(i18nService.translate("anotherValidI18nField", context.currentResource())).thenReturn("FromNameValue");

        when(i18nService.translate("com.acs.commmons.test", context.request())).thenReturn("Translated from english");
        when(i18nService.translate("anotherValidI18nField", context.request())).thenReturn("FromNameValue");

        when(i18nService.i18n(context.request())).thenReturn(i18n);
        when(i18nService.i18n(context.currentResource())).thenReturn(i18n);

    }

    @Test
    public void test_get_name() {
        assertEquals(I18N.SOURCE, i18nInjector.getName());
    }

    @Test
    public void test_from_resource() {
        Adaptable adaptable = slingHttpServletRequest.getResource();
        testAdaptable(adaptable);
    }

    @Test
    public void test_from_request() {
        Adaptable adaptable = slingHttpServletRequest;
        testAdaptable(adaptable);
    }


    @Test
    public void createAnnotationProcessor() {
    }

    private void testAdaptable(Adaptable adaptable) {
        TestModelI18nValue adapted = adaptable.adaptTo(TestModelI18nValue.class);
        assertNotNull(adapted);
        assertEquals("Translated from english", adapted.getValidI18nField());
        assertEquals("FromNameValue", adapted.getAnotherValidI18nField());
        assertNull("we should skip javax.Inject", adapted.getInjectField());
        assertSame(i18n, adapted.getI18n());

        if (adaptable instanceof Resource) {
            verify(i18nService, times(1)).i18n((Resource) adaptable);
        } else if (adaptable instanceof HttpServletRequest) {
            verify(i18nService, times(1)).i18n((HttpServletRequest) adaptable);
        }
    }
}
