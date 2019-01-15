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
import com.adobe.acs.commons.httpcache.config.impl.keys.KeyValueHttpCacheKey;
import com.adobe.acs.commons.httpcache.config.impl.keys.helper.KeyValueMapWrapper;
import com.adobe.acs.commons.httpcache.exception.HttpCacheKeyCreationException;
import com.adobe.acs.commons.httpcache.exception.HttpCacheRepositoryAccessException;
import com.adobe.cq.commerce.common.ValueMapDecorator;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ValueMapValueHttpCacheConfigExtensionTest {

    public static final String REQUEST_URI = "/check-in.html";
    private static final String REQUEST_RESOURCE_PATH = "/content/eurowings/de/check-in";

    @Mock
    private SlingHttpServletRequest request;
    @Mock
    private SlingHttpServletRequest emptyRequest;

    @Mock
    private HttpCacheConfig validCacheKeyConfig;

    @Mock
    private Resource requestResource;

    @Mock
    private Resource emptyResource;

    KeyValueConfig configA = new KeyValueConfig(){
        @Override
        public Class<? extends Annotation> annotationType() {
            return null;
        }

        @Override
        public String configName() {
            return "unique-config2";
        }

        @Override
        public String[] allowedKeys() {
            return new String[]{"present-valuemap-key", "non-present-valuemap"};
        }

        @Override
        public String[] allowedValues() {
            return new String[0];
        }

        @Override
        public boolean emptyAllowed() {
            return false;
        }
    };

    KeyValueConfig configB = new KeyValueConfig(){
        @Override
        public Class<? extends Annotation> annotationType() {
            return null;
        }

        @Override
        public String configName() {
            return "unique-config";
        }

        @Override
        public String[] allowedKeys() {
            return new String[]{"present-valuemap-key"};
        }

        @Override
        public String[] allowedValues() {
            return new String[]{"present-valuemap-key=present-valuemap-value|value2"};
        }

        @Override
        public boolean emptyAllowed() {
            return true;
        }
    };

    private final Map<String,Object> properties = new HashMap<>();
    private final ValueMapValueHttpCacheConfigExtension systemUnderTest = new ValueMapValueHttpCacheConfigExtension();


    @Before
    public void setUp(){

        properties.put("present-valuemap-key","present-valuemap-value");

        when(requestResource.getValueMap()).thenAnswer((Answer<ValueMapDecorator>) invocationOnMock -> new ValueMapDecorator(properties));
        when(emptyResource.getValueMap()).thenReturn(ValueMap.EMPTY);

        when(validCacheKeyConfig.getAuthenticationRequirement()).thenReturn("anonymous");
        when(request.getRequestURI()).thenReturn(REQUEST_URI);
        when(request.getResource()).thenReturn(requestResource);
        when(emptyRequest.getResource()).thenReturn(emptyResource);
        when(requestResource.getPath()).thenReturn(REQUEST_RESOURCE_PATH);
    }


    @Test
    public void test() throws HttpCacheRepositoryAccessException, HttpCacheKeyCreationException {

        systemUnderTest.activate(configA);
        assertTrue(systemUnderTest.accepts(request, null));
        assertFalse(systemUnderTest.accepts(emptyRequest, null));

        KeyValueHttpCacheKey cacheKey = (KeyValueHttpCacheKey) systemUnderTest.build(request, validCacheKeyConfig);
        KeyValueMapWrapper map = cacheKey.getKeyValueMap();

        assertTrue(map.containsKey("present-valuemap-key"));
        assertEquals(StringUtils.EMPTY, map.get("present-valuemap-key"));

        assertFalse(map.containsKey("non-present-valuemap"));

        assertEquals("/check-in.html[ValueMap:present-valuemap-key][AUTH_REQ:anonymous]", cacheKey.toString());

    }

    @Test
    public void test_with_specific_values() throws HttpCacheRepositoryAccessException, HttpCacheKeyCreationException {

        systemUnderTest.activate(configB);
        assertTrue(systemUnderTest.accepts(request, null));

        KeyValueHttpCacheKey cacheKey = (KeyValueHttpCacheKey) systemUnderTest.build(request, validCacheKeyConfig);
        KeyValueMapWrapper map = cacheKey.getKeyValueMap();

        assertTrue(map.containsKey("present-valuemap-key"));
        assertEquals("present-valuemap-value", map.get("present-valuemap-key"));

        assertFalse(map.containsKey("non-present-valuemap"));

        assertEquals("/check-in.html[ValueMap:present-valuemap-key=present-valuemap-value][AUTH_REQ:anonymous]", cacheKey.toString());

    }

}
