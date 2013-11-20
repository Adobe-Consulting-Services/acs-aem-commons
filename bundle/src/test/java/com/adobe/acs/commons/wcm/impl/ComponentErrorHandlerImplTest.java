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

import com.adobe.acs.commons.wcm.ComponentHelper;
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
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import java.io.PrintWriter;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
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

    @InjectMocks
    ComponentErrorHandlerImpl handler = new ComponentErrorHandlerImpl();

    @Before
    public void setUp() throws Exception {
        when(request.getAttribute("com.day.cq.wcm.componentcontext")).thenReturn(componentContext);
        when(request.getResource()).thenReturn(resource);
        when(resource.getPath()).thenReturn("/content/test");

        when(response.getWriter()).thenReturn(responseWriter);
    }


    @After
    public void tearDown() throws Exception {
        reset(request, response, responseWriter, chain, resource, componentContext, componentHelper);
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
        when(componentHelper.isEditMode(request)).thenReturn(true);

        doThrow(new ServletException()).when(chain).doFilter(request, response);

        handler.doFilter(request, response, chain);

        verify(responseWriter, times(1)).print(any(String.class));
        verifyNoMoreInteractions(responseWriter);
    }
}
