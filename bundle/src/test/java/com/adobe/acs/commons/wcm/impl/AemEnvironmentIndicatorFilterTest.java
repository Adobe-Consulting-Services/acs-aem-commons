/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2019 Adobe
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.xss.XSSAPI;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.day.cq.wcm.api.WCMMode;
import com.google.common.collect.ImmutableMap;

import io.wcm.testing.mock.aem.junit.AemContext;

@RunWith(MockitoJUnitRunner.class)
public class AemEnvironmentIndicatorFilterTest {
    
    @Rule
    public AemContext context = new AemContext();
    
    @Mock
    FilterChain chain;
    
    @Mock
    XSSAPI xss;
    
    public AemEnvironmentIndicatorFilter filter;
    
    public Map<String,Object> props = new HashMap<>();
    
    
    @Before
    public void setup() {
        AemEnvironmentIndicatorFilter aei = new AemEnvironmentIndicatorFilter();
        filter = spy(aei);
        
        context.registerService(XSSAPI.class,xss);
        
        props.put("css-color", "blue");
        props.put("browser-title-prefix", "prefix");
    }
    
    
    @Test
    public void noHttpRequests() throws IOException, ServletException {
        ServletRequest req = mock(ServletRequest.class);
        ServletResponse resp = mock(ServletResponse.class);
        
        filter.doFilter(req,resp,chain);
        verify(chain, times(1)).doFilter(any(), any());
        verify(filter, never()).accepts(any());
    }
    
    @Test
    public void withNoConfiguration() throws IOException, ServletException {
        context.registerInjectActivateService(filter);
        filter.doFilter(context.request(), context.response(), chain);
        
        verify(chain,times(1)).doFilter(same(context.request()),same(context.response()));
        verify(filter,times(1)).accepts(same(context.request()));
    }
    
    
    @Test
    public void testFiltering() throws IOException, ServletException {
        context.registerInjectActivateService(filter,props);
        context.request().setMethod("GET");  
        context.response().setContentType("text/html");
//        context.response().getWriter().println("<html><body>somebody</body></html>");
        
        doAnswer((Answer) invocation -> {
            HttpServletResponse response = (HttpServletResponse) invocation.getArguments()[1];
            response.getWriter().println("<html><body>somebody</body></html>");
            return null;
        }).when(chain).doFilter(any(), any());
        
        filter.doFilter(context.request(), context.response(), chain);
        String response = context.response().getOutputAsString();
        assertTrue(response.startsWith("<html><body>somebody<style>#acs-commons-env-indicator"));
        assertTrue(response.contains("background-color:blue"));
    }
    
    @Test
    public void testDisallowedWcmMode() throws IOException, ServletException {
        props.put("excluded-wcm-modes","edit");
        context.registerInjectActivateService(filter,props);
        context.request().setMethod("GET");  
        context.response().setContentType("text/html");
        WCMMode.EDIT.toRequest(context.request());
        doAnswer((Answer) invocation -> {
            HttpServletResponse response = (HttpServletResponse) invocation.getArguments()[1];
            response.getWriter().println("<html><body>somebody</body></html>");
            return null;
        }).when(chain).doFilter(any(), any());
        filter.doFilter(context.request(), context.response(), chain);
        String response = context.response().getOutputAsString();
        assertEquals(String.format("%s%n", "<html><body>somebody</body></html>"),response);
    }
    
    @Test
    public void testAllowedWcmMode() throws IOException, ServletException {
        props.put("excluded-wcm-modes","edit");
        context.registerInjectActivateService(filter,props);
        context.request().setMethod("GET");  
        context.response().setContentType("text/html");
        WCMMode.PREVIEW.toRequest(context.request()); 
        doAnswer((Answer) invocation -> {
            HttpServletResponse response = (HttpServletResponse) invocation.getArguments()[1];
            response.getWriter().println("<html><body>somebody</body></html>");
            return null;
        }).when(chain).doFilter(any(), any());
        filter.doFilter(context.request(), context.response(), chain);
        String response = context.response().getOutputAsString();
        assertTrue(response.startsWith("<html><body>somebody<style>#acs-commons-env-indicator"));
        assertTrue(response.contains("background-color:blue"));
    }
    
    
    
    
    @Test
    public void testAcceptForMethod() {
        context.registerInjectActivateService(filter,props);
        context.request().setMethod("bla");
        assertFalse(filter.accepts(context.request()));
        context.request().setMethod("GET");
        assertTrue(filter.accepts(context.request()));
    }
    
    @Test
    public void testAcceptForXRequestedWithHeader() {
        context.registerInjectActivateService(filter,props);
        context.request().setMethod("GET");
        context.request().setHeader("X-Requested-With", "XMLHttpRequest");
        assertFalse(filter.accepts(context.request()));
        context.request().setHeader("X-Requested-With", "somethingElse");
        assertTrue(filter.accepts(context.request()));
    }
    
    @Test
    public void testAcceptForReferrers() {
        context.build().resource("/resource", ImmutableMap.of("jcr:title","title")).commit();
        Resource resource = context.resourceResolver().getResource("/resource");
        context.registerInjectActivateService(filter,props);
        context.request().setMethod("GET");
        context.request().setServletPath("/resource");
        
        // do not work on pages displayed in the Touch editor
        context.request().setHeader("Referer", "/editor.html/resource");
        assertFalse(filter.accepts(context.request()));
        
        // decorate pages inside the classic content finder
        context.request().setHeader("Referer", "/cf#/resource");
        assertTrue(filter.accepts(context.request()));
        
        context.request().setHeader("Referer", "/cf");
        assertFalse(filter.accepts(context.request()));

    }

}
