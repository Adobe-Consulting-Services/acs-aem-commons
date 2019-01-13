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
import com.adobe.acs.commons.httpcache.config.impl.keys.RequestCookieCacheKey;
import com.adobe.acs.commons.httpcache.config.impl.keys.helper.RequestCookieKeyValueMap;
import com.adobe.acs.commons.httpcache.config.impl.keys.helper.RequestCookieKeyValueMapBuilder;
import com.adobe.acs.commons.httpcache.exception.HttpCacheKeyCreationException;
import com.adobe.acs.commons.httpcache.exception.HttpCacheRepositoryAccessException;
import com.google.common.collect.ImmutableSet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.http.Cookie;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RequestCookieCacheExtensionTest {

    public static final String REQUEST_URI = "/check-in.html";
    private static final String REQUEST_RESOURCE_PATH = "/content/eurowings/de/check-in";
    @Mock
    private SlingHttpServletRequest request;

    @Mock
    private SlingHttpServletRequest emptyRequest;

    @Mock
    private Cookie presentCookie;

    @Mock
    private Cookie irrelevantCookie;

    private Cookie[] cookies;
    private Cookie[] emptyCookies;

    RequestCookieCacheExtension.Config configA = new RequestCookieCacheExtension.Config(){
        @Override
        public Class<? extends Annotation> annotationType() {
            return null;
        }

        @Override
        public String configName() {
            return "unique-config2";
        }

        @Override
        public String[] allowedCookieKeys() {
            return new String[]{"present-cookie-key", "non-present-cookie"};
        }

        @Override
        public String[] allowedCookieKeyValues() {
            return new String[0];
        }

        @Override
        public boolean emptyAllowed() {
            return false;
        }
    };

    RequestCookieCacheExtension.Config configB = new RequestCookieCacheExtension.Config(){
        @Override
        public Class<? extends Annotation> annotationType() {
            return null;
        }

        @Override
        public String configName() {
            return "unique-config";
        }

        @Override
        public String[] allowedCookieKeys() {
            return new String[]{"present-cookie-key"};
        }

        @Override
        public String[] allowedCookieKeyValues() {
            return new String[]{"present-cookie-key=present-cookie-value|value2"};
        }

        @Override
        public boolean emptyAllowed() {
            return true;
        }
    };

    @Mock
    private HttpCacheConfig validCacheKeyConfig;

    @Mock
    private Resource requestResource;

    private final RequestCookieCacheExtension systemUnderTest = new RequestCookieCacheExtension();


    @Before
    public void setUp(){

        cookies = new Cookie[]{presentCookie};
        emptyCookies = new Cookie[]{irrelevantCookie};

        when(emptyRequest.getCookies()).thenReturn(emptyCookies);
        when(request.getCookies()).thenReturn(cookies);

        when(irrelevantCookie.getName()).thenReturn("bla");
        when(presentCookie.getName()).thenReturn("present-cookie-key");
        when(presentCookie.getValue()).thenReturn("present-cookie-value");

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

        RequestCookieCacheKey cacheKey = (RequestCookieCacheKey) systemUnderTest.build(request, validCacheKeyConfig);
        RequestCookieKeyValueMap map = cacheKey.getKeyValueMap();

        assertTrue(map.containsKey("present-cookie-key"));
        assertEquals("present-cookie-value", map.get("present-cookie-key"));

        assertFalse(map.containsKey("non-present-cookie"));

        assertEquals("/check-in.html[CookieKeyValues:present-cookie-key=present-cookie-value][AUTH_REQ:anonymous]", cacheKey.toString());

    }

    @Test
    public void test_with_specific_values() throws HttpCacheRepositoryAccessException, HttpCacheKeyCreationException {

        systemUnderTest.activate(configB);
        assertTrue(systemUnderTest.accepts(request, null));

        RequestCookieCacheKey cacheKey = (RequestCookieCacheKey) systemUnderTest.build(request, validCacheKeyConfig);
        RequestCookieKeyValueMap map = cacheKey.getKeyValueMap();

        assertTrue(map.containsKey("present-cookie-key"));
        assertEquals("present-cookie-value", map.get("present-cookie-key"));

        assertFalse(map.containsKey("non-present-cookie"));

        assertEquals("/check-in.html[CookieKeyValues:present-cookie-key=present-cookie-value][AUTH_REQ:anonymous]", cacheKey.toString());

    }

    @Test
    public void testKey() throws HttpCacheRepositoryAccessException, HttpCacheKeyCreationException {
        systemUnderTest.activate(configA);
        assertTrue(systemUnderTest.accepts(request, null));
        assertFalse(systemUnderTest.accepts(emptyRequest, null));

        ImmutableSet<String> allowedKeys = ImmutableSet.of("present-cookie-key", "non-present-cookie-key");
        ImmutableSet<Cookie> presentValues = ImmutableSet.of(presentCookie);

        Map<String, String> cookieKeyValues = new HashMap<>();
        RequestCookieKeyValueMap requestCookieKeyValueMap = new RequestCookieKeyValueMapBuilder(allowedKeys, cookieKeyValues, presentValues).build();

        RequestCookieCacheKey cookieCacheKey = new RequestCookieCacheKey(REQUEST_URI, validCacheKeyConfig, requestCookieKeyValueMap);

        assertTrue(systemUnderTest.doesKeyMatchConfig(cookieCacheKey, validCacheKeyConfig));
    }

}
