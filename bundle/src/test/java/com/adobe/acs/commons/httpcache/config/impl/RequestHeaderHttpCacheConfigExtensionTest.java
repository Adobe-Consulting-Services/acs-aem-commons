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
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.collections.IteratorUtils.asEnumeration;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RequestHeaderHttpCacheConfigExtensionTest {

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

    RequestHeaderHttpCacheConfigExtension.Config configA = new RequestHeaderHttpCacheConfigExtension.Config(){
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
            return new String[]{"present-header-key", "non-present-header"};
        }

        @Override
        public String[] allowedValues() {
            return new String[0];
        }

        @Override
        public boolean emptyAllowed() {
            return false;
        }

        @Override
        public String webconsole_configurationFactory_nameHint() {
            return null;
        }
    };

    RequestHeaderHttpCacheConfigExtension.Config configB = new RequestHeaderHttpCacheConfigExtension.Config(){
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
            return new String[]{"present-header-key"};
        }

        @Override
        public String[] allowedValues() {
            return new String[]{"present-header-key=present-header-value|value2"};
        }

        @Override
        public boolean emptyAllowed() {
            return true;
        }

        @Override
        public String webconsole_configurationFactory_nameHint() {
            return null;
        }
    };

    private final Map<String,String> headerValueMap = new HashMap<>();
    private final RequestHeaderHttpCacheConfigExtension systemUnderTest = new RequestHeaderHttpCacheConfigExtension();


    @Before
    public void setUp(){

        headerValueMap.put("present-header-key","present-header-value");

        when(request.getHeaderNames()).thenAnswer((Answer<Enumeration<String>>) invocationOnMock -> asEnumeration(headerValueMap.keySet().iterator()));
        when(emptyRequest.getHeaderNames()).thenAnswer((Answer<Enumeration<String>>) invocationOnMock -> asEnumeration(Collections.emptyIterator()));

        when(request.getHeader(anyString())).thenAnswer((Answer<String>) invocationOnMock -> {
            String key = invocationOnMock.getArguments()[0].toString();
            return headerValueMap.get(key);
        });

        when(validCacheKeyConfig.getAuthenticationRequirement()).thenReturn("anonymous");
        when(request.getRequestURI()).thenReturn(REQUEST_URI);
        when(request.getResource()).thenReturn(requestResource);
        when(requestResource.getPath()).thenReturn(REQUEST_RESOURCE_PATH);
    }


    @Test
    public void test() throws HttpCacheRepositoryAccessException, HttpCacheKeyCreationException {

        systemUnderTest.activate(configA);
        assertTrue(systemUnderTest.accepts(request, null));
        assertFalse(systemUnderTest.accepts(emptyRequest, null));

        KeyValueHttpCacheKey cacheKey = (KeyValueHttpCacheKey) systemUnderTest.build(request, validCacheKeyConfig);
        KeyValueMapWrapper map = cacheKey.getKeyValueMap();

        assertTrue(map.containsKey("present-header-key"));
        assertEquals(StringUtils.EMPTY, map.get("present-header-key"));

        assertFalse(map.containsKey("non-present-header"));

        assertEquals("/check-in.html[RequestHeaders:present-header-key][AUTH_REQ:anonymous]", cacheKey.toString());

    }

    @Test
    public void test_with_specific_values() throws HttpCacheRepositoryAccessException, HttpCacheKeyCreationException {

        systemUnderTest.activate(configB);
        assertTrue(systemUnderTest.accepts(request, null));

        KeyValueHttpCacheKey cacheKey = (KeyValueHttpCacheKey) systemUnderTest.build(request, validCacheKeyConfig);
        KeyValueMapWrapper map = cacheKey.getKeyValueMap();

        assertTrue(map.containsKey("present-header-key"));
        assertEquals("present-header-value", map.get("present-header-key"));

        assertFalse(map.containsKey("non-present-header"));

        assertEquals("/check-in.html[RequestHeaders:present-header-key=present-header-value][AUTH_REQ:anonymous]", cacheKey.toString());

    }




}
