/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.adobeio.service.impl;

import com.adobe.acs.commons.adobeio.service.IntegrationService;
import com.google.gson.JsonObject;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.util.EntityUtils;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.skyscreamer.jsonassert.JSONAssert;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class EndpointServiceImplTest {

    @Mock
    private IntegrationService integrationService;

    @Mock
    private AdobeioHelper helper;

    @InjectMocks
    private EndpointServiceImpl endpointService = new EndpointServiceImpl();

    @Mock
    private CloseableHttpClient httpClient;

    @Mock
    private EndpointConfiguration config;

    @Mock
    private CloseableHttpResponse response;

    @Before
    public void setup() throws Exception {
        when(config.id()).thenReturn("test");
        when(config.endpoint()).thenReturn("https://test.com");
        when(integrationService.getTimeoutinMilliSeconds()).thenReturn(60000);
        when(integrationService.getAccessToken()).thenReturn("ACCESS_TOKEN");
        when(integrationService.getApiKey()).thenReturn("API_KEY");
        when(helper.getHttpClient(60000)).thenReturn(httpClient);
        when(httpClient.execute(any())).thenReturn(response);
        when(response.getEntity()).thenReturn(new StringEntity("{'result':'ok'}"));
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(new ProtocolVersion("http", 1, 1), 200, "OK"));
    }

    @Test
    public void testGet() throws Exception {
        when(config.method()).thenReturn("GET");
        endpointService.activate(config);

        JsonObject result = endpointService.performIO_Action();
        JSONAssert.assertEquals("{'result':'ok'}", result.toString(), false);

        ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
        verify(httpClient, times(1)).execute(captor.capture());
        verify(httpClient, times(1)).close();
        verifyNoMoreInteractions(httpClient);

        HttpUriRequest request = captor.getValue();
        assertTrue(request instanceof HttpGet);
        assertThat(request, hasUri("https://test.com"));
        assertThat(request, hasHeader("authorization", "Bearer ACCESS_TOKEN"));
        assertThat(request, hasHeader("cache-control", "no-cache"));
        assertThat(request, hasHeader("x-api-key", "API_KEY"));
        assertThat(request, hasHeader("content-type", "application/json"));
        assertEquals(4, request.getAllHeaders().length);
    }

    @Test
    public void testGetWithCustomContentType() throws Exception {
        when(config.method()).thenReturn("GET");
        endpointService.activate(config);
        String[] customHeaders = new String[1];
        customHeaders[0] = "content-type:application/vnd.adobe.target.v1+json";
        JsonObject result = endpointService.performIO_Action("https://test.com", "GET",  customHeaders, null);
        JSONAssert.assertEquals("{'result':'ok'}", result.toString(), false);

        ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
        verify(httpClient, times(1)).execute(captor.capture());
        verify(httpClient, times(1)).close();
        verifyNoMoreInteractions(httpClient);

        HttpUriRequest request = captor.getValue();
        assertTrue(request instanceof HttpGet);
        assertThat(request, hasUri("https://test.com"));
        assertThat(request, hasHeader("authorization", "Bearer ACCESS_TOKEN"));
        assertThat(request, hasHeader("cache-control", "no-cache"));
        assertThat(request, hasHeader("x-api-key", "API_KEY"));
        assertThat(request, hasHeader("Content-Type", "application/vnd.adobe.target.v1+json"));
        assertEquals(4, request.getAllHeaders().length);
    }

    @Test
    public void testGetWithQueryParams() throws Exception {
        when(config.method()).thenReturn("GET");
        endpointService.activate(config);

        Map<String, String> params = new HashMap<>();
        params.put("foo", "bar");
        params.put("some with", "spaces to check");

        JsonObject result = endpointService.performIO_Action(params);
        JSONAssert.assertEquals("{'result':'ok'}", result.toString(), false);

        ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
        verify(httpClient, times(1)).execute(captor.capture());
        verify(httpClient, times(1)).close();
        verifyNoMoreInteractions(httpClient);

        HttpUriRequest request = captor.getValue();
        assertTrue(request instanceof HttpGet);
        assertThat(request, hasUri("https://test.com?foo=bar&some+with=spaces+to+check"));
        assertThat(request, hasHeader("authorization", "Bearer ACCESS_TOKEN"));
        assertThat(request, hasHeader("cache-control", "no-cache"));
        assertThat(request, hasHeader("x-api-key", "API_KEY"));
        assertThat(request, hasHeader("content-type", "application/json"));
        assertThat(request, hasExactHeaderCount(4));
    }

    @Test
    public void testGetWithServiceSpecificHeaders() throws Exception {
        when(config.method()).thenReturn("GET");
        when(config.specificServiceHeaders()).thenReturn(new String[] {
           "",
           ":",
           "foo:",
           ":bar",
           "custom:header",
           "custom:header1",
           "custom2:header2"
        });
        endpointService.activate(config);

        JsonObject result = endpointService.performIO_Action();
        JSONAssert.assertEquals("{'result':'ok'}", result.toString(), false);

        ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
        verify(httpClient, times(1)).execute(captor.capture());
        verify(httpClient, times(1)).close();
        verifyNoMoreInteractions(httpClient);

        HttpUriRequest request = captor.getValue();
        assertTrue(request instanceof HttpGet);
        assertThat(request, hasUri("https://test.com"));
        assertThat(request, hasHeader("authorization", "Bearer ACCESS_TOKEN"));
        assertThat(request, hasHeader("cache-control", "no-cache"));
        assertThat(request, hasHeader("x-api-key", "API_KEY"));
        assertThat(request, hasHeader("content-type", "application/json"));
        assertThat(request, hasHeader("custom", "header", "header1"));
        assertThat(request, hasHeader("custom2", "header2"));
        assertEquals(7, request.getAllHeaders().length);
    }

    @Test
    public void testPostWithServiceSpecificHeaders() throws Exception {
        when(config.method()).thenReturn("POST");
        when(config.specificServiceHeaders()).thenReturn(new String[] {
           "content-type:application/json;charset=UTF-8",
        });
        endpointService.activate(config);
        JsonObject payload = new JsonObject();
        payload.addProperty("result", "My ‘Payload’");
        JsonObject result = endpointService.performIO_Action(payload);
        JSONAssert.assertEquals("{'result':'ok'}", result.toString(), false);

        ArgumentCaptor<HttpPost> captor = ArgumentCaptor.forClass(HttpPost.class);
        verify(httpClient, times(1)).execute(captor.capture());
        verify(httpClient, times(1)).close();
        verifyNoMoreInteractions(httpClient);

        HttpPost request = captor.getValue();
        assertTrue(request instanceof HttpPost);
        assertThat(request, hasUri("https://test.com"));
        assertThat(request, hasEntity("{\"result\":\"My ‘Payload’\"}"));
        assertThat(request, hasHeader("authorization", "Bearer ACCESS_TOKEN"));
        assertThat(request, hasHeader("cache-control", "no-cache"));
        assertThat(request, hasHeader("x-api-key", "API_KEY"));
        assertThat(request, hasHeader("content-type", "application/json", "application/json;charset=UTF-8"));

        assertEquals(5, request.getAllHeaders().length);
    }

    @Test
    public void testConvertServiceSpecificHeadersWithNull() {
          List<Map.Entry<String, String>> headers = endpointService.convertServiceSpecificHeaders(null);
       
          assertEquals(Collections.EMPTY_LIST, headers);
    }
    
    @Test
    public void testConvertServiceSpecificHeadersWitNothNull() {
          String[] arr = {"name1:value","name2:value"};
          List<Map.Entry<String, String>> headers = endpointService.convertServiceSpecificHeaders(arr);

          assertEquals(2, headers.size());
    }

    @Test
    public void testCharsetFrom() {
        Charset iso88591 = endpointService.charsetFrom(new BasicHeader("Content-Type", "application/json;charset=iso-8859-1"));
        assertEquals("ISO-8859-1", iso88591.name());

        Charset utf8 = endpointService.charsetFrom(new BasicHeader("Content-Type", "application/json;charset=UTF-8"));
        assertEquals("UTF-8", utf8.name());

        assertEquals(null, endpointService.charsetFrom(null));
        assertEquals(null, endpointService.charsetFrom(new BasicHeader("Content-Type", "application/json;charset=invalid")));
    }

    public static TypeSafeMatcher<HttpUriRequest> hasUri(final String uri) {
        return new TypeSafeMatcher<HttpUriRequest>() {
            @Override
            protected boolean matchesSafely(HttpUriRequest request) {
                return uri.equals(request.getURI().toString());
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("with uri: " + uri);
            }
        };
    }

    public static TypeSafeMatcher<HttpUriRequest> hasExactHeaderCount(final int count) {
        return new TypeSafeMatcher<HttpUriRequest>() {
            @Override
            protected boolean matchesSafely(HttpUriRequest request) {
                return count == request.getAllHeaders().length;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("with " + count + " headers");
            }
        };
    }

    public static TypeSafeMatcher<HttpUriRequest> hasHeader(final String headerName, final String... headerValues) {
        return new TypeSafeMatcher<HttpUriRequest>() {
            @Override
            protected boolean matchesSafely(HttpUriRequest request) {
                Header[] headers = request.getHeaders(headerName);
                if (headers == null) {
                    return false;
                }
                List<String> actualValues = new ArrayList<>();
                for (Header header : headers) {
                    actualValues.add(header.getValue());
                }
                return Arrays.equals(headerValues, actualValues.toArray(new String[0]));
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("with header: " + headerName + " = " + Arrays.toString(headerValues));
            }
        };
    }

    public static TypeSafeMatcher<HttpPost> hasEntity(final String payload) {
        return new TypeSafeMatcher<HttpPost>() {
            @Override
            protected boolean matchesSafely(HttpPost request) {
                try {
                    HttpEntity entity = request.getEntity();
                    String content = EntityUtils.toString(entity);
                    return payload.equals(content);
                } catch (IOException e) {
                    return false;
                }
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("with payload: " + payload);
            }

            @Override
            public void describeMismatchSafely(HttpPost request, Description description) {
                try {
                    HttpEntity entity = request.getEntity();
                    String content = EntityUtils.toString(entity);
                    description.appendText("was " + content);
                } catch (IOException e) {
                    description.appendText("got exception " + e.getMessage());
                }
            }
        };
    }
}
