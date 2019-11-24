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
import com.adobe.acs.commons.httpcache.config.impl.keys.CombinedCacheKey;
import com.adobe.acs.commons.httpcache.exception.HttpCacheKeyCreationException;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.keys.CacheKeyFactory;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.osgi.framework.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CombinedCacheKeyFactoryTest {


    public static final String RESOURCE_PATH = "/content/acs-commons/test/jcr:content/component";
    public static final String URI = "/content/acs-commons/test/jcr:content/component.html";


    @Mock
    private CacheKeyFactory factory1;
    @Mock
    private CacheKeyFactory factory2;
    @Mock
    private CacheKeyFactory factory3;
    @Mock
    private CacheKeyFactory factory4;
    @Mock
    private SlingHttpServletRequest request;
    @Mock
    private Resource resource;
    @Mock
    private HttpCacheConfig config;
    @Mock
    private CacheKey key1;
    @Mock
    private CacheKey key2;
    @Mock
    private CacheKey key3;
    @Mock
    private CacheKey key4;
    @Mock
    private CombinedCacheKeyFactory.Config ocd;

    private final CombinedCacheKeyFactory underTest = new CombinedCacheKeyFactory();

    private List<CacheKeyFactory> cacheKeyFactoryList = new ArrayList();

    @Before
    public void init() throws HttpCacheKeyCreationException {

        when(factory1.build(anyString(), any(HttpCacheConfig.class))).thenReturn(key1);
        when(factory2.build(anyString(), any(HttpCacheConfig.class))).thenReturn(key2);
        when(factory3.build(anyString(), any(HttpCacheConfig.class))).thenReturn(key3);
        when(factory4.build(anyString(), any(HttpCacheConfig.class))).thenReturn(key4);

        when(factory3.build(request, config)).thenReturn(key3);
        when(factory4.build(request, config)).thenReturn(key4);

        when(request.getResource()).thenReturn(resource);
        when(request.getRequestURI()).thenReturn(URI);

        when(resource.getPath()).thenReturn(RESOURCE_PATH);
        underTest.activate(ocd);
        cacheKeyFactoryList.clear();
    }

    @Test
    public void test_no_match() throws HttpCacheKeyCreationException {
        underTest.bindCacheKeyFactory(factory1, mockHttpCacheConfigExtension(1L, 1));
        underTest.bindCacheKeyFactory(factory2, mockHttpCacheConfigExtension(2L, 2));

        cacheKeyFactoryList.add(factory3);
        cacheKeyFactoryList.add(factory4);

        CombinedCacheKey chain = new CombinedCacheKey(request, config, cacheKeyFactoryList);
        boolean match = underTest.doesKeyMatchConfig(chain, config);

        assertFalse(match);

        underTest.unbindCacheKeyFactory(factory1, mockHttpCacheConfigExtension(1L, 1));
        underTest.unbindCacheKeyFactory(factory2, mockHttpCacheConfigExtension(2L, 2));

    }

    @Test
    public void test_match() throws HttpCacheKeyCreationException {
        underTest.bindCacheKeyFactory(factory3, mockHttpCacheConfigExtension(1L, 1));
        underTest.bindCacheKeyFactory(factory4, mockHttpCacheConfigExtension(2L, 2));

        cacheKeyFactoryList.add(factory3);
        cacheKeyFactoryList.add(factory4);

        CombinedCacheKey chain = new CombinedCacheKey(request, config, cacheKeyFactoryList);
        boolean match = underTest.doesKeyMatchConfig(chain, config);

        assertTrue(match);
        underTest.unbindCacheKeyFactory(factory3, mockHttpCacheConfigExtension(1L, 1));
        underTest.unbindCacheKeyFactory(factory4, mockHttpCacheConfigExtension(2L, 2));


    }

    private Map<String, Object> mockHttpCacheConfigExtension(long pid, int rank) {

        Map<String, Object> properties = new HashMap<>();

        properties.put(Constants.SERVICE_RANKING, rank);
        properties.put(Constants.SERVICE_ID, pid);

        return properties;

    }

}
