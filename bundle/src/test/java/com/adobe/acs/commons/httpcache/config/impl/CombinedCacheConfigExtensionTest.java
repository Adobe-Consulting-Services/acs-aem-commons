/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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
package com.adobe.acs.commons.httpcache.config.impl;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.config.HttpCacheConfigExtension;
import com.adobe.acs.commons.httpcache.exception.HttpCacheRepositoryAccessException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.osgi.framework.Constants;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

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
        when(extension3.accepts(request, config)).thenReturn(true);
        when(extension4.accepts(request, config)).thenReturn(true);
        
        when(ocd.httpcache_config_extension_combiner_require_all_to_accept()).thenReturn(true);

        underTest.activate(ocd);
    }

    @Test
    public void test() throws HttpCacheRepositoryAccessException {
        underTest.bindCacheConfigExtension(extension1, mockHttpCacheConfigExtension(1L, 1,"extension1"));
        underTest.bindCacheConfigExtension(extension2, mockHttpCacheConfigExtension(2L, 2, "extension2"));

        boolean accepts = underTest.accepts(request, config);

        assertFalse(accepts);

        verify(extension1, times(1)).accepts(request, config);
        verify(extension2, never()).accepts(request, config);

        underTest.unbindCacheConfigExtension(extension1, mockHttpCacheConfigExtension(1L, 1,"extension1"));
        underTest.unbindCacheConfigExtension(extension2, mockHttpCacheConfigExtension(2L, 2,"extension2"));

    }

    @Test
    public void test_accepts() throws HttpCacheRepositoryAccessException {
        underTest.bindCacheConfigExtension(extension3, mockHttpCacheConfigExtension(1L, 1,"extension3"));
        underTest.bindCacheConfigExtension(extension4, mockHttpCacheConfigExtension(2L, 2,"extension4"));

        boolean accepts = underTest.accepts(request, config);

        assertTrue(accepts);

        verify(extension3, times(1)).accepts(request, config);
        verify(extension4, times(1)).accepts(request, config);

        underTest.unbindCacheConfigExtension(extension3, mockHttpCacheConfigExtension(1L, 1, "extension3"));
        underTest.unbindCacheConfigExtension(extension4, mockHttpCacheConfigExtension(2L, 2,"extension4"));


    }

    private Map<String, Object> mockHttpCacheConfigExtension(long id, int rank, String pid) {

        Map<String, Object> properties = new HashMap<>();

        properties.put(Constants.SERVICE_RANKING, rank);
        properties.put(Constants.SERVICE_ID, id);
        properties.put(Constants.SERVICE_PID, pid);
        return properties;

    }

}
