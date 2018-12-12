package com.adobe.acs.commons.httpcache.store.caffeine.impl;

import com.adobe.acs.commons.httpcache.store.HttpCacheStore;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
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

    private long value_ttl = 30L;
    private long value_maxsize = 20L;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        systemUnderTest = new CaffeineStoreRegisterer();

        properties.put(CaffeineStoreRegisterer.PROP_TTL, value_ttl);
        properties.put(CaffeineStoreRegisterer.PROP_MAX_SIZE_IN_MB, value_maxsize);
    }

    @Test
    public void test_register(){

        systemUnderTest.activate(bundleContext, properties);
        verify(bundleContext, atLeastOnce()).registerService(any(String[].class), any(Object.class), argumentCaptor.capture());
        assertEquals(HttpCacheStore.VALUE_CAFFEINE_MEMORY_STORE_TYPE,argumentCaptor.getValue().get(HttpCacheStore.KEY_CACHE_STORE_TYPE));
    }

    @Test
    public void test_class_not_found() throws Exception {

        PowerMockito.whenNew(CaffeineMemHttpCacheStoreImpl.class).withArguments(value_ttl, value_maxsize).thenThrow(new NoClassDefFoundError("Caffeine lib not loaded!"));
        systemUnderTest.activate(bundleContext, properties);
        verify(bundleContext, never()).registerService(any(String[].class), any(Object.class), argumentCaptor.capture());
    }

}
