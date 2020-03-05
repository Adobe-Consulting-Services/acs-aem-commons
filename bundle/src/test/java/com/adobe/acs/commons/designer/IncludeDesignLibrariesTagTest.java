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
package com.adobe.acs.commons.designer;

import static org.mockito.Mockito.*;

import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.day.cq.wcm.api.designer.Design;

@RunWith(MockitoJUnitRunner.class)
public class IncludeDesignLibrariesTagTest {
    
    @Mock
    private PageContext pageContext;
    
    @Mock
    private SlingHttpServletRequest request;
    
    @Mock
    private DesignHtmlLibraryManager dhlm;
    
    @Mock
    private SlingBindings slingBindings;
    
    @Mock
    private SlingScriptHelper sling;
    
    @Mock
    private Design currentDesign;
    
    @Mock
    private Design otherDesign;
    
    @Mock
    private JspWriter writer;
    
    @InjectMocks
    private IncludeDesignLibrariesTag tag = new IncludeDesignLibrariesTag();

    @Before
    public void setUp() throws Exception {
        when(pageContext.getRequest()).thenReturn(request);
        when(pageContext.getOut()).thenReturn(writer);
        when(request.getAttribute(SlingBindings.class.getName())).thenReturn(slingBindings);
        when(slingBindings.getSling()).thenReturn(sling);
        when(sling.getService(DesignHtmlLibraryManager.class)).thenReturn(dhlm);
        when(pageContext.getAttribute("currentDesign")).thenReturn(currentDesign);
    }

    @Test
    public void test_css_and_js() throws Exception {
        tag.setCss(true);
        tag.setJs(true);
        tag.setRegion("head");
        tag.doEndTag();
        verify(dhlm, only()).writeIncludes(request, currentDesign, PageRegion.HEAD, writer);
    }

    @Test
    public void test_css_only() throws Exception {
        tag.setCss(true);
        tag.setRegion("head");
        tag.doEndTag();
        verify(dhlm, only()).writeCssInclude(request, currentDesign, PageRegion.HEAD, writer);
    }

    @Test
    public void test_js_only() throws Exception {
        tag.setJs(true);
        tag.setRegion("head");
        tag.doEndTag();
        verify(dhlm, only()).writeJsInclude(request, currentDesign, PageRegion.HEAD, writer);
    }

    @Test
    public void test_nothing_included() throws Exception {
        tag.setRegion("head");
        tag.doEndTag();
        verifyNoMoreInteractions(dhlm);
    }

    @Test
    public void test_alternate_design() throws Exception {
        tag.setJs(true);
        tag.setRegion("head");
        tag.setDesign(otherDesign);
        tag.doEndTag();
        verify(dhlm, only()).writeJsInclude(request, otherDesign, PageRegion.HEAD, writer);
    }

    @Test
    public void test_without_dhlm() throws Exception {
        when(sling.getService(DesignHtmlLibraryManager.class)).thenReturn(null);
        tag.setJs(true);
        tag.setRegion("head");
        tag.doEndTag();
        verifyNoMoreInteractions(dhlm);
    }

    @Test
    public void testRelease() throws Exception {
        tag.setJs(true);
        tag.release();
        tag.setRegion("head");
        tag.doEndTag();
        verifyNoMoreInteractions(dhlm);
    }


}
