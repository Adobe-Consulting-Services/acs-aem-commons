/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2025 Adobe
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

package com.adobe.acs.commons.wcm.impl;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.day.cq.wcm.api.AuthoringUIMode;
import com.day.cq.wcm.api.WCMMode;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import java.io.IOException;

public class AcsCommonsConsoleAuthoringUIModeFilterTest {

    @Rule
    public final AemContext context = new AemContext();

    private AcsCommonsConsoleAuthoringUIModeFilter filter;

    private FilterChain filterChain;
    private SlingHttpServletRequest slingRequest;
    private SlingHttpServletResponse slingResponse;

    @Before
    public void setUp() {
        filter = new AcsCommonsConsoleAuthoringUIModeFilter();
        filterChain = mock(FilterChain.class);
        slingRequest = context.request();
        slingResponse = spy(context.response());
    }

    @Test
    public void testDoFilter_AddsCookie() throws IOException, ServletException {
        WCMMode.EDIT.toRequest(slingRequest);

        filter.doFilter(slingRequest, slingResponse, filterChain);

        verify(slingResponse).addCookie(any(Cookie.class));
    }

    @Test
    public void testDoFilter_DoesntAddCookie() throws IOException, ServletException {
        WCMMode.DISABLED.toRequest(slingRequest);

        filter.doFilter(slingRequest, slingResponse, filterChain);

        verify(slingResponse, times(0)).addCookie(any(Cookie.class));
    }
}