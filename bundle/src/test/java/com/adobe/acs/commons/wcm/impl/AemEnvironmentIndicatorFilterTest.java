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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import com.day.cq.wcm.api.WCMMode;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.xss.XSSAPI;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
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
  
    public Map<String, Object> props = new HashMap<>();
  
  
    @Before
    public void setup() {
        AemEnvironmentIndicatorFilter aei = new AemEnvironmentIndicatorFilter();
        filter = spy(aei);
    
        context.registerService(XSSAPI.class, xss);
    
        props.clear();
        props.put("css-color", "blue");
        props.put("browser-title-prefix", "prefix");
    }
  
  
    @Test
    public void noHttpRequests() throws IOException, ServletException {
        ServletRequest req = mock(ServletRequest.class);
        ServletResponse resp = mock(ServletResponse.class);
    
        filter.doFilter(req, resp, chain);
        verify(chain, times(1)).doFilter(any(), any());
        verify(filter, never()).accepts(any());
    }
  
    @Test
    public void withNoConfiguration() throws IOException, ServletException {
        context.registerInjectActivateService(filter);
        filter.doFilter(context.request(), context.response(), chain);
    
        verify(chain, times(1)).doFilter(same(context.request()), same(context.response()));
        verify(filter, times(1)).accepts(same(context.request()));
    }
  
    @Test
    public void testDoFilterWhenAcceptsIsTrue() throws IOException, ServletException {
        context.registerInjectActivateService(filter, props);
        context.request().setMethod("GET");
        context.response().setContentType("text/html");
    
        doAnswer(invocation -> {
          HttpServletResponse response =
              (HttpServletResponse) invocation.getArguments()[1];
          response.getWriter().println("<html><body>somebody</body></html>");
          return null;
        }).when(chain).doFilter(any(), any());
    
        doReturn(true).when(filter).accepts(any());
    
        filter.doFilter(context.request(), context.response(), chain);
        String response = context.response().getOutputAsString();
        // assertThat(response).startsWith(
        // "<html><body>somebody<style>#acs-commons-env-indicator");
        assertTrue(startsWith("<html><body>somebody<style>#acs-commons-env-indicator")
            .matches(response));
        // assertThat(response).contains("background-color:blue");
        assertTrue(containsString("background-color:blue").matches(response));
    }
  
    @Test
    public void testDoFilterWhenAcceptsIsFalse() throws IOException, ServletException {
        context.registerInjectActivateService(filter, props);
        context.request().setMethod("GET");
        context.response().setContentType("text/html");
    
        doAnswer(invocation -> {
          HttpServletResponse response =
              (HttpServletResponse) invocation.getArguments()[1];
          response.getWriter().println("<html><body>somebody</body></html>");
          return null;
        }).when(chain).doFilter(any(), any());
    
        doReturn(false).when(filter).accepts(any());
    
        filter.doFilter(context.request(), context.response(), chain);
    
        verify(filter, never()).writeEnvironmentIndicator(any(), any(), any(), any());
    }
  
    @Test
    public void testWriteEnvironmentIndicator() {
        PrintWriter pw;
    
        pw = mock(PrintWriter.class);
        filter.writeEnvironmentIndicator("", "", "", pw);
        verify(pw, never()).write(any(String.class));
        verify(pw, never()).printf(any(), any());
    
        pw = mock(PrintWriter.class);
        filter.writeEnvironmentIndicator("css", "html", "", pw);
        verify(pw).write(eq("<style>css </style>"));
        verify(pw).write(eq("<div id=\"acs-commons-env-indicator\">html</div>"));
        verify(pw, never()).printf(any(), any());
    
        pw = mock(PrintWriter.class);
        filter.writeEnvironmentIndicator("", "", "prefix", pw);
        verify(pw, never()).write(any(String.class));
        verify(pw).printf(
            eq("<script>(function() { var c = 0; t = '%s' + ' | ' + document.title, "
                + "i = setInterval(function() { if (document.title === t && c++ > 10) { clearInterval(i); } else { document.title = t; } }, 1500); "
                + "document.title = t; })();</script>\n"),
            eq("prefix"));
    }
  
    @Test
    public void testAccept() {
        AemEnvironmentIndicatorFilter filter = mock(AemEnvironmentIndicatorFilter.class);
        // remember to call the real .accepts() method
        when(filter.accepts(any())).thenCallRealMethod();
    
        when(filter.isImproperlyConfigured(any(), any()))
            .thenReturn(true, false);
        when(filter.isUnsupportedRequestMethod(any()))
            .thenReturn(true, false);
        when(filter.isXhr(any()))
            .thenReturn(true, false);
        when(filter.hasAemEditorReferrer(any(), any()))
            .thenReturn(true, false);
        when(filter.isDisallowedWcmMode(any(), any()))
            .thenReturn(true, false);
    
        SlingHttpServletRequest mockRequest = mock(SlingHttpServletRequest.class);
    
        // isImproperlyConfigured returns true
        assertFalse(filter.accepts(mockRequest));
        // isUnsupportedRequestMethod returns true
        assertFalse(filter.accepts(mockRequest));
        // isAnXhrRequest returns true
        assertFalse(filter.accepts(mockRequest));
        // hasAemEditorReferrer returns true
        assertFalse(filter.accepts(mockRequest));
        // isDisallowedWcmMode returns true
        assertFalse(filter.accepts(mockRequest));
        // all checks return false
        assertTrue(filter.accepts(mockRequest));
    }
  
    @Test
    public void testIsImproperlyConfigured() {
        assertFalse(filter.isImproperlyConfigured("not-blank", "not-blank"));
        assertFalse(filter.isImproperlyConfigured("", "not-blank"));
        assertFalse(filter.isImproperlyConfigured("not-blank", ""));
        assertTrue(filter.isImproperlyConfigured("", ""));
    }
  
    @Test
    public void testIsUnsupportedRequestMethod() {
        assertFalse(filter.isUnsupportedRequestMethod("get"));
        assertTrue(filter.isUnsupportedRequestMethod("not-get"));
    }
  
    @Test
    public void testIsXhr() {
        assertFalse(filter.isXhr("not-XMLHttpRequest"));
        assertTrue(filter.isXhr("XMLHttpRequest"));
    }
  
    @Test
    public void testHasAemEditorReferrer() {
        // not an editor referrer
        assertFalse(filter.hasAemEditorReferrer("/not-cf-or-editor", "/does-not-matter"));
        // request uri does not match referrer editor.html suffix
        assertFalse(filter.hasAemEditorReferrer("/editor.html/uri", "/other-uri"));
        // editor.html with matching uri
        assertTrue(filter.hasAemEditorReferrer("/editor.html/uri", "/uri"));
        // is /cf
        assertTrue(filter.hasAemEditorReferrer("/cf", "/does-not-matter"));
    }
  
    @Test
    public void testIsDisallowedWcmMode() {
        String[] excludedWcmModes = {
            "edit"
        };
    
        // WCMMode does not exist in array
        assertFalse(filter.isDisallowedWcmMode(WCMMode.DESIGN, excludedWcmModes));
        // WCMMode exists in array
        assertTrue(filter.isDisallowedWcmMode(WCMMode.EDIT, excludedWcmModes));
    }
  
    @Test
    public void testActivate() {
        when(filter.shouldUseBaseCss(anyBoolean(), any(), any())).thenReturn(false);
        when(filter.shouldUseColorCss(anyBoolean(), any(), any())).thenReturn(false);
    
        context.registerInjectActivateService(filter);
    
        verify(filter, never()).createBaseCss();
        verify(filter, never()).createColorCss(any());
    
        // when
        when(filter.shouldUseBaseCss(anyBoolean(), any(), any())).thenReturn(true);
        when(filter.shouldUseColorCss(anyBoolean(), any(), any())).thenReturn(true);
    
        context.registerInjectActivateService(filter);
    
        verify(filter).createBaseCss();
        verify(filter).createColorCss(any());
    }
  
    @Test
    public void testCreateBaseCss() {
        assertEquals(filter.createBaseCss(),
            "#acs-commons-env-indicator { "
                + ";background-image:url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUAAAAFCAYAAACNbyblAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAA3NpVFh0WE1MOmNvbS5hZG9iZS54bXAAAAAAADw/eHBhY2tldCBiZWdpbj0i77u/IiBpZD0iVzVNME1wQ2VoaUh6cmVTek5UY3prYzlkIj8+IDx4OnhtcG1ldGEgeG1sbnM6eD0iYWRvYmU6bnM6bWV0YS8iIHg6eG1wdGs9IkFkb2JlIFhNUCBDb3JlIDUuNS1jMDIxIDc5LjE1NDkxMSwgMjAxMy8xMC8yOS0xMTo0NzoxNiAgICAgICAgIj4gPHJkZjpSREYgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIj4gPHJkZjpEZXNjcmlwdGlvbiByZGY6YWJvdXQ9IiIgeG1sbnM6eG1wTU09Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9tbS8iIHhtbG5zOnN0UmVmPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvc1R5cGUvUmVzb3VyY2VSZWYjIiB4bWxuczp4bXA9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC8iIHhtcE1NOk9yaWdpbmFsRG9jdW1lbnRJRD0ieG1wLmRpZDo5ZmViMDk1Ni00MTMwLTQ0NGMtYWM3Ny02MjU0NjY0OTczZWIiIHhtcE1NOkRvY3VtZW50SUQ9InhtcC5kaWQ6MDk4RTBGQkYzMjA5MTFFNDg5MDFGQzVCQkEyMjY0NDQiIHhtcE1NOkluc3RhbmNlSUQ9InhtcC5paWQ6MDk4RTBGQkUzMjA5MTFFNDg5MDFGQzVCQkEyMjY0NDQiIHhtcDpDcmVhdG9yVG9vbD0iQWRvYmUgUGhvdG9zaG9wIENDIChNYWNpbnRvc2gpIj4gPHhtcE1NOkRlcml2ZWRGcm9tIHN0UmVmOmluc3RhbmNlSUQ9InhtcC5paWQ6Mjc5NmRkZmItZDVlYi00N2RlLWI1NDMtNDgxNzU2ZjIwZDc1IiBzdFJlZjpkb2N1bWVudElEPSJ4bXAuZGlkOjlmZWIwOTU2LTQxMzAtNDQ0Yy1hYzc3LTYyNTQ2NjQ5NzNlYiIvPiA8L3JkZjpEZXNjcmlwdGlvbj4gPC9yZGY6UkRGPiA8L3g6eG1wbWV0YT4gPD94cGFja2V0IGVuZD0iciI/Ps64/vsAAAAkSURBVHjaYvz//z8DGjBmAAkiYWOwInQBZEFjZB0YAiAMEGAAVBk/wkPTSYQAAAAASUVORK5CYII=');"
                + "border-bottom: 1px solid rgba(0, 0, 0, .25);"
                + "box-sizing: border-box;"
                + "-moz-box-sizing: border-box;"
                + "-webkit-box-sizing: border-box;"
                + "position: fixed;"
                + "left: 0;"
                + "top: 0;"
                + "right: 0;"
                + "height: 5px;"
                + "z-index: 100000000000000;"
                + " }");
    }
  
    @Test
    public void testCreateColorCss() {
        assertEquals(filter.createColorCss("red"),
            "#acs-commons-env-indicator { "
                + "background-color:red;"
                + " }");
    }
  
    @Test
    public void testShouldWriteBaseCss() {
        // "alwaysInclude is true
        assertTrue(filter.shouldUseBaseCss(true, null, null));
        // alwaysInclude is false, css blank, color not blank
        assertTrue(filter.shouldUseBaseCss(false, "", "color"));
        // alwaysInclude is false, css blank, color blank
        assertFalse(filter.shouldUseBaseCss(false, "", ""));
        // alwaysInclude is false, css not blank
        assertFalse(filter.shouldUseBaseCss(false, "css", "color"));
    }
  
    @Test
    public void testShouldWriteColorCss() {
        // "alwaysInclude is true
        assertTrue(filter.shouldUseColorCss(true, null, null));
        // "alwaysInclude is false, css blank, color not blank
        assertTrue(filter.shouldUseColorCss(false, "", "color"));
        // "alwaysInclude is false, css blank, color blank
        assertFalse(filter.shouldUseColorCss(false, "", ""));
        // "alwaysInclude is false, css not blank
        assertFalse(filter.shouldUseColorCss(false, "css", "color"));
    }
}
