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
package com.adobe.acs.commons.httpcache.store.caffeine.impl;

import com.adobe.acs.commons.httpcache.store.HttpCacheStore;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.BundleContext;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        CaffeineStoreRegisterer.class,
        CaffeineMemHttpCacheStoreImpl.class
})
public class CaffeineStoreRegistererTest {

    private CaffeineStoreRegisterer systemUnderTest;


    @Mock
    BundleContext bundleContext;

    @Captor
    ArgumentCaptor<Dictionary<String, ?>> argumentCaptor;
    private Map<String, Object> properties = new HashMap<>();

    private long valueTtl = 30L;
    private long valueMaxSize = 20L;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        systemUnderTest = new CaffeineStoreRegisterer();

        properties.put(Config.PROP_TTL, valueTtl);
        properties.put(Config.PROP_MAX_SIZE_IN_MB, valueMaxSize);
    }

    @Test
    public void test_register(){

        systemUnderTest.activate(bundleContext, properties);
        verify(bundleContext, atLeastOnce()).registerService(any(String[].class), any(Object.class), argumentCaptor.capture());
        assertEquals(HttpCacheStore.VALUE_CAFFEINE_MEMORY_STORE_TYPE,argumentCaptor.getValue().get(HttpCacheStore.KEY_CACHE_STORE_TYPE));
    }

    @Test
    public void test_class_not_found() throws Exception {

        PowerMockito.whenNew(CaffeineMemHttpCacheStoreImpl.class).withArguments(valueTtl, valueMaxSize).thenThrow(new NoClassDefFoundError("Caffeine lib not loaded!"));
        systemUnderTest.activate(bundleContext, properties);
        verify(bundleContext, never()).registerService(any(String[].class), any(Object.class), argumentCaptor.capture());
    }

}
