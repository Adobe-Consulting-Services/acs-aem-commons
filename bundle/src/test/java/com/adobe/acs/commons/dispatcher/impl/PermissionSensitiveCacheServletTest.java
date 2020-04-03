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
package com.adobe.acs.commons.dispatcher.impl;

import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

import static junitx.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


@RunWith(MockitoJUnitRunner.class)
public class PermissionSensitiveCacheServletTest {

    public static final String TEST_PAGE = "/content/test.html";

    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.JCR_MOCK);

    @InjectMocks
    private PermissionSensitiveCacheServlet servlet = new PermissionSensitiveCacheServlet();

    MockSlingHttpServletRequest request;

    @Test
    public void doHeadShouldAllowAccess() throws Exception {

        context.create().resource( "/content/test" );

        request = context.request();

        Map<String,Object> requestMap = new HashMap<>();
        requestMap.put( "uri", TEST_PAGE );

        request.setParameterMap( requestMap );

        MockSlingHttpServletResponse response = context.response();

        servlet.doHead( request, response );

        assertEquals( HttpServletResponse.SC_OK, response.getStatus() );

    }

    @Test
    public void doHeadShouldNotAllowAccess() throws Exception {

        request = context.request();

        Map<String,Object> requestMap = new HashMap<>();
        requestMap.put( "uri", TEST_PAGE );

        request.setParameterMap( requestMap );

        MockSlingHttpServletResponse response = context.response();

        servlet.doHead( request, response );

        assertEquals( HttpServletResponse.SC_UNAUTHORIZED, response.getStatus() );

    }

    @Test
    public void testRequestUriNull(){

        String requestUri = null;

        boolean isValidUri = servlet.isUriValid( requestUri );

        assertFalse( isValidUri );

    }

    @Test
    public void testRequestUriRelative(){
        String requestUri = "dam/test.jpg";

        boolean isValidUri = servlet.isUriValid( requestUri );

        assertFalse( isValidUri );
    }

    @Test
    public void testRequestUriAbsolute(){

        boolean isValidUri = servlet.isUriValid( TEST_PAGE );

        assertTrue( isValidUri );

    }

}