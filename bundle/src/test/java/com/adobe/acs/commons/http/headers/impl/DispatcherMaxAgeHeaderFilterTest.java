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
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.converter.Converters;

@RunWith(MockitoJUnitRunner.class)
public class DispatcherMaxAgeHeaderFilterTest {


    Set<String> agents = null;
    Set<String> cachecontrol = null;

    @SuppressWarnings("rawtypes")
    Map params = null;

    @Mock
    HttpServletRequest request;

    @Mock
    HttpServletResponse response;

    @Mock
    BundleContext bundleContext;
    
    @Mock
    FilterChain chain;
    
    @Before
    public void setup() throws Exception {
        agents = new HashSet<String>();
        cachecontrol = new HashSet<String>();
        params = new HashMap();

        when(request.getMethod()).thenReturn("GET");
        when(request.getParameterMap()).thenReturn(params);
        agents.add(AbstractCacheHeaderFilter.DISPATCHER_AGENT_HEADER_VALUE);
        when(request.getHeaders(AbstractCacheHeaderFilter.SERVER_AGENT_NAME))
                .thenReturn(Collections.enumeration(agents));
        AbstractCacheHeaderFilterTest.mockResponseHeaders(response, params);

    }

    DispatcherMaxAgeHeaderFilter createFilter(long maxAge) {
        Map<String, Object> props = new HashMap<>();
        props.put("filter.pattern", new String[] { "/content/dam/.*" });
        props.put("max.age", maxAge);
        DispatcherMaxAgeHeaderFilter.Config config = Converters.standardConverter().convert(props).to(DispatcherMaxAgeHeaderFilter.Config.class);
        return new DispatcherMaxAgeHeaderFilter(config, bundleContext);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testActivateInvalidMaxAge() throws Exception {
        createFilter(-1);
    }

    @Test
    public void testDoFilter() throws Exception {
        DispatcherMaxAgeHeaderFilter filter = createFilter(2000);
        filter.doFilter(request, response, chain);
        assertEquals("max-age=2000", response.getHeader("Cache-Control"));
    }
}
