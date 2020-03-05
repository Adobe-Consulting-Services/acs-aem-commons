/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2015 Adobe
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
package com.adobe.acs.commons.http.headers.impl;

import static org.mockito.Mockito.*;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import org.mockito.stubbing.Answer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;

import com.adobe.acs.commons.http.headers.impl.AbstractDispatcherCacheHeaderFilter;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;


public class AbstractDispatcherCacheHeaderFilterTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    AbstractDispatcherCacheHeaderFilter filter;

    Dictionary<String, Object> properties = null;

    @SuppressWarnings("rawtypes")
    Map params = null;

    Set<String> agents = null;

    String pattern = "/content/.*";

    private String headerName = "Header Name";

    private String headerValue = "Header Value";

    @Mock
    ComponentContext componentContext;

    @Mock
    BundleContext bundleContext;

    @Mock
    ServiceRegistration serviceRegistration;

    @Mock
    HttpServletRequest request;

    @Mock
    HttpServletResponse response;

    @Mock
    FilterChain chain;

    @Before
    @SuppressWarnings("rawtypes")
    public void setup() throws Exception {
        properties = new Hashtable<String, Object>();
        String[] patterns = new String[] { pattern };
        properties.put(AbstractDispatcherCacheHeaderFilter.PROP_FILTER_PATTERN, patterns);

        params = new HashMap();
        agents = new HashSet<String>();

        filter = new AbstractDispatcherCacheHeaderFilter() {

            @Override
            protected String getHeaderName() {
                return headerName;
            }

            @Override
            protected String getHeaderValue() {
                return headerValue;
            }

            @Override
            protected void doActivate(ComponentContext context) throws Exception {
            }
        };
        when(componentContext.getProperties()).thenReturn(properties);
        when(componentContext.getBundleContext()).thenReturn(bundleContext);
        when(bundleContext.registerService(anyString(), any(), (Dictionary) any())).thenReturn(serviceRegistration);

        when(request.getMethod()).thenReturn("GET");
        when(request.getParameterMap()).thenReturn(params);

        final Map<String, Object> attributes = new HashMap<>();
        doAnswer(i -> attributes.put(i.getArgument(0), i.getArgument(1))).when(request).setAttribute(any(), any());
        when(request.getAttribute(any())).thenAnswer(i -> attributes.get(i.getArgument(0)));
    }

    @After
    public void tearDown() throws Exception {
        properties = null;
        params = null;
        agents = null;

        reset(componentContext, bundleContext, serviceRegistration, request, response, chain);
    }

    @Test
    public void testActivateSuccess() throws Exception {

        final BaseMatcher<Dictionary<String, Object>> filterPropsMatcher = new BaseMatcher<Dictionary<String, Object>>() {
            @Override
            public void describeTo(Description description) {
                // do nothing
            }

            @Override
            @SuppressWarnings("unchecked")
            public boolean matches(Object item) {
                return StringUtils.equals(pattern, ((Dictionary<String, Object>) item).get(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_REGEX).toString());
            }
        };

        filter.activate(componentContext);
        verify(componentContext).getProperties();
        verify(componentContext).getBundleContext();
        verify(bundleContext).registerService(eq(Filter.class.getName()), eq(filter), argThat(filterPropsMatcher));

        verifyNoMoreInteractions(componentContext, bundleContext, serviceRegistration);

    }

    @Test
    public void testActivateMultipleFilters() throws Exception {

        final ServiceRegistration secondRegistration = mock(ServiceRegistration.class);

        final String secondPattern = "/content/dam/.*";
        properties.put(AbstractDispatcherCacheHeaderFilter.PROP_FILTER_PATTERN,
                new String[] { pattern, secondPattern });

        final BaseMatcher<Dictionary<String, Object>> firstPropsMatcher = new BaseMatcher<Dictionary<String, Object>>() {
            @Override
            public void describeTo(Description description) {
                // do nothing
            }

            @Override
            @SuppressWarnings("unchecked")
            public boolean matches(Object item) {
                return StringUtils.equals(pattern, ((Dictionary<String, Object>) item).get(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_REGEX).toString());
            }
        };

        final BaseMatcher<Dictionary<String, Object>> secondPropsMatcher = new BaseMatcher<Dictionary<String, Object>>() {
            @Override
            public void describeTo(Description description) {
                // do nothing
            }

            @Override
            @SuppressWarnings("unchecked")
            public boolean matches(Object item) {
                return StringUtils.equals(secondPattern, ((Dictionary<String, Object>) item).get(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_REGEX).toString());
            }
        };

        filter.activate(componentContext);
        verify(componentContext).getProperties();
        verify(componentContext, times(2)).getBundleContext();
        verify(bundleContext).registerService(eq(Filter.class.getName()), eq(filter), argThat(firstPropsMatcher));
        verify(bundleContext).registerService(eq(Filter.class.getName()), eq(filter), argThat(secondPropsMatcher));

        verifyNoMoreInteractions(componentContext, bundleContext, serviceRegistration, secondRegistration);

    }

    @Test(expected = ConfigurationException.class)
    public void testActivateNoFilters() throws Exception {
        properties.remove(AbstractDispatcherCacheHeaderFilter.PROP_FILTER_PATTERN);
        when(componentContext.getProperties()).thenReturn(properties);
        filter.activate(componentContext);
    }

    @Test
    public void testDeactivate() throws Exception {

        filter.activate(componentContext);
        filter.deactivate(componentContext);

        filter.deactivate(componentContext);

        verify(serviceRegistration).unregister();
        verifyNoMoreInteractions(serviceRegistration);

    }

    @Test
    @SuppressWarnings("rawtypes")
    public void testDeactivateMultipleFilters() throws Exception {

        ServiceRegistration secondRegistration = mock(ServiceRegistration.class);

        final String secondPattern = "/content/dam/.*";
        properties.put(AbstractDispatcherCacheHeaderFilter.PROP_FILTER_PATTERN,
                new String[] { pattern, secondPattern });

        when(bundleContext.registerService(anyString(), any(), (Dictionary) any())).thenReturn(serviceRegistration,
                secondRegistration);

        filter.activate(componentContext);
        filter.deactivate(componentContext);

        verify(serviceRegistration).unregister();
        verify(secondRegistration).unregister();
        verifyNoMoreInteractions(serviceRegistration, secondRegistration);

    }

    @Test
    public void testDoFilterInvalidRequest() throws Exception {
        ServletRequest request = mock(ServletRequest.class);
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        verifyNoMoreInteractions(request, this.request, response, chain);
    }

    @Test
    public void testDoFilterInvalidResponse() throws Exception {
        ServletResponse response = mock(ServletResponse.class);
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        verifyNoMoreInteractions(request, response, this.response, chain);
    }

    @Test
    public void testDoFilterInvalidMethod() throws Exception {

        when(request.getMethod()).thenReturn("POST");
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);

        verify(request).getHeaders(AbstractDispatcherCacheHeaderFilter.SERVER_AGENT_NAME);
        verify(request).getMethod();
        verifyNoMoreInteractions(request, this.request, response, chain);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testDoFilterHashParams() throws Exception {

        params.put("parameter", "value");
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);

        verify(request).getHeaders(AbstractDispatcherCacheHeaderFilter.SERVER_AGENT_NAME);
        verify(request).getMethod();
        verify(request).getParameterMap();
        verifyNoMoreInteractions(request, this.request, response, chain);
    }

    @Test
    public void testDoFilterNoServerAgent() throws Exception {

        when(request.getHeaders(AbstractDispatcherCacheHeaderFilter.SERVER_AGENT_NAME))
            .thenReturn(Collections.enumeration(agents));

        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);

        verify(request).getHeaders(AbstractDispatcherCacheHeaderFilter.SERVER_AGENT_NAME);
        verify(request).getMethod();
        verify(request).getParameterMap();
        verifyNoMoreInteractions(request, this.request, response, chain);
    }

    @Test
    public void testDoFilterInvalidServerAgent() throws Exception {


        agents.add("Not-Day-Communique-Dispatcher");

        when(request.getHeaders(AbstractDispatcherCacheHeaderFilter.SERVER_AGENT_NAME))
            .thenReturn(Collections.enumeration(agents));

        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);

        verify(request).getHeaders(AbstractDispatcherCacheHeaderFilter.SERVER_AGENT_NAME);
        verify(request).getMethod();
        verify(request).getParameterMap();
        verifyNoMoreInteractions(request, this.request, response, chain);
    }

    

    @Test
    public void testDoFilterSuccess() throws Exception {


        agents.add(AbstractDispatcherCacheHeaderFilter.DISPATCHER_AGENT_HEADER_VALUE);
        when(request.getHeaders(AbstractDispatcherCacheHeaderFilter.SERVER_AGENT_NAME))
            .thenReturn(Collections.enumeration(agents));

        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);

        verify(request).getHeaders(AbstractDispatcherCacheHeaderFilter.SERVER_AGENT_NAME);
        verify(request).getMethod();
        verify(request).getParameterMap();
        verify(request).getAttribute("com.adobe.acs.commons.http.headers.impl.AbstractDispatcherCacheHeaderFilter.header.Header Name");
        verify(request).setAttribute(eq("com.adobe.acs.commons.http.headers.impl.AbstractDispatcherCacheHeaderFilter.header.Header Name"), any());
        verify(response).addHeader(headerName, headerValue);
        verifyNoMoreInteractions(request, this.request, response, chain);
    }


    @Test
    public void testMultipleFilters() throws Exception {
        agents.add(AbstractDispatcherCacheHeaderFilter.DISPATCHER_AGENT_HEADER_VALUE);
        when(request.getHeaders(AbstractDispatcherCacheHeaderFilter.SERVER_AGENT_NAME))
                .thenAnswer(i -> Collections.enumeration(agents));

        filter.doFilter(request, response, chain);
        filter.doFilter(request, response, chain);

        verify(response, times(1)).addHeader(headerName, headerValue);

    }

}
