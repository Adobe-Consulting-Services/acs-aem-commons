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
package com.adobe.acs.commons.i18n.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.internal.verification.VerificationModeFactory.noMoreInteractions;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.management.NotCompliantMBeanException;
import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.i18n.ResourceBundleProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Constants;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.adobe.acs.commons.models.injectors.impl.InjectorUtils;
import com.day.cq.i18n.I18n;
import com.day.cq.wcm.api.Page;

@PrepareForTest({
        I18n.class,
        I18nProviderImpl.class,
        InjectorUtils.class
})
@RunWith(PowerMockRunner.class)
public class I18nProviderImplTest {

    private static final String I18N_KEY = "i18nKey";

    private final I18nProviderImpl i18nProvider = new I18nProviderImpl();
    private final HashMap<String, Object> props = new HashMap<>();

    private final Map<String,String> i18nMap = new HashMap<>();

    @Mock
    private Config config;

    @Mock
    private ResourceBundleProvider resourceBundleProvider;

    @Mock
    private ResourceBundle resourceBundle;

    @Mock
    private Resource resource;

    @Mock
    private SlingHttpServletRequest request;

    @Mock
    private Page resourcePage;

    public I18nProviderImplTest() throws NotCompliantMBeanException {
    }

    @Before
    public void setUp() {
        props.clear();
        props.put(Constants.SERVICE_ID, 11L);
        props.put(Constants.SERVICE_RANKING, 11);
        i18nProvider.bindResourceBundleProvider(resourceBundleProvider, props);
        when(resourceBundleProvider.getResourceBundle(Locale.US)).thenReturn(resourceBundle);
        when(resource.getPath()).thenReturn("/some/path");

        i18nMap.put(I18N_KEY, "Translated from English!");

        PowerMockito.mockStatic(I18n.class);
        when(I18n.get(any(HttpServletRequest.class), anyString())).thenAnswer(
                (Answer<String>) invocationOnMock -> i18nMap.get(invocationOnMock.getArguments()[1])
        );

        when(I18n.get(any(ResourceBundle.class), anyString())).thenAnswer(
                (Answer<String>) invocationOnMock -> i18nMap.get(invocationOnMock.getArguments()[1])
        );


        PowerMockito.mockStatic(InjectorUtils.class);
        when(InjectorUtils.getResourcePage(resource)).thenReturn(resourcePage);
        when(resourcePage.getLanguage(false)).thenReturn(Locale.US);

        when(config.getTtl()).thenReturn(10L);
        when(config.maxSizeCount()).thenReturn(10L);

        i18nProvider.activate(config);
    }

    @After
    public void tearDown() {
        i18nProvider.unbindResourceBundleProvider(resourceBundleProvider, props);
    }

    @Test
    public void test_get_bytes_length() {
        assertEquals(0L, i18nProvider.getBytesLength(null));
    }

    @Test
    public void test_secondary_activate() {
        when(config.getTtl()).thenReturn(Config.DEFAULT_TTL);
        when(config.maxSizeCount()).thenReturn(Config.DEFAULT_MAX_SIZE_IN_MB);

        i18nProvider.activate(config);
    }

    @Test
    public void test_translate_resource() throws Exception {
        final I18n mocked = mock(I18n.class);
        when(mocked.get(I18N_KEY)).thenReturn("Translated from English!");

        PowerMockito.whenNew(I18n.class)
                .withParameterTypes(ResourceBundle.class)
                .withArguments(resourceBundle)
                .thenReturn(mocked);

        when(InjectorUtils.getResourcePage(resource)).thenReturn(resourcePage);

        when(resourceBundleProvider.getResourceBundle(Locale.US)).thenReturn(resourceBundle);

        final String translated = i18nProvider.translate(I18N_KEY, resource);
        assertEquals("Translated from English!", translated);
    }

    @Test
    public void test_translate_request() {
        final String translated = i18nProvider.translate(I18N_KEY, request);
        assertEquals("Translated from English!", translated);

        PowerMockito.verifyStatic(I18n.class, times(1));
        I18n.get(request, I18N_KEY);
    }

    @Test
    public void test_translate_locale() {
        final String translated = i18nProvider.translate(I18N_KEY, Locale.US);
        assertEquals("Translated from English!", translated);

        PowerMockito.verifyStatic(I18n.class, times(1));
        I18n.get(resourceBundle, I18N_KEY);
    }

    @Test
    public void test_i18n_request() throws Exception {
        final I18n mocked = mock(I18n.class);

        PowerMockito.whenNew(I18n.class)
                .withParameterTypes(HttpServletRequest.class)
                .withArguments(request)
                .thenReturn(mocked);


        final I18n actual = i18nProvider.i18n(request);
        assertSame(mocked, actual);
    }

    @Test
    public void test_i18n_locale() throws Exception {
        final I18n mocked = mock(I18n.class);

        PowerMockito.whenNew(I18n.class)
                .withParameterTypes(ResourceBundle.class)
                .withArguments(resourceBundle)
                .thenReturn(mocked);


        final I18n actual = i18nProvider.i18n(Locale.US);
        assertSame(mocked, actual);
    }

    @Test
    public void test_i18n_resource() throws Exception {
        final I18n mocked = mock(I18n.class);

        PowerMockito.whenNew(I18n.class)
                .withParameterTypes(ResourceBundle.class)
                .withArguments(resourceBundle)
                .thenReturn(mocked);

        final I18n actual = i18nProvider.i18n(resource);
        assertSame(mocked, actual);
    }


    @Test
    public void test_i18n_resource_withcaching() throws Exception {
        final I18n mocked = mock(I18n.class);

        PowerMockito.whenNew(I18n.class)
                .withParameterTypes(ResourceBundle.class)
                .withArguments(resourceBundle)
                .thenReturn(mocked);


        final I18n actual = i18nProvider.i18n(resource);
        assertSame(mocked, actual);

        PowerMockito.verifyNew(I18n.class, times(1)).withArguments(resourceBundle);

        final I18n cached = i18nProvider.i18n(resource);
        PowerMockito.verifyNew(I18n.class, noMoreInteractions()).withArguments(resourceBundle);

        assertSame(actual, cached);
    }

}
