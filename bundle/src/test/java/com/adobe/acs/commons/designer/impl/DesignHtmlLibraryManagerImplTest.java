package com.adobe.acs.commons.designer.impl;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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
import org.mockito.runners.MockitoJUnitRunner;

import com.adobe.acs.commons.designer.DesignHtmlLibraryManager;
import com.adobe.acs.commons.designer.PageRegion;
import com.day.cq.wcm.api.designer.Design;
import com.day.cq.widget.ClientLibrary;
import com.day.cq.widget.HtmlLibrary;
import com.day.cq.widget.HtmlLibraryManager;
import com.day.cq.widget.LibraryType;

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
    ArgumentCaptor<String[]> libraryCaptor;

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

        verify(verifyingLibraryManager, only()).writeIncludes(any(SlingHttpServletRequest.class),
                any(Writer.class), libraryCaptor.capture());
        verifyNoMoreInteractions(verifyingLibraryManager);

        assertEquals(4, libraryCaptor.getValue().length);
        assertArrayEquals(new Object[] { "css1", "css2", "js1", "js2" }, libraryCaptor.getValue());

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
            // TODO Auto-generated method stub

        }

        @Override
        public void writeJsInclude(SlingHttpServletRequest request, Writer out, String... categories)
                throws IOException {
            // TODO Auto-generated method stub

        }

        @Override
        public void writeIncludes(SlingHttpServletRequest request, Writer out, String... categories)
                throws IOException {
            verifyingLibraryManager.writeIncludes(request, out, categories);
        }

        @Override
        public void writeCssInclude(SlingHttpServletRequest request, Writer out, boolean themed,
                String... categories) throws IOException {
            // TODO Auto-generated method stub

        }

        @Override
        public void writeCssInclude(SlingHttpServletRequest request, Writer out, String... categories)
                throws IOException {
            // TODO Auto-generated method stub

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
    }

    /**
     * Mockito doesn't like varargs yet.
     * @see https://code.google.com/p/mockito/issues/detail?id=372
     *
     */
    interface NonVarArgsHtmlLibraryManager {
        void writeIncludes(SlingHttpServletRequest request, Writer out, String[] categories);
    }

}
