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

import static java.util.Collections.emptyMap;
import static org.apache.commons.collections.IteratorUtils.asEnumeration;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RequestParameterHttpCacheConfigExtensionTest {

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

    RequestParameterHttpCacheConfigExtension.Config configA = new RequestParameterHttpCacheConfigExtension.Config(){
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
            return new String[]{"present-parameter-key", "non-present-parameter"};
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

    RequestParameterHttpCacheConfigExtension.Config configB = new RequestParameterHttpCacheConfigExtension.Config(){
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
            return new String[]{"present-parameter-key"};
        }

        @Override
        public String[] allowedValues() {
            return new String[]{"present-parameter-key=present-parameter-value|value2"};
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

    private final Map<String,String[]> parameterValueMap = new HashMap<>();
    private final RequestParameterHttpCacheConfigExtension systemUnderTest = new RequestParameterHttpCacheConfigExtension();


    @Before
    public void setUp(){

        parameterValueMap.put("present-parameter-key", new String[]{"present-parameter-value"});

        when(request.getParameterNames()).thenAnswer((Answer<Enumeration<String>>) invocationOnMock -> asEnumeration(parameterValueMap.keySet().iterator()));
        when(emptyRequest.getParameterNames()).thenAnswer((Answer<Enumeration<String>>) invocationOnMock -> asEnumeration(Collections.emptyIterator()));

        when(request.getParameterMap()).thenAnswer((Answer<Map<String, String[]>>) invocationOnMock -> parameterValueMap);
        when(emptyRequest.getParameterMap()).thenAnswer((Answer<Map<String, String[]>>) invocationOnMock -> emptyMap());


        when(request.getParameter(anyString())).thenAnswer((Answer<String>) invocationOnMock -> {
            String key = invocationOnMock.getArguments()[0].toString();
            return parameterValueMap.get(key)[0];
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

        assertTrue(map.containsKey("present-parameter-key[0]"));
        assertEquals(StringUtils.EMPTY, map.get("present-parameter-key[0]"));

        assertFalse(map.containsKey("non-present-parameter"));

        assertEquals("/check-in.html[RequestParameters:present-parameter-key[0]][AUTH_REQ:anonymous]", cacheKey.toString());

    }

    @Test
    public void test_with_specific_values() throws HttpCacheRepositoryAccessException, HttpCacheKeyCreationException {

        systemUnderTest.activate(configB);
        assertTrue(systemUnderTest.accepts(request, null));

        KeyValueHttpCacheKey cacheKey = (KeyValueHttpCacheKey) systemUnderTest.build(request, validCacheKeyConfig);
        KeyValueMapWrapper map = cacheKey.getKeyValueMap();

        assertTrue(map.containsKey("present-parameter-key[0]"));
        assertEquals("present-parameter-value", map.get("present-parameter-key[0]"));

        assertFalse(map.containsKey("non-present-parameter"));

        assertEquals("/check-in.html[RequestParameters:present-parameter-key[0]=present-parameter-value][AUTH_REQ:anonymous]", cacheKey.toString());

    }
}
