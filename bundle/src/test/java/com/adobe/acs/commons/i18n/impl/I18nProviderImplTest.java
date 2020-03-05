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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.management.NotCompliantMBeanException;
import javax.management.openmbean.OpenDataException;
import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.i18n.ResourceBundleProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Constants;

import com.adobe.acs.commons.util.impl.exception.CacheMBeanException;
import com.day.cq.i18n.I18n;
import com.day.cq.wcm.api.Page;

@RunWith(MockitoJUnitRunner.class)
public final class I18nProviderImplTest {

    private static final Locale LOCALE = Locale.US;
    private static final String I18N_KEY = "i18nKey";
    private static final String TRANSLATED_FROM_ENGLISH = "Translated from English!";

    private final I18nProviderImpl i18nProvider = spy(new I18nProviderImpl());
    private final HashMap<String, Object> resourceBundleProviderProps = new HashMap<>();

    private final Map<String, String> i18nMap = new HashMap<>();

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
        resourceBundleProviderProps.put(Constants.SERVICE_ID, 11L);
        resourceBundleProviderProps.put(Constants.SERVICE_RANKING, 11);

        when(resourceBundleProvider.getResourceBundle(LOCALE)).thenReturn(resourceBundle);
        when(resource.getPath()).thenReturn("/some/path");

        i18nMap.put(I18N_KEY, TRANSLATED_FROM_ENGLISH);

        final Answer<String> answer = (Answer<String>) invocationOnMock -> i18nMap.get(invocationOnMock.getArguments()[1]);

        doReturn(resourcePage).when(i18nProvider).getResourcePage(resource);

        when(resourcePage.getLanguage(false)).thenReturn(LOCALE);

        when(config.getTtl()).thenReturn(10L);
        when(config.maxSizeCount()).thenReturn(10L);

        i18nProvider.bindResourceBundleProvider(resourceBundleProvider, resourceBundleProviderProps);
        i18nProvider.activate(config);
    }

    @After
    public void tearDown() {
        i18nProvider.unbindResourceBundleProvider(resourceBundleProvider, resourceBundleProviderProps);
        resourceBundleProviderProps.clear();
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
    public void test_translate_resource() {
        final I18n mocked = mock(I18n.class);
        when(mocked.get(I18N_KEY)).thenReturn(TRANSLATED_FROM_ENGLISH);
        doReturn(mocked).when(i18nProvider).i18n(resourceBundle);

        doReturn(resourcePage).when(i18nProvider).getResourcePage(resource);

        when(resourceBundleProvider.getResourceBundle(LOCALE)).thenReturn(resourceBundle);

        final String translated = i18nProvider.translate(I18N_KEY, resource);
        assertEquals(TRANSLATED_FROM_ENGLISH, translated);
    }

    @Test
    public void test_i18n_locale() {
        final I18n mocked = mock(I18n.class);

        doReturn(mocked).when(i18nProvider).i18n(resourceBundle);

        final I18n actual = i18nProvider.i18n(LOCALE);
        assertSame(mocked, actual);
    }

    @Test
    public void test_i18n_resource() {
        final I18n mocked = mock(I18n.class);

        doReturn(mocked).when(i18nProvider).i18n(resourceBundle);

        final I18n actual = i18nProvider.i18n(resource);
        assertSame(mocked, actual);
    }

    @Test
    public void test_i18n_resource_withcaching() {
        final I18n mocked = mock(I18n.class);

        doReturn(mocked).when(i18nProvider).i18n(resourceBundle);

        final I18n actual = i18nProvider.i18n(resource);
        assertSame(mocked, actual);

        final I18n cached = i18nProvider.i18n(resource);

        assertSame(actual, cached);
    }

    @Test
    public void test_translate_resource_null()  {
        doReturn(null).when(i18nProvider).i18n(resource);
        assertNull(i18nProvider.translate(null, resource));
    }

    @Test
    public void test_i18n_getLocaleFromResource_page_null() {
        doReturn(null).when(i18nProvider).getResourcePage(resource);
        assertNotNull(i18nProvider.i18n(resource));
    }

    @Test
    public void test_AbstractGuavaCacheMBean() throws CacheMBeanException, OpenDataException {
        final I18n cacheObj = mock(I18n.class);
        assertNotNull(i18nProvider.getCache());
        assertNotNull(i18nProvider.getBytesLength(cacheObj));
        i18nProvider.addCacheData(resourceBundleProviderProps, cacheObj);
        assertNotNull(i18nProvider.toString(cacheObj));
        assertNotNull(i18nProvider.getCacheEntryType());
    }

}
