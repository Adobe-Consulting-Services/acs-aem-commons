package com.adobe.acs.commons.httpcache.config.impl;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.config.HttpCacheConfigExtension;
import com.adobe.acs.commons.httpcache.exception.HttpCacheRepositoryAccessException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Constants;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CombinedCacheConfigExtensionTest {


    @Mock
    private HttpCacheConfigExtension extension1;
    @Mock
    private HttpCacheConfigExtension extension2;
    @Mock
    private HttpCacheConfigExtension extension3;
    @Mock
    private HttpCacheConfigExtension extension4;
    @Mock
    private SlingHttpServletRequest request;
    @Mock
    private HttpCacheConfig config;
    @Mock
    private CombinedCacheConfigExtension.Config ocd;

    private final CombinedCacheConfigExtension underTest = new CombinedCacheConfigExtension();

    @Before
    public void init() throws HttpCacheRepositoryAccessException {

        when(extension1.accepts(request, config)).thenReturn(false);
        when(extension2.accepts(request, config)).thenReturn(false);
        when(extension3.accepts(request, config)).thenReturn(true);
        when(extension4.accepts(request, config)).thenReturn(true);

        underTest.activate(ocd);
    }

    @Test
    public void test() throws HttpCacheRepositoryAccessException {
        underTest.bindCacheConfigExtension(extension1, mockHttpCacheConfigExtension(1L, 1));
        underTest.bindCacheConfigExtension(extension2, mockHttpCacheConfigExtension(2L, 2));

        boolean accepts = underTest.accepts(request, config);

        assertFalse(accepts);

        verify(extension1, times(1)).accepts(request, config);
        verify(extension2, never()).accepts(request, config);

        underTest.unbindCacheConfigExtension(extension1, mockHttpCacheConfigExtension(1L, 1));
        underTest.unbindCacheConfigExtension(extension2, mockHttpCacheConfigExtension(2L, 2));

    }

    @Test
    public void test_accepts() throws HttpCacheRepositoryAccessException {
        underTest.bindCacheConfigExtension(extension3, mockHttpCacheConfigExtension(1L, 1));
        underTest.bindCacheConfigExtension(extension4, mockHttpCacheConfigExtension(2L, 2));

        boolean accepts = underTest.accepts(request, config);

        assertTrue(accepts);

        verify(extension3, times(1)).accepts(request, config);
        verify(extension4, times(1)).accepts(request, config);

        underTest.unbindCacheConfigExtension(extension3, mockHttpCacheConfigExtension(1L, 1));
        underTest.unbindCacheConfigExtension(extension4, mockHttpCacheConfigExtension(2L, 2));


    }

    private Map<String,Object> mockHttpCacheConfigExtension(long pid, int rank){

        Map<String,Object> properties = new HashMap<>();

        properties.put(Constants.SERVICE_RANKING, rank );
        properties.put(Constants.SERVICE_ID, pid);

        return properties;

    }

}
