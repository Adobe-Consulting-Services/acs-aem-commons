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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;

@RunWith(MockitoJUnitRunner.class)
public class AbstractExpiresHeaderFilterTest {

    AbstractExpiresHeaderFilter filter;

    Map<String, String[]> params = new HashMap<>();

    @Mock
    BundleContext bundleContext;

    @Mock
    HttpServletRequest request;
    
    @Mock
    HttpServletResponse response;
    
    @Mock
    FilterChain chain;

    @Before
    public void setup() throws Exception {

        filter = createFilter("02:30");

        when(request.getMethod()).thenReturn("GET");
        when(request.getParameterMap()).thenReturn(params);
        when(request.getHeaders(AbstractCacheHeaderFilter.SERVER_AGENT_NAME))
                .thenReturn(Collections.enumeration(Collections.singletonList(AbstractCacheHeaderFilter.DISPATCHER_AGENT_HEADER_VALUE)));

    }

    private AbstractExpiresHeaderFilter createFilter(String expires) {
       return new AbstractExpiresHeaderFilter(expires, new AbstractCacheHeaderFilter.ServletRequestPredicates(new String[] { "/content/dam/.*" }), 0, bundleContext) {
            @Override
            protected void adjustExpires(Calendar nextExpiration) {
                // Do nothing.
            }
        };
    }

    @Test
    public void testGetHeaderName() {
        assertEquals("Expires", filter.getHeaderName());
    }

    @Test
    public void testGetHeaderValue() throws Exception {
        Calendar expected = Calendar.getInstance();
        expected.set(Calendar.HOUR_OF_DAY, 2);
        expected.set(Calendar.MINUTE, 30);
        expected.set(Calendar.SECOND, 0);
        expected.set(Calendar.MILLISECOND, 0);

        String header = filter.getHeaderValue(request);
        Date date = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z").parse(header);
        Calendar actual = Calendar.getInstance();
        actual.setTime(date);
        actual.set(Calendar.SECOND, 0);
        actual.set(Calendar.MILLISECOND, 0);

        assertTrue(DateUtils.isSameInstant(expected, actual));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testActivateInvalidExpiresTime() throws Exception {
        createFilter("9999");
    }

    @Test
    public void testDoFilter() throws Exception {
        filter.doFilter(request, response, chain);
        verify(response, times(1)).setHeader(eq("Expires"), any());
    }
}
