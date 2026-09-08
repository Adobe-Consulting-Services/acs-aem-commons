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
package com.adobe.acs.commons.http.headers.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;


@RunWith(MockitoJUnitRunner.class)
public class AbstractCacheHeaderFilterTest {
    Map<String, String[]> params = null;

    Set<String> agents = null;

    String pattern = "/content/.*";

    private static final String HEADER_NAME = "Header Name";

    private static final String HEADER_VALUE = "Header Value";

    @Mock
    BundleContext bundleContext;

    @Mock
    ServiceRegistration<Filter> serviceRegistration;
    
    @Mock
    ServiceRegistration<Filter> secondRegistration;

    @Mock
    HttpServletRequest request;

    @Mock
    HttpServletResponse response;
    
    Map<String, Collection<String>> headers = new HashMap<>();

    @Mock
    FilterChain chain;

    @Before
    public void setup() throws Exception {

        params = new HashMap<>();
        agents = new HashSet<>();

        when(bundleContext.registerService(eq(Filter.class), any(Filter.class), any())).thenReturn(serviceRegistration);

        mockResponseHeaders(response, headers);
        when(request.getMethod()).thenReturn("GET");
        when(request.getParameterMap()).thenReturn(params);

    }

    static void mockResponseHeaders(HttpServletResponse response, Map<String, Collection<String>> headers) {
        lenient().when(response.containsHeader(any())).thenAnswer(i -> headers.containsKey(i.getArgument(0)));
        lenient().doAnswer(i -> {
            String name = i.getArgument(0);
            String value = i.getArgument(1);
            Collection<String> values = new LinkedList<>();
            values.add(value);
            headers.put(name, values);
            return null;
        }).when(response).setHeader(any(), any());
        lenient().doAnswer(i -> {
            String name = i.getArgument(0);
            String value = i.getArgument(1);
            headers.computeIfAbsent(name, k -> new LinkedList<>()).add(value);
            return null;
        }).when(response).addHeader(any(), any());
        lenient().when(response.getHeader(any())).thenAnswer(i -> {
            String name = i.getArgument(0);
            Collection<String> values = headers.get(name);
            return (values != null && !values.isEmpty()) ? values.iterator().next() : null;
        });
        lenient().when(response.getHeaders(any())).thenAnswer(i -> {
            String name = i.getArgument(0);
            Collection<String> values = headers.get(name);
            return (values != null) ? Arrays.asList(values.toArray(new String[0])) : Collections.emptyList();
        });
        // no support for int or date(long) headers in this mock
    }

    static final class HttpWhiteboardFilterPropertiesMatcher extends TypeSafeMatcher<Dictionary<String, Object>> {
        private final String pattern;
        HttpWhiteboardFilterPropertiesMatcher(String pattern) {
            this.pattern = pattern;
        }
        @Override
        public void describeTo(Description description) {
            // do nothing
        }

        @Override
        protected boolean matchesSafely(Dictionary<String, Object> item) {
            return StringUtils.equals(pattern, item.get(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_REGEX).toString());
        }
    }

    @Test
    public void testMockResponseHeaders() {
        Map<String, Collection<String>> testHeaders = new HashMap<>();
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        mockResponseHeaders(mockResponse, testHeaders);

        String headerName = "Test-Header";
        String headerValue = "Test-Value";

        assertFalse(testHeaders.containsKey(headerName));

        mockResponse.setHeader(headerName, headerValue);
        assertTrue(testHeaders.containsKey(headerName));
        assertEquals(Collections.singletonList(headerValue), testHeaders.get(headerName));
        
        assertTrue(mockResponse.containsHeader(headerName));
        assertEquals(headerValue, mockResponse.getHeader(headerName));
        assertEquals(Collections.singletonList(headerValue), mockResponse.getHeaders(headerName));

        String additionalValue = "Additional-Value";
        mockResponse.addHeader(headerName, additionalValue);
        assertTrue(testHeaders.containsKey(headerName));
        assertEquals(Arrays.asList(headerValue, additionalValue), testHeaders.get(headerName));
    }

    AbstractCacheHeaderFilter createFilter(boolean isSlingFilter, String... patterns) {
        return createFilter(HEADER_VALUE, isSlingFilter, patterns);
    }

    AbstractCacheHeaderFilter createFilter(String headerValue, boolean isSlingFilter, String... patterns) {
        return new AbstractCacheHeaderFilter(isSlingFilter, patterns, bundleContext) {

            @Override
            protected String getHeaderName() {
                return HEADER_NAME;
            }

            @Override
            protected String getHeaderValue(HttpServletRequest request) {
                return headerValue;
            }
        };
    }

    @Test
    public void testActivateSuccess() throws Exception {
        AbstractCacheHeaderFilter filter = createFilter(false, pattern);
        verify(bundleContext).registerService(eq(Filter.class), eq(filter), argThat(new HttpWhiteboardFilterPropertiesMatcher(pattern)));
        verifyNoMoreInteractions(bundleContext, serviceRegistration);
    }

    @Test
    public void testActivateMultipleFilters() throws Exception {
        final String secondPattern = "/content/dam/.*";
        AbstractCacheHeaderFilter filter = createFilter(false, pattern, secondPattern);
        verify(bundleContext).registerService(eq(Filter.class), eq(filter), argThat(new HttpWhiteboardFilterPropertiesMatcher(pattern)));
        verify(bundleContext).registerService(eq(Filter.class), eq(filter), argThat(new HttpWhiteboardFilterPropertiesMatcher(secondPattern)));
        verifyNoMoreInteractions(bundleContext, serviceRegistration, secondRegistration);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testActivateNoFilters() throws Exception {
        createFilter(false);
    }

    @Test
    public void testDeactivate() throws Exception {
        AbstractCacheHeaderFilter filter = createFilter(false, pattern);
        filter.deactivate();
        filter.deactivate();
        verify(serviceRegistration).unregister();
        verifyNoMoreInteractions(serviceRegistration);

    }

    @Test
    public void testDeactivateMultipleFilters() throws Exception {
        final String secondPattern = "/content/dam/.*";
        when(bundleContext.registerService(eq(Filter.class), any(Filter.class), argThat(new HttpWhiteboardFilterPropertiesMatcher(secondPattern)))).thenReturn(secondRegistration);
        AbstractCacheHeaderFilter filter = createFilter(false, pattern, secondPattern);
        filter.deactivate();
        verify(serviceRegistration).unregister();
        verify(secondRegistration).unregister();
        verifyNoMoreInteractions(serviceRegistration, secondRegistration);
    }

    @Test
    public void testDoFilterInvalidRequest() throws Exception {
        // no HTTP request
        ServletRequest request = mock(ServletRequest.class);
        AbstractCacheHeaderFilter filter = createFilter(false, pattern);
        filter.doFilter(request, response, chain);
        assertFalse(response.containsHeader(HEADER_NAME));
        verify(chain).doFilter(request, response);
    }

    @Test
    public void testDoFilterInvalidResponse() throws Exception {
        // no HTTP response
        ServletResponse response = mock(ServletResponse.class);
        AbstractCacheHeaderFilter filter = createFilter(false, pattern);
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test
    public void testDoFilterInvalidMethod() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        AbstractCacheHeaderFilter filter = createFilter(false, pattern);
        filter.doFilter(request, response, chain);
        assertFalse(response.containsHeader(HEADER_NAME));
    }

    @Test
    public void testDoFilterUrlParams() throws Exception {
        params.put("parameter", new String[] { "value" });
        AbstractCacheHeaderFilter filter = createFilter(false, pattern);
        filter.doFilter(request, response, chain);
        assertFalse(response.containsHeader(HEADER_NAME));
        verify(chain).doFilter(request, response);
    }

    @Test
    public void testDoFilterNoServerAgent() throws Exception {

        when(request.getHeaders(AbstractCacheHeaderFilter.SERVER_AGENT_NAME))
            .thenReturn(Collections.enumeration(agents));
        AbstractCacheHeaderFilter filter = createFilter(false, pattern);
        filter.doFilter(request, response, chain);
        assertFalse(response.containsHeader(HEADER_NAME));
        verify(chain).doFilter(request, response);
    }

    @Test
    public void testDoFilterInvalidServerAgent() throws Exception {
        agents.add("Not-Day-Communique-Dispatcher");

        when(request.getHeaders(AbstractCacheHeaderFilter.SERVER_AGENT_NAME))
            .thenReturn(Collections.enumeration(agents));
        AbstractCacheHeaderFilter filter = createFilter(false, pattern);
        filter.doFilter(request, response, chain);
        assertFalse(response.containsHeader(HEADER_NAME));
        verify(chain).doFilter(request, response);
    }

    @Test
    public void testDoFilterSuccess() throws Exception {
        agents.add(AbstractCacheHeaderFilter.DISPATCHER_AGENT_HEADER_VALUE);
        when(request.getHeaders(AbstractCacheHeaderFilter.SERVER_AGENT_NAME))
            .thenReturn(Collections.enumeration(agents));
        AbstractCacheHeaderFilter filter = createFilter(false, pattern);
        filter.doFilter(request, response, chain);
        assertTrue(response.containsHeader(HEADER_NAME));
        verify(chain).doFilter(request, response);
    }


    @Test
    public void testMultipleFilters() throws Exception {
        agents.add(AbstractCacheHeaderFilter.DISPATCHER_AGENT_HEADER_VALUE);
        when(request.getHeaders(AbstractCacheHeaderFilter.SERVER_AGENT_NAME))
                .thenAnswer(i -> Collections.enumeration(agents));
        AbstractCacheHeaderFilter filter1 = createFilter("firstvalue", false, pattern);
        AbstractCacheHeaderFilter filter2 = createFilter("secondvalue", false, pattern);
        
        FilterChain nestedChain = (req, res) -> filter2.doFilter(req, res, chain);
        
        filter1.doFilter(request, response, nestedChain);
        verify(chain).doFilter(request, response);
        assertTrue(response.containsHeader(HEADER_NAME));
        assertEquals(Collections.singletonList("firstvalue"), headers.get(HEADER_NAME));
    }

}
