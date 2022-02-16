/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2018 Adobe
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
package com.adobe.acs.commons.throttling;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestProgressTracker;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class RequestThrottlerTest {

    @Rule
    public SlingContext context = new SlingContext();

    RequestThrottler rt;
    RequestThrottler.Config config;

    @Before
    public void before() {
        RequestThrottler r = new RequestThrottler();
        rt = spy(r);
        config = mock(RequestThrottler.Config.class);
        context.create().resource("/content/foobar", "a", "b");
    }

    @Test
    public void pathFilterNoConfiguration() {
        when(config.filtered_paths()).thenReturn(new String[] {});
        rt.activate(config);
        assertFalse(rt.needsFiltering("/bla"));
    }

    @Test
    public void pathFilterExactMatch() {
        when(config.filtered_paths()).thenReturn(new String[] { "/content" });
        rt.activate(config);
        assertTrue(rt.needsFiltering("/content"));
        assertFalse(rt.needsFiltering("/content/something"));
        assertFalse(rt.needsFiltering("/foo"));
    }

    @Test
    public void pathFilterWildcardMatch() {
        when(config.filtered_paths()).thenReturn(new String[] { "/content.*" });
        rt.activate(config);
        assertTrue(rt.needsFiltering("/content"));
        assertTrue(rt.needsFiltering("/content/something"));
        assertFalse(rt.needsFiltering("/foo"));
        assertFalse(rt.needsFiltering("/foo/content"));
    }

    @Test
    public void noMatchingPath() throws Exception {
        when(config.filtered_paths()).thenReturn(new String[] { "/content" });
        when(config.max_requests_per_minute()).thenReturn(10);
        rt.activate(config);
        context.request().setResource(context.resourceResolver().getResource("/"));
        FilterChain chain = mock(FilterChain.class);
        doNothing().when(chain).doFilter(any(), any());
        rt.doFilter(context.request(), context.response(), chain);
        verify(chain).doFilter(context.request(), context.response());
        verify(rt, never()).doFilterInternal(any(), any());
    }

    @Test
    public void doFilter_withMatchingPath() throws Exception {
        context.request().setResource(context.resourceResolver().getResource("/content/foobar"));

        Instant now = Instant.now();
        Clock c = mock(Clock.class);
        when(c.instant()).thenReturn(now);
        when(config.filtered_paths()).thenReturn(new String[] { "/content/.*" });
        when(config.max_requests_per_minute()).thenReturn(10);
        rt.activate(config);
        rt.clock = c;
        /*
         * The implementation of context.response() is current incomplete and throws an
         * UnsupportedOperationException when calling getRequestProgressTracker
         */
        SlingHttpServletRequest request = spy(context.request());
        RequestProgressTracker rpt = mock(RequestProgressTracker.class);
        doReturn(rpt).when(request).getRequestProgressTracker();

        FilterChain chain = mock(FilterChain.class);
        doNothing().when(chain).doFilter(any(), any());

        for (int i = 0; i < 10; i++) {
            rt.doFilter(request, context.response(), chain);

        }
        verify(chain, times(10)).doFilter(request, context.response());
        verify(rpt, times(10)).log(any());

    }

}
