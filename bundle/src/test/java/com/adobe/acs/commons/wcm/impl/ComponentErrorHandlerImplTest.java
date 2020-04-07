/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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

package com.adobe.acs.commons.wcm.impl;

import com.adobe.acs.commons.util.ModeUtil;
import com.adobe.acs.commons.wcm.ComponentErrorHandler;
import com.adobe.acs.commons.wcm.ComponentHelper;
import com.day.cq.wcm.api.WCMMode;
import com.day.cq.wcm.api.components.Component;
import com.day.cq.wcm.api.components.ComponentContext;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ComponentErrorHandlerImplTest {
    @Mock
    SlingHttpServletRequest request;

    @Mock
    SlingHttpServletResponse response;

    @Mock
    FilterChain chain;

    @Mock
    ComponentContext componentContext;

    @Mock
    PrintWriter responseWriter;

    @Mock
    Resource resource;

    @Mock
    ComponentHelper componentHelper;

    @Spy
    @InjectMocks
    ComponentErrorHandlerImpl handler = new ComponentErrorHandlerImpl();

    @Before
    public void setUp() throws Exception {
        when(request.getAttribute("com.day.cq.wcm.componentcontext")).thenReturn(componentContext);
        when(componentContext.getComponent()).thenReturn(mock(Component.class));
        when(request.getResource()).thenReturn(resource);

        when(resource.getPath()).thenReturn("/content/test");
        when(resource.getResourceType()).thenReturn("acs-commons/test/demo");
        when(resource.isResourceType("acs-commons/test/demo")).thenReturn(true);

        when(response.getWriter()).thenReturn(responseWriter);
        
        when(request.getRequestURI()).thenReturn("/content/page.html");
        when(response.getContentType()).thenReturn("text/html");
    }

    @After
    public void tearDown() throws Exception {
        reset(request, response, responseWriter, chain, resource, componentContext, componentHelper);
    }

    @Test
    public void testAccepts_suppressAttribute() throws Exception {
        when(request.getAttribute(ComponentErrorHandler.SUPPRESS_ATTR)).thenReturn(true);

        final boolean result = handler.accepts(request, response);
        assertFalse(result);
    }

    @Test
    public void testAccepts_suppressAttribute_selfSuppression() throws Exception {

        when(request.getAttribute(ComponentErrorHandler.SUPPRESS_ATTR)).thenReturn(false, true);
        when(componentContext.isRoot()).thenReturn(false);
        when(ModeUtil.isEdit(request)).thenReturn(true);

        doThrow(new ServletException()).when(chain).doFilter(request, response);

        boolean expectedResult = true;
        boolean result = !expectedResult;

        try {
            handler.doFilter(request, response, chain);
        } catch(ServletException ex) {
            result = true;
        }

        assertEquals(expectedResult, result);
        verify(responseWriter,never()).print(any(String.class));
        verifyNoMoreInteractions(responseWriter);
    }

    @Test
    public void testAccepts_suppressResourceTypes() throws Exception {
        final Map<String, String> config = new HashMap<String, String>();
        config.put("suppress-resource-types", "acs-commons/test/demo");

        handler.activate(config);

        final boolean result = handler.accepts(request, response);
        assertFalse(result);
    }

    @Test
    public void testNullComponentContext() throws Exception {
        handler.doFilter(request, response, chain);

        verify(chain, times(1)).doFilter(eq(request), eq(response));
        verify(responseWriter, never()).print(any(String.class));
        verifyNoMoreInteractions(chain);
    }

    @Test
    public void testRootComponentContext() throws Exception {
        when(componentContext.isRoot()).thenReturn(true);

        handler.doFilter(request, response, chain);

        verify(chain, times(1)).doFilter(eq(request), eq(response));
        verify(responseWriter, never()).print(any(String.class));
        verifyNoMoreInteractions(chain);
    }

    @Test
    public void testNoError() throws Exception {

        handler.doFilter(request, response, chain);

        verify(chain, times(1)).doFilter(eq(request), eq(response));
        verify(responseWriter, never()).print(any(String.class));
        verifyNoMoreInteractions(chain);
    }

    @Test
    public void testEditError() throws Exception {
        when(componentContext.isRoot()).thenReturn(false);
        when(request.getAttribute(WCMMode.class.getName())).thenReturn(WCMMode.EDIT);

        doThrow(new ServletException("Should not delegate to chained filters")).when(chain).doFilter(request, response);

        handler.doFilter(request, response, chain);

        verify(responseWriter, times(1)).print(any(String.class));
        verifyNoMoreInteractions(responseWriter);
    }



    @Test(expected = ServletException.class)
    public void testDisabledError_NotPreviouslyProcessedRequest() throws Exception {
        // This should not invoke ComponentErrorHandling
        when(request.getAttribute(ComponentErrorHandlerImpl.REQ_ATTR_PREVIOUSLY_PROCESSED)).thenReturn(null);
        when(ModeUtil.isDisabled(request)).thenReturn(true);

        doThrow(new ServletException()).when(chain).doFilter(request, response);

        handler.doFilter(request, response, chain);
        verify(responseWriter, never()).print(any(String.class));
        verifyNoMoreInteractions(responseWriter);

    }

    @Test(expected = ServletException.class)
    public void testDisabledError_PreviouslyProcessedRequest() throws Exception {
        // This should not invoke ComponentErrorHandling
        when(request.getAttribute(ComponentErrorHandlerImpl.REQ_ATTR_PREVIOUSLY_PROCESSED)).thenReturn(true);
        when(ModeUtil.isDisabled(request)).thenReturn(true);

        doThrow(new ServletException()).when(chain).doFilter(request, response);

        handler.doFilter(request, response, chain);

        verify(responseWriter, times(1)).print(any(String.class));
        verifyNoMoreInteractions(responseWriter);
    }
}
