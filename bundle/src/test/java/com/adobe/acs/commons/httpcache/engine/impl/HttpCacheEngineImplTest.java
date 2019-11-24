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
package com.adobe.acs.commons.httpcache.engine.impl;

import com.adobe.acs.commons.fam.ThrottledTaskRunner;
import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import com.adobe.acs.commons.httpcache.engine.CacheContent;
import com.adobe.acs.commons.httpcache.engine.HttpCacheServletResponseWrapper;
import com.adobe.acs.commons.httpcache.exception.HttpCacheException;
import com.adobe.acs.commons.httpcache.keys.CacheKey;
import com.adobe.acs.commons.httpcache.store.HttpCacheStore;
import com.adobe.acs.commons.httpcache.store.mem.impl.MemTempSinkImpl;
import com.day.cq.commons.feed.StringResponseWrapper;
import org.apache.commons.collections.map.SingletonMap;
import org.apache.commons.io.IOUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.commons.testing.sling.MockSlingHttpServletRequest;
import org.apache.sling.commons.testing.sling.MockSlingHttpServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.stubbing.Answer;

import javax.management.NotCompliantMBeanException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.adobe.acs.commons.httpcache.engine.impl.HttpCacheEngineImpl.PROP_GLOBAL_RESPONSE_COOKIE_EXCLUSIONS;
import static com.adobe.acs.commons.httpcache.engine.impl.HttpCacheEngineImpl.PROP_GLOBAL_RESPONSE_HEADER_EXCLUSIONS;
import static com.adobe.acs.commons.httpcache.store.HttpCacheStore.VALUE_JCR_CACHE_STORE_TYPE;
import static com.adobe.acs.commons.httpcache.store.HttpCacheStore.VALUE_MEM_CACHE_STORE_TYPE;
import static java.util.Collections.emptyMap;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class HttpCacheEngineImplTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    HttpCacheConfig memCacheConfig;

    @Mock
    HttpCacheConfig jcrCacheConfig;

    @Mock
    HttpCacheStore memCacheStore;

    @Mock
    HttpCacheStore jcrCacheStore;

    @Mock
    ThrottledTaskRunner throttledTaskRunner;

    @InjectMocks
    HttpCacheEngineImpl systemUnderTest;

    @Captor
    ArgumentCaptor<CacheContent> cacheContentCaptor;

    Map<String, Object> sharedMemConfigProps = new HashMap<>();
    Map<String, Object> sharedJcrConfigProps = new HashMap<>();

    @Before
    public void init() throws NotCompliantMBeanException {

        systemUnderTest.activate(Collections.emptyMap());

        sharedMemConfigProps.put(HttpCacheStore.KEY_CACHE_STORE_TYPE, VALUE_MEM_CACHE_STORE_TYPE);
        sharedJcrConfigProps.put(HttpCacheStore.KEY_CACHE_STORE_TYPE, VALUE_JCR_CACHE_STORE_TYPE);
        when(memCacheConfig.isValid()).thenReturn(true);
        when(jcrCacheConfig.isValid()).thenReturn(true);
        when(memCacheConfig.getCacheStoreName()).thenReturn(VALUE_MEM_CACHE_STORE_TYPE);
        when(jcrCacheConfig.getCacheStoreName()).thenReturn(VALUE_JCR_CACHE_STORE_TYPE);
        when(memCacheStore.getStoreType()).thenReturn(VALUE_MEM_CACHE_STORE_TYPE);
        when(jcrCacheStore.getStoreType()).thenReturn(VALUE_JCR_CACHE_STORE_TYPE);

        doAnswer((Answer<Void>) invocationOnMock -> {
            Runnable runnable = invocationOnMock.getArgument(0);
            runnable.run();
            return null;
        }).when(throttledTaskRunner).scheduleWork(any(Runnable.class));

        systemUnderTest.bindHttpCacheConfig(memCacheConfig, sharedMemConfigProps);
        systemUnderTest.bindHttpCacheConfig(jcrCacheConfig, sharedJcrConfigProps);
        systemUnderTest.bindHttpCacheStore(memCacheStore);
        systemUnderTest.bindHttpCacheStore(jcrCacheStore);
    }

    @After
    public void tearDown() {
        systemUnderTest.unbindHttpCacheConfig(memCacheConfig, sharedMemConfigProps);
        systemUnderTest.unbindHttpCacheConfig(jcrCacheConfig, sharedJcrConfigProps);
        systemUnderTest.unbindHttpCacheStore(memCacheStore);
        systemUnderTest.unbindHttpCacheStore(jcrCacheStore);
        systemUnderTest.deactivate(emptyMap());
    }

    @Test
    public void test_get_cache_config() throws HttpCacheException {
        SlingHttpServletRequest request = new MockSlingHttpServletRequest("/content/acs-commons/home", "my-selector", "html", "", "");

        when(jcrCacheConfig.getFilterScope()).thenReturn(HttpCacheConfig.FilterScope.REQUEST);
        when(jcrCacheConfig.accepts(request)).thenReturn(true);
        HttpCacheConfig foundConfig = systemUnderTest.getCacheConfig(request, HttpCacheConfig.FilterScope.REQUEST);
        assertSame(jcrCacheConfig, foundConfig);
    }

    @Test
    public void test_cache_hit() throws HttpCacheException{
        SlingHttpServletRequest request = new MockSlingHttpServletRequest("/content/acs-commons/home", "my-selector", "html", "", "");

        when(jcrCacheConfig.getFilterScope()).thenReturn(HttpCacheConfig.FilterScope.REQUEST);
        when(jcrCacheConfig.accepts(request)).thenReturn(true);
        HttpCacheConfig foundConfig = systemUnderTest.getCacheConfig(request, HttpCacheConfig.FilterScope.REQUEST);
        assertSame(jcrCacheConfig, foundConfig);

        CacheKey mockedCacheKey = mock(CacheKey.class);
        //cacheConfig.buildCacheKey(request)
        when(jcrCacheConfig.buildCacheKey(request)).thenReturn(mockedCacheKey);
        when(jcrCacheStore.contains(mockedCacheKey)).thenReturn(true);

        boolean isHit = systemUnderTest.isCacheHit(request, foundConfig);
        assertTrue(isHit);
    }

    @Test
    public void test_deliver_cache_content() throws HttpCacheException, IOException {
        SlingHttpServletRequest request = new MockSlingHttpServletRequest("/content/acs-commons/home", "my-selector", "html", "", "");

        when(jcrCacheConfig.getFilterScope()).thenReturn(HttpCacheConfig.FilterScope.REQUEST);
        when(jcrCacheConfig.accepts(request)).thenReturn(true);
        HttpCacheConfig foundConfig = systemUnderTest.getCacheConfig(request, HttpCacheConfig.FilterScope.REQUEST);
        assertSame(jcrCacheConfig, foundConfig);

        CacheKey mockedCacheKey = mock(CacheKey.class);
        CacheContent mockedCacheContent = mock(CacheContent.class);

        when(mockedCacheContent.getWriteMethod()).thenReturn(HttpCacheServletResponseWrapper.ResponseWriteMethod.PRINTWRITER);
        when(mockedCacheContent.getInputDataStream()).thenReturn(getClass().getResourceAsStream("cachecontent.html"));
        //cacheConfig.buildCacheKey(request)
        when(jcrCacheConfig.buildCacheKey(request)).thenReturn(mockedCacheKey);
        when(jcrCacheStore.contains(mockedCacheKey)).thenReturn(true);
        when(jcrCacheStore.getIfPresent(mockedCacheKey)).thenReturn(mockedCacheContent);
        MockSlingHttpServletResponse response = new MockSlingHttpServletResponse();

        boolean delivered = systemUnderTest.deliverCacheContent(request,response, jcrCacheConfig );

        assertTrue(delivered);

        assertEquals(IOUtils.toString(getClass().getResourceAsStream("cachecontent.html"), StandardCharsets.UTF_8), response.getOutput().toString());
    }

    @Test
    public void test_deliver_cache_content_outputstream() throws HttpCacheException, IOException {
        SlingHttpServletRequest request = new MockSlingHttpServletRequest("/content/acs-commons/home", "my-selector", "html", "", "");
        when(jcrCacheConfig.getFilterScope()).thenReturn(HttpCacheConfig.FilterScope.REQUEST);
        when(jcrCacheConfig.accepts(request)).thenReturn(true);
        HttpCacheConfig foundConfig = systemUnderTest.getCacheConfig(request, HttpCacheConfig.FilterScope.REQUEST);
        assertSame(jcrCacheConfig, foundConfig);

        CacheKey mockedCacheKey = mock(CacheKey.class);
        CacheContent mockedCacheContent = mock(CacheContent.class);

        when(mockedCacheContent.getWriteMethod()).thenReturn(HttpCacheServletResponseWrapper.ResponseWriteMethod.OUTPUTSTREAM);
        when(mockedCacheContent.getInputDataStream()).thenReturn(getClass().getResourceAsStream("cachecontent.html"));
        when(mockedCacheContent.getCharEncoding()).thenReturn("utf-8");
        //cacheConfig.buildCacheKey(request)
        when(jcrCacheConfig.buildCacheKey(request)).thenReturn(mockedCacheKey);
        when(jcrCacheStore.contains(mockedCacheKey)).thenReturn(true);
        when(jcrCacheStore.getIfPresent(mockedCacheKey)).thenReturn(mockedCacheContent);
        StringResponseWrapper response = new StringResponseWrapper(new MockSlingHttpServletResponse());


        boolean delivered = systemUnderTest.deliverCacheContent(request,response, jcrCacheConfig );

        assertTrue(delivered);

        assertEquals(IOUtils.toString(getClass().getResourceAsStream("cachecontent.html"), StandardCharsets.UTF_8), response.getString());
    }

    @Test
    public void test_cache_response() throws HttpCacheException, IOException {

        Map<String,Object> props = new HashMap<>();
        props.put(PROP_GLOBAL_RESPONSE_HEADER_EXCLUSIONS, new String[]{"ignoredResponseHeaderGlobal"});
        props.put(PROP_GLOBAL_RESPONSE_COOKIE_EXCLUSIONS, new String[]{"myLoginCookie"});

        //prepare and mock
        systemUnderTest.activate(props);


        SlingHttpServletRequest request = new MockSlingHttpServletRequest("/content/acs-commons/home", "my-selector", "html", "", "");
        SlingHttpServletResponse response = mock(SlingHttpServletResponse.class);

        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
        Map<String,String[]> headers = new HashMap<>();

        when(response.getStatus()).thenReturn(200);
        when(response.getCharacterEncoding()).thenReturn("utf-8");
        when(response.getContentType()).thenReturn("text/html");
        when(response.getHeaderNames()).thenAnswer((Answer<List>) invocationOnMock -> headers.entrySet().stream().map(Map.Entry::getKey).collect(Collectors.toList()));

        when(response.getHeaders(anyString())).thenAnswer((Answer<List>) invocationOnMock -> Arrays.asList(headers.get( invocationOnMock.getArgument(0))));

        when(response.getWriter()).thenReturn(new PrintWriter(byteOutputStream));

        when(jcrCacheConfig.getFilterScope()).thenReturn(HttpCacheConfig.FilterScope.REQUEST);
        when(jcrCacheConfig.accepts(request)).thenReturn(true);
        when(jcrCacheConfig.getExcludedResponseHeaderPatterns()).thenReturn( Arrays.asList(Pattern.compile("ignoredResponseHeaderConfigSpecific")));


        CacheContent mockedCacheContent = mock(CacheContent.class);

        when(mockedCacheContent.getWriteMethod()).thenReturn(HttpCacheServletResponseWrapper.ResponseWriteMethod.PRINTWRITER);
        when(mockedCacheContent.getInputDataStream()).thenReturn(getClass().getResourceAsStream("cachecontent.html"));
        when(mockedCacheContent.getCharEncoding()).thenReturn("utf-8");
        //cacheConfig.buildCacheKey(request)

        headers.put("someResponseHeader", new String[]{"SomeValue"});
        headers.put("ignoredResponseHeaderGlobal", new String[]{"SomeValue"});
        headers.put("ignoredResponseHeaderConfigSpecific", new String[]{"SomeValue"});
        headers.put("Set-Cookie", new String[]{
                "__Secure-myLoginCookie=123; Secure; Domain=acs-commons.com",
                "__Host-myLoginCookie=123; Secure; Path=/",
                "myLoginCookie=38afes7a8; HttpOnly; Path=/",
                "__Host-myOtherCookie=123; Secure; Path=/"
        });

        CacheKey mockedCacheKey = mock(CacheKey.class);
        when(jcrCacheConfig.buildCacheKey(request)).thenReturn(mockedCacheKey);
        when(jcrCacheStore.contains(mockedCacheKey)).thenReturn(true);
        when(jcrCacheStore.getIfPresent(mockedCacheKey)).thenReturn(mockedCacheContent);
        when(jcrCacheStore.createTempSink()).thenReturn(new MemTempSinkImpl());


        //execute code


        HttpCacheServletResponseWrapper wrappedResponse = systemUnderTest.wrapResponse(request,response,jcrCacheConfig);

        wrappedResponse.getWriter().write("rendered-html");

        systemUnderTest.cacheResponse(request, wrappedResponse, jcrCacheConfig);


        //assertions
        HttpCacheConfig foundConfig = systemUnderTest.getCacheConfig(request, HttpCacheConfig.FilterScope.REQUEST);
        assertSame(jcrCacheConfig, foundConfig);

        verify(jcrCacheStore,atLeastOnce()).put(eq(mockedCacheKey), cacheContentCaptor.capture());

        final CacheContent capturedContent = cacheContentCaptor.getValue();
        assertEquals("utf-8", capturedContent.getCharEncoding());
        assertEquals("text/html", capturedContent.getContentType());
        assertEquals(200, capturedContent.getStatus());
        final Map<String, List<String>> storedHeaders = capturedContent.getHeaders();
        assertTrue(storedHeaders.containsKey("someResponseHeader"));
        assertFalse(storedHeaders.containsKey("ignoredResponseHeaderGlobal"));
        assertFalse(storedHeaders.containsKey("ignoredResponseHeaderConfigSpecific"));

        List<String> storedCookies = storedHeaders.get("Set-Cookie");
        assertFalse(storedCookies.isEmpty());
        assertTrue(storedCookies.contains("__Host-myOtherCookie=123; Secure; Path=/"));
        assertFalse(storedCookies.contains("__Secure-myLoginCookie=123; Secure; Domain=acs-commons.com"));
        assertFalse(storedCookies.contains("__Host-myLoginCookie=123; Secure; Path=/"));
        assertFalse(storedCookies.contains("myLoginCookie=38afes7a8; HttpOnly; Path=/"));


        String cachedHTML = IOUtils.toString(capturedContent.getInputDataStream(), StandardCharsets.UTF_8);

        assertEquals("rendered-html", cachedHTML);
    }

}
