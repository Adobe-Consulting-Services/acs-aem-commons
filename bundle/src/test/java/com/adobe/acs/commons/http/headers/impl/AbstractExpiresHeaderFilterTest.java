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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;

import com.adobe.acs.commons.http.headers.impl.AbstractDispatcherCacheHeaderFilter;
import com.adobe.acs.commons.http.headers.impl.AbstractExpiresHeaderFilter;

@RunWith(MockitoJUnitRunner.class)
public class AbstractExpiresHeaderFilterTest {

    AbstractExpiresHeaderFilter filter;

    Dictionary<String, Object> properties = null;

    private String exipres = "02:30";
    
    Set<String> agents = null;
    Set<String> expires = null;

    @SuppressWarnings("rawtypes")
    Map params = null;

    @Mock
    ComponentContext componentContext;

    @Mock
    HttpServletRequest request;

    @Before
    public void setup() throws Exception {
        properties = new Hashtable<String, Object>();
        properties.put(AbstractExpiresHeaderFilter.PROP_EXPIRES_TIME, exipres);

        agents = new HashSet<String>();
        expires = new HashSet<String>();
        params = new HashMap();

        filter = new AbstractExpiresHeaderFilter() {
            @Override
            protected void adjustExpires(Calendar nextExpiration) {
                // Do nothing.
            }
        };

        when(request.getMethod()).thenReturn("GET");
        when(request.getParameterMap()).thenReturn(params);
        agents.add(AbstractDispatcherCacheHeaderFilter.DISPATCHER_AGENT_HEADER_VALUE);
        when(request.getHeaders(AbstractDispatcherCacheHeaderFilter.SERVER_AGENT_NAME))
                .thenReturn(Collections.enumeration(agents));

    }

    @After
    public void tearDown() throws Exception {
        properties = null;
        agents = null;
        expires = null;
        params = null;

        reset(componentContext, request);
    }

    @Test
    public void testGetHeaderName() {
        assertEquals(AbstractExpiresHeaderFilter.EXPIRES_NAME, filter.getHeaderName());
    }

    @Test
    public void testGetHeaderValue() throws Exception {

        when(componentContext.getProperties()).thenReturn(properties);

        Calendar expected = Calendar.getInstance();
        expected.set(Calendar.HOUR_OF_DAY, 2);
        expected.set(Calendar.MINUTE, 30);
        expected.set(Calendar.SECOND, 0);
        expected.set(Calendar.MILLISECOND, 0);

        filter.doActivate(componentContext);
        String header = filter.getHeaderValue();
        Date date = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z").parse(header);
        Calendar actual = Calendar.getInstance();
        actual.setTime(date);
        actual.set(Calendar.SECOND, 0);
        actual.set(Calendar.MILLISECOND, 0);

        assertTrue(DateUtils.isSameInstant(expected, actual));
    }

    @Test
    public void testAcceptsHasExpiresHeader() throws Exception {

        expires.add("Some Expires Header");

        when(request.getHeaders(AbstractExpiresHeaderFilter.EXPIRES_NAME))
                .thenReturn(Collections.enumeration(expires));

        boolean result = filter.accepts(request);
        assertFalse(result);

        verify(request).getHeaders(AbstractExpiresHeaderFilter.EXPIRES_NAME);
    }

    @Test
    public void testAcceptsNoExpiresHeader() throws Exception {

        when(request.getHeaders(AbstractExpiresHeaderFilter.EXPIRES_NAME))
                .thenReturn(Collections.enumeration(expires));

        boolean result = filter.accepts(request);
        assertTrue(result);

        verify(request).getHeaders(AbstractExpiresHeaderFilter.EXPIRES_NAME);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testAcceptsCalledParent() throws Exception {

        params.put("key", "value");

        boolean result = filter.accepts(request);
        assertFalse(result);

        verify(request, times(0)).getHeaders(AbstractExpiresHeaderFilter.EXPIRES_NAME);
    }

    @Test(expected = ConfigurationException.class)
    public void testActivateNoExpiresTime() throws Exception {
        properties.remove(AbstractExpiresHeaderFilter.PROP_EXPIRES_TIME);
        when(componentContext.getProperties()).thenReturn(properties);
        filter.activate(componentContext);
    }

    @Test(expected = ConfigurationException.class)
    public void testActivateInvalidExpiresTime() throws Exception {
        properties.put(AbstractExpiresHeaderFilter.PROP_EXPIRES_TIME, "9999");
        when(componentContext.getProperties()).thenReturn(properties);
        filter.activate(componentContext);
    }

    @Test
    public void testDoActivateSuccess() throws Exception {

        when(componentContext.getProperties()).thenReturn(properties);

        filter.doActivate(componentContext);
        assertNotNull(filter.getHeaderValue());
        verify(componentContext).getProperties();
        verifyNoMoreInteractions(componentContext);

    }
}
