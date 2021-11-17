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
package com.adobe.acs.commons.designer.impl;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;

import junitx.util.PrivateAccessor;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.adobe.acs.commons.designer.DesignHtmlLibraryManager;
import com.adobe.acs.commons.designer.PageRegion;
import com.day.cq.wcm.api.designer.Design;
import com.adobe.granite.ui.clientlibs.ClientLibrary;
import com.adobe.granite.ui.clientlibs.HtmlLibrary;
import com.adobe.granite.ui.clientlibs.HtmlLibraryManager;
import com.adobe.granite.ui.clientlibs.LibraryType;

@RunWith(MockitoJUnitRunner.class)
public class DesignHtmlLibraryManagerImplTest {

    @Mock
    private NonVarArgsHtmlLibraryManager verifyingLibraryManager;

    @InjectMocks
    private DesignHtmlLibraryManagerImpl instance;

    @Mock
    private SlingHttpServletRequest request;

    @Mock
    private Design design;

    @Mock
    private Resource designContentResource;

    @Mock
    private Writer writer;

    @Captor
    ArgumentCaptor<String[]> jsLibraryCaptor;

    @Captor
    ArgumentCaptor<String[]> cssLibraryCaptor;

    @Before
    public void setUp() throws Exception {
        HtmlLibraryManager bridge = new BridgeHtmlLibraryManager();
        PrivateAccessor.setField(instance, "htmlLibraryManager", bridge);
    }

    @Test
    public void testWriteIncludesIncludesCssAndJs() throws IOException {
        when(design.getContentResource()).thenReturn(designContentResource);
        setLibraries(designContentResource, PageRegion.HEAD, new String[] { "css1", "css2" }, new String[] {
                "js1", "js2" });

        instance.writeIncludes(request, design, PageRegion.HEAD, writer);

        verify(verifyingLibraryManager).writeCssInclude(any(SlingHttpServletRequest.class),
                any(Writer.class), cssLibraryCaptor.capture());
        verify(verifyingLibraryManager).writeJsInclude(any(SlingHttpServletRequest.class),
                any(Writer.class), jsLibraryCaptor.capture());
        verifyNoMoreInteractions(verifyingLibraryManager);

        assertEquals(2, jsLibraryCaptor.getValue().length);
        assertArrayEquals(new Object[] { "js1", "js2" }, jsLibraryCaptor.getValue());

        assertEquals(2, cssLibraryCaptor.getValue().length);
        assertArrayEquals(new Object[] { "css1", "css2" }, cssLibraryCaptor.getValue());

    }

    @Test
    public void testGetLibrariesDoesDeDuplication() throws IOException {
        when(design.getContentResource()).thenReturn(designContentResource);
        setLibraries(designContentResource, PageRegion.HEAD, new String[] { "css1", "cssandjs1" }, new String[] {
                "js1", "cssandjs1" });

        String[] categories = instance.getLibraries(design,  PageRegion.HEAD);

        assertArrayEquals(new Object[] { "css1", "cssandjs1", "js1" }, categories);

    }

    private void setLibraries(Resource r, PageRegion region, String[] css, String[] js) {
        Resource child = mock(Resource.class);

        Map<String, Object> values = new HashMap<String, Object>();
        values.put(DesignHtmlLibraryManager.PROPERTY_CSS, css);
        values.put(DesignHtmlLibraryManager.PROPERTY_JS, js);
        when(child.adaptTo(ValueMap.class)).thenReturn(new ValueMapDecorator(values));

        when(r.getChild(DesignHtmlLibraryManager.RESOURCE_NAME + "/" + region.toString())).thenReturn(child);
    }

    private final class BridgeHtmlLibraryManager implements HtmlLibraryManager {
        @Override
        public void writeThemeInclude(SlingHttpServletRequest request, Writer out, String... categories)
                throws IOException {
            // TODO Auto-generated method stub

        }

        @Override
        public void writeJsInclude(SlingHttpServletRequest request, Writer out, boolean themed,
                String... categories) throws IOException {
            // TODO
        }

        @Override
        public void writeJsInclude(SlingHttpServletRequest request, Writer out, String... categories)
                throws IOException {
            verifyingLibraryManager.writeJsInclude(request, out, categories);
        }

        @Override
        public void writeIncludes(SlingHttpServletRequest request, Writer out, String... categories)
                throws IOException {
         // TODO Auto-generated method stub
        }

        @Override
        public void writeCssInclude(SlingHttpServletRequest request, Writer out, boolean themed,
                String... categories) throws IOException {
            // TODO Auto-generated method stub

        }

        @Override
        public void writeCssInclude(SlingHttpServletRequest request, Writer out, String... categories)
                throws IOException {
            verifyingLibraryManager.writeCssInclude(request, out, categories);
        }

        @Override
        public boolean isMinifyEnabled() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isGzipEnabled() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isDebugEnabled() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public Collection<ClientLibrary> getThemeLibraries(String[] categories, LibraryType type,
                String themeName, boolean transitive) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public HtmlLibrary getLibrary(LibraryType type, String path) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public HtmlLibrary getLibrary(SlingHttpServletRequest request) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Collection<ClientLibrary> getLibraries(String[] categories, LibraryType type, boolean ignoreThemed,
                boolean transitive) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Map<String, ClientLibrary> getLibraries() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void invalidateOutputCache() throws RepositoryException {
            // TODO Auto-generated method stub
            
        }
    }

}
