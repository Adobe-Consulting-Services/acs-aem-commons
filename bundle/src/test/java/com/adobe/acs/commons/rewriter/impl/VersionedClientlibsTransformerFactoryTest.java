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

package com.adobe.acs.commons.rewriter.impl;

import ch.qos.logback.classic.turbo.TurboFilter;
import com.adobe.granite.ui.clientlibs.HtmlLibrary;
import com.adobe.granite.ui.clientlibs.HtmlLibraryManager;
import com.adobe.granite.ui.clientlibs.LibraryType;

import junitx.util.PrivateAccessor;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.rewriter.ProcessingContext;
import org.apache.sling.rewriter.Transformer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

@RunWith(MockitoJUnitRunner.class)
public class VersionedClientlibsTransformerFactoryTest {
    @Mock
    private HtmlLibraryManager htmlLibraryManager;

    @Mock
    private HtmlLibrary htmlLibrary;

    @Mock
    private HtmlLibrary proxiedHtmlLibrary;

    @Mock
    private ContentHandler handler;

    @Mock
    private SlingHttpServletRequest slingRequest;

    @Mock
    private SlingHttpServletResponse slingResponse;

    @Mock
    private FilterChain filterChain;

    private VersionedClientlibsTransformerFactory factory;

    private Filter filter;

    private Transformer transformer;

    @Mock
    private ComponentContext componentContext;

    @Mock
    private BundleContext bundleContext;

    @Mock
    private ProcessingContext processingContext;

    @Mock
    private ResourceResolver resourceResolver;

    private final String PATH = "/etc/clientlibs/test";
    private final String FAKE_STREAM_CHECKSUM="fcadcfb01c1367e9e5b7f2e6d455ba8f"; // md5 of "I love strings"
    private final String PROXIED_FAKE_STREAM_CHECKSUM="669a712c318596cd7e7520e3e2000cfb"; // md5 of "I love strings when they are proxied"
    private final byte[] BYTES;
    private final java.io.InputStream INPUTSTREAM;
    private final String INPUTSTREAM_MD5;
    private final String PROXIED_PATH = "/apps/myco/test";
    private final String PROXY_PATH = "/etc.clientlibs/myco/test";


    public VersionedClientlibsTransformerFactoryTest() throws Exception {
        BYTES = "test".getBytes("UTF-8");
        INPUTSTREAM = new ByteArrayInputStream(BYTES);
        INPUTSTREAM_MD5 = DigestUtils.md5Hex(BYTES);
    }

    @Before
    public void setUp() throws Exception {
        when(componentContext.getBundleContext()).thenReturn(bundleContext);
        when(componentContext.getProperties()).thenReturn(new Hashtable<String, Object>());
        factory = new VersionedClientlibsTransformerFactory();
        filter = factory.new BadMd5VersionedClientLibsFilter();
        PrivateAccessor.setField(factory, "htmlLibraryManager", htmlLibraryManager);
        factory.activate(componentContext);

        when(htmlLibrary.getLibraryPath()).thenReturn(PATH);
        when(htmlLibrary.getInputStream()).thenReturn(new java.io.ByteArrayInputStream("I love strings".getBytes()));

        when(proxiedHtmlLibrary.getLibraryPath()).thenReturn(PROXIED_PATH);
        when(proxiedHtmlLibrary.getInputStream()).thenReturn(new java.io.ByteArrayInputStream("I love strings when they are proxied".getBytes()));

        when(processingContext.getRequest()).thenReturn(slingRequest);
        when(slingRequest.getResourceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.getSearchPath()).thenReturn(new String[] { "/libs/", "/apps/" });


        transformer = factory.createTransformer();
        transformer.init(processingContext, null);
        transformer.setContentHandler(handler);

        verifyNoMoreInteractions(bundleContext);
    }

    @After
    public void tearDown() throws Exception {
        reset(htmlLibraryManager, htmlLibrary, handler);
        transformer = null;
    }

    @Test
    public void testRegisterFilter() throws Exception {
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put("enforce.md5", Boolean.TRUE);
        when(componentContext.getProperties()).thenReturn(props);
        factory.activate(componentContext);
        verify(bundleContext).registerService(eq(Filter.class.getName()), any(Object.class), any(Dictionary.class));
    }

    @Test
    public void testNoop() throws Exception {

        when(htmlLibraryManager.getLibrary(eq(LibraryType.JS), eq(PATH))).thenReturn(htmlLibrary);

        final AttributesImpl in = new AttributesImpl();
        in.addAttribute("", "href", "", "CDATA", PATH + ".css");

        transformer.startElement(null, "a", null, in);

        ArgumentCaptor<Attributes> attributesCaptor = ArgumentCaptor.forClass(Attributes.class);

        verify(handler, only()).startElement(isNull(String.class), eq("a"), isNull(String.class),
                attributesCaptor.capture());

        assertEquals(PATH + ".css", attributesCaptor.getValue().getValue(0));
    }

    @Test
    public void testCSSClientLibrary() throws Exception {

        when(htmlLibraryManager.getLibrary(eq(LibraryType.CSS), eq(PATH))).thenReturn(htmlLibrary);

        final AttributesImpl in = new AttributesImpl();
        in.addAttribute("", "href", "", "CDATA", PATH + ".css");
        in.addAttribute("", "type", "", "CDATA", "text/css");
        in.addAttribute("", "rel", "", "CDATA", "stylesheet");

        transformer.startElement(null, "link", null, in);

        ArgumentCaptor<Attributes> attributesCaptor = ArgumentCaptor.forClass(Attributes.class);

        verify(handler, only()).startElement(isNull(String.class), eq("link"), isNull(String.class),
                attributesCaptor.capture());

        assertEquals(PATH + "."+ FAKE_STREAM_CHECKSUM +".css", attributesCaptor.getValue().getValue(0));
    }

    @Test
    public void testCSSClientLibraryWithMd5Enforce() throws Exception {
        PrivateAccessor.setField(factory, "enforceMd5", true);

        when(htmlLibraryManager.getLibrary(eq(LibraryType.CSS), eq(PATH))).thenReturn(htmlLibrary);

        final AttributesImpl in = new AttributesImpl();
        in.addAttribute("", "href", "", "CDATA", PATH + ".css");
        in.addAttribute("", "type", "", "CDATA", "text/css");
        in.addAttribute("", "rel", "", "CDATA", "stylesheet");

        transformer.startElement(null, "link", null, in);

        ArgumentCaptor<Attributes> attributesCaptor = ArgumentCaptor.forClass(Attributes.class);

        verify(handler, only()).startElement(isNull(String.class), eq("link"), isNull(String.class),
                attributesCaptor.capture());

        assertEquals(PATH + ".ACSHASH"+ FAKE_STREAM_CHECKSUM +".css", attributesCaptor.getValue().getValue(0));
    }

    @Test
    public void testCSSClientLibraryWithDot() throws Exception {
        final String path = PATH + ".foo";

        when(htmlLibraryManager.getLibrary(eq(LibraryType.CSS), eq(path))).thenReturn(htmlLibrary);
        when(htmlLibrary.getLibraryPath()).thenReturn(path);

        final AttributesImpl in = new AttributesImpl();
        in.addAttribute("", "href", "", "CDATA", path + ".css");
        in.addAttribute("", "type", "", "CDATA", "text/css");
        in.addAttribute("", "rel", "", "CDATA", "stylesheet");

        transformer.startElement(null, "link", null, in);

        ArgumentCaptor<Attributes> attributesCaptor = ArgumentCaptor.forClass(Attributes.class);

        verify(handler, only()).startElement(isNull(String.class), eq("link"), isNull(String.class),
                attributesCaptor.capture());

        assertEquals(path + "."+ FAKE_STREAM_CHECKSUM +".css", attributesCaptor.getValue().getValue(0));
    }

    @Test
    public void testMinifiedCSSClientLibrary() throws Exception {

        when(htmlLibraryManager.getLibrary(eq(LibraryType.CSS), eq(PATH))).thenReturn(htmlLibrary);

        final AttributesImpl in = new AttributesImpl();
        in.addAttribute("", "href", "", "CDATA", PATH + ".min.css");
        in.addAttribute("", "type", "", "CDATA", "text/css");
        in.addAttribute("", "rel", "", "CDATA", "stylesheet");

        transformer.startElement(null, "link", null, in);

        ArgumentCaptor<Attributes> attributesCaptor = ArgumentCaptor.forClass(Attributes.class);

        verify(handler, only()).startElement(isNull(String.class), eq("link"), isNull(String.class),
                attributesCaptor.capture());

        assertEquals(PATH + ".min."+ FAKE_STREAM_CHECKSUM +".css", attributesCaptor.getValue().getValue(0));
    }

    @Test
    public void testMinifiedCSSClientLibraryWithEnforceMd5() throws Exception {
        PrivateAccessor.setField(factory, "enforceMd5", true);

        when(htmlLibraryManager.getLibrary(eq(LibraryType.CSS), eq(PATH))).thenReturn(htmlLibrary);

        final AttributesImpl in = new AttributesImpl();
        in.addAttribute("", "href", "", "CDATA", PATH + ".min.css");
        in.addAttribute("", "type", "", "CDATA", "text/css");
        in.addAttribute("", "rel", "", "CDATA", "stylesheet");

        transformer.startElement(null, "link", null, in);

        ArgumentCaptor<Attributes> attributesCaptor = ArgumentCaptor.forClass(Attributes.class);

        verify(handler, only()).startElement(isNull(String.class), eq("link"), isNull(String.class),
                attributesCaptor.capture());

        assertEquals(PATH + ".min.ACSHASH"+ FAKE_STREAM_CHECKSUM +".css", attributesCaptor.getValue().getValue(0));
    }

    @Test
    public void testJavaScriptClientLibrary() throws Exception {

        when(htmlLibraryManager.getLibrary(eq(LibraryType.JS), eq(PATH))).thenReturn(htmlLibrary);

        final AttributesImpl in = new AttributesImpl();
        in.addAttribute("", "src", "", "CDATA", PATH + ".js");
        in.addAttribute("", "type", "", "CDATA", "text/javascript");

        transformer.startElement(null, "script", null, in);

        ArgumentCaptor<Attributes> attributesCaptor = ArgumentCaptor.forClass(Attributes.class);

        verify(handler, only()).startElement(isNull(String.class), eq("script"), isNull(String.class),
                attributesCaptor.capture());

        assertEquals(PATH + "."+ FAKE_STREAM_CHECKSUM +".js", attributesCaptor.getValue().getValue(0));
    }

    @Test
    public void testJavaScriptClientLibraryWithDot() throws Exception {
        final String path = PATH + ".foo";

        when(htmlLibraryManager.getLibrary(eq(LibraryType.JS), eq(path))).thenReturn(htmlLibrary);
        when(htmlLibrary.getLibraryPath()).thenReturn(path);

        final AttributesImpl in = new AttributesImpl();
        in.addAttribute("", "src", "", "CDATA", path + ".js");
        in.addAttribute("", "type", "", "CDATA", "text/javascript");

        transformer.startElement(null, "script", null, in);

        ArgumentCaptor<Attributes> attributesCaptor = ArgumentCaptor.forClass(Attributes.class);

        verify(handler, only()).startElement(isNull(String.class), eq("script"), isNull(String.class),
                attributesCaptor.capture());

        assertEquals(path + "."+ FAKE_STREAM_CHECKSUM +".js", attributesCaptor.getValue().getValue(0));
    }

    @Test
    public void testProxiedJavaScriptClientLibrary() throws Exception {

        when(htmlLibraryManager.getLibrary(eq(LibraryType.JS), eq(PROXIED_PATH))).thenReturn(proxiedHtmlLibrary);

        final AttributesImpl in = new AttributesImpl();
        in.addAttribute("", "src", "", "CDATA", PROXY_PATH + ".js");
        in.addAttribute("", "type", "", "CDATA", "text/javascript");

        transformer.startElement(null, "script", null, in);

        ArgumentCaptor<Attributes> attributesCaptor = ArgumentCaptor.forClass(Attributes.class);

        verify(handler, only()).startElement(isNull(String.class), eq("script"), isNull(String.class),
                attributesCaptor.capture());

        assertEquals(PROXY_PATH + "."+ PROXIED_FAKE_STREAM_CHECKSUM +".js", attributesCaptor.getValue().getValue(0));
    }



    @Test
    public void testMinifiedJavaScriptClientLibrary() throws Exception {

        when(htmlLibraryManager.getLibrary(eq(LibraryType.JS), eq(PATH))).thenReturn(htmlLibrary);

        final AttributesImpl in = new AttributesImpl();
        in.addAttribute("", "src", "", "CDATA", PATH + ".min.js");
        in.addAttribute("", "type", "", "CDATA", "text/javascript");

        transformer.startElement(null, "script", null, in);

        ArgumentCaptor<Attributes> attributesCaptor = ArgumentCaptor.forClass(Attributes.class);

        verify(handler, only()).startElement(isNull(String.class), eq("script"), isNull(String.class),
                attributesCaptor.capture());

        assertEquals(PATH + ".min."+ FAKE_STREAM_CHECKSUM +".js", attributesCaptor.getValue().getValue(0));
    }

    @Test
    public void testCSSClientLibraryWithInvalidExtension() throws Exception {

        when(htmlLibraryManager.getLibrary(eq(LibraryType.CSS), eq(PATH))).thenReturn(htmlLibrary);

        final AttributesImpl in = new AttributesImpl();
        in.addAttribute("", "href", "", "CDATA", PATH + ".styles");
        in.addAttribute("", "type", "", "CDATA", "text/css");
        in.addAttribute("", "rel", "", "CDATA", "stylesheet");

        transformer.startElement(null, "link", null, in);

        ArgumentCaptor<Attributes> attributesCaptor = ArgumentCaptor.forClass(Attributes.class);

        verify(handler, only()).startElement(isNull(String.class), eq("link"), isNull(String.class),
                attributesCaptor.capture());

        assertEquals(PATH + ".styles", attributesCaptor.getValue().getValue(0));
    }

     @Test
    public void testCSSClientLibraryWithRelAttributeValueDiffersFromStylesheet() throws Exception {

        when(htmlLibraryManager.getLibrary(eq(LibraryType.CSS), eq(PATH))).thenReturn(htmlLibrary);

        final AttributesImpl in = new AttributesImpl();
        in.addAttribute("", "href", "", "CDATA", PATH + ".css");
        in.addAttribute("", "type", "", "CDATA", "text/css");
        in.addAttribute("", "rel", "", "CDATA", "preload");

        transformer.startElement(null, "link", null, in);

        ArgumentCaptor<Attributes> attributesCaptor = ArgumentCaptor.forClass(Attributes.class);

        verify(handler, only()).startElement(isNull(String.class), eq("link"), isNull(String.class),
                attributesCaptor.capture());

        assertEquals(PATH + "."+ FAKE_STREAM_CHECKSUM +".css", attributesCaptor.getValue().getValue(0));
    }
    
    @Test
    public void testJavaScriptClientLibraryWithInvalidExtension() throws Exception {

        when(htmlLibraryManager.getLibrary(eq(LibraryType.JS), eq(PATH))).thenReturn(htmlLibrary);

        final AttributesImpl in = new AttributesImpl();
        in.addAttribute("", "src", "", "CDATA", PATH + ".vbs");
        in.addAttribute("", "type", "", "CDATA", "text/javascript");

        transformer.startElement(null, "script", null, in);

        ArgumentCaptor<Attributes> attributesCaptor = ArgumentCaptor.forClass(Attributes.class);

        verify(handler, only()).startElement(isNull(String.class), eq("script"), isNull(String.class),
                attributesCaptor.capture());

        assertEquals(PATH + ".vbs", attributesCaptor.getValue().getValue(0));
    }


    @Test
    public void testJavaScriptClientLibraryWithRelativePath() throws Exception {

        when(htmlLibraryManager.getLibrary(eq(LibraryType.JS), eq(PATH))).thenReturn(htmlLibrary);

        final AttributesImpl in = new AttributesImpl();
        in.addAttribute("", "src", "", "CDATA", "relative/script.js");
        in.addAttribute("", "type", "", "CDATA", "text/javascript");

        transformer.startElement(null, "script", null, in);

        ArgumentCaptor<Attributes> attributesCaptor = ArgumentCaptor.forClass(Attributes.class);

        verify(handler, only()).startElement(isNull(String.class), eq("script"), isNull(String.class),
                attributesCaptor.capture());

        assertEquals("relative/script.js", attributesCaptor.getValue().getValue(0));
    }

    @Test
    public void testJavaScriptClientLibraryWithSameSchemePath() throws Exception {

        when(htmlLibraryManager.getLibrary(eq(LibraryType.JS), eq(PATH))).thenReturn(htmlLibrary);

        final AttributesImpl in = new AttributesImpl();
        in.addAttribute("", "src", "", "CDATA", "//example.com/same/scheme/script.js");
        in.addAttribute("", "type", "", "CDATA", "text/javascript");

        transformer.startElement(null, "script", null, in);

        ArgumentCaptor<Attributes> attributesCaptor = ArgumentCaptor.forClass(Attributes.class);

        verify(handler, only()).startElement(isNull(String.class), eq("script"), isNull(String.class),
                attributesCaptor.capture());

        assertEquals("//example.com/same/scheme/script.js", attributesCaptor.getValue().getValue(0));
    }

    @Test
    public void testCSSClientLibraryWithSameSchemePath() throws Exception {

        when(htmlLibraryManager.getLibrary(eq(LibraryType.CSS), eq(PATH))).thenReturn(htmlLibrary);

        final AttributesImpl in = new AttributesImpl();
        in.addAttribute("", "href", "", "CDATA", "//example.com/same/scheme/styles.css");
        in.addAttribute("", "type", "", "CDATA", "text/css");
        in.addAttribute("", "rel", "", "CDATA", "stylesheet");

        transformer.startElement(null, "link", null, in);

        ArgumentCaptor<Attributes> attributesCaptor = ArgumentCaptor.forClass(Attributes.class);

        verify(handler, only()).startElement(isNull(String.class), eq("link"), isNull(String.class),
                attributesCaptor.capture());

        assertEquals("//example.com/same/scheme/styles.css", attributesCaptor.getValue().getValue(0));
    }

    @Test
    public void testJavaScriptClientLibraryWithDomainedPath() throws Exception {

        when(htmlLibraryManager.getLibrary(eq(LibraryType.JS), eq(PATH))).thenReturn(htmlLibrary);

        final AttributesImpl in = new AttributesImpl();
        in.addAttribute("", "src", "", "CDATA", "http://www.example.com/same/scheme/script.js");
        in.addAttribute("", "type", "", "CDATA", "text/javascript");

        transformer.startElement(null, "script", null, in);

        ArgumentCaptor<Attributes> attributesCaptor = ArgumentCaptor.forClass(Attributes.class);

        verify(handler, only()).startElement(isNull(String.class), eq("script"), isNull(String.class),
                attributesCaptor.capture());

        assertEquals("http://www.example.com/same/scheme/script.js", attributesCaptor.getValue().getValue(0));
    }

    @Test
    public void testCSSClientLibraryWithSameDomainedPath() throws Exception {

        when(htmlLibraryManager.getLibrary(eq(LibraryType.CSS), eq(PATH))).thenReturn(htmlLibrary);

        final AttributesImpl in = new AttributesImpl();
        in.addAttribute("", "href", "", "CDATA", "https://example.com/same/scheme/styles.css");
        in.addAttribute("", "type", "", "CDATA", "text/css");
        in.addAttribute("", "rel", "", "CDATA", "stylesheet");

        transformer.startElement(null, "link", null, in);

        ArgumentCaptor<Attributes> attributesCaptor = ArgumentCaptor.forClass(Attributes.class);

        verify(handler, only()).startElement(isNull(String.class), eq("link"), isNull(String.class),
                attributesCaptor.capture());

        assertEquals("https://example.com/same/scheme/styles.css", attributesCaptor.getValue().getValue(0));
    }

    @Test
    public void doFilter_nonJSCSS() throws Exception {
        when(slingRequest.getRequestURI()).thenReturn("/some_other/uri.html");
        filter.doFilter(slingRequest, slingResponse, filterChain);
        verifyNothingHappened();
    }

    @Test
    public void doFilter_notFoundInCache_md5Match() throws Exception {
        when(slingRequest.getRequestURI()).thenReturn("/etc/clientlibs/some.min.ACSHASH" + INPUTSTREAM_MD5 + ".js");

        HtmlLibrary library = mock(HtmlLibrary.class);
        when(library.getInputStream()).thenReturn(INPUTSTREAM);
        when(library.getLibraryPath()).thenReturn("/etc/clientlibs/some.js");
        when(htmlLibraryManager.getLibrary(LibraryType.JS, "/etc/clientlibs/some")).thenReturn(library);

        filter.doFilter(slingRequest, slingResponse, filterChain);

        verifyNo404();
    }

    @Test
    public void doFilter_notFoundInCache_md5MisMatch() throws Exception {
        when(slingRequest.getRequestURI()).thenReturn("/etc/clientlibs/some.min.ACSHASHfoobar.js");

        HtmlLibrary library = mock(HtmlLibrary.class);
        when(library.getInputStream()).thenReturn(INPUTSTREAM );
        when(library.getLibraryPath()).thenReturn("/etc/clientlibs/some.js");
        when(htmlLibraryManager.getLibrary(LibraryType.JS, "/etc/clientlibs/some")).thenReturn(library);

        filter.doFilter(slingRequest, slingResponse, filterChain);

        verify404();
    }

    @Test
    public void doFilter_notFoundInCacheWithDot_md5MisMatch() throws Exception {
        when(slingRequest.getRequestURI()).thenReturn("/etc/clientlibs/some.path.min.ACSHASHfoobar.js");

        HtmlLibrary library = mock(HtmlLibrary.class);
        when(library.getInputStream()).thenReturn(INPUTSTREAM );
        when(library.getLibraryPath()).thenReturn("/etc/clientlibs/some.path.js");
        when(htmlLibraryManager.getLibrary(LibraryType.JS, "/etc/clientlibs/some.path")).thenReturn(library);

        filter.doFilter(slingRequest, slingResponse, filterChain);

        verify404();
    }

    @Test
    public void doFilter_notFoundInCache_NoClientLib() throws Exception {
        when(slingRequest.getRequestURI()).thenReturn("/etc/clientlibs/some.min.foobar.js");

        filter.doFilter(slingRequest, slingResponse, filterChain);

        verifyNo404();
    }

    @Test
    public void doFilter_foundInCache_md5Match() throws Exception {
        when(slingRequest.getRequestURI()).thenReturn("/etc/clientlibs/some.min.ACSHASH" + INPUTSTREAM_MD5 + ".js");
        factory.getCache().put(new VersionedClientLibraryMd5CacheKey("/etc/clientlibs/some", LibraryType.JS), INPUTSTREAM_MD5);

        HtmlLibrary htmlLibrary = mock(HtmlLibrary.class);
        when(htmlLibrary.getLibraryPath()).thenReturn("/etc/clientlibs/some");
        when(htmlLibraryManager.getLibrary(LibraryType.JS, "/etc/clientlibs/some")).thenReturn(htmlLibrary);

        filter.doFilter(slingRequest, slingResponse, filterChain);

        verifyNo404();
    }

    @Test
    public void doFilter_foundInCacheWithDot_md5Match() throws Exception {
        when(slingRequest.getRequestURI()).thenReturn("/etc/clientlibs/some.path.min.ACSHASH" + INPUTSTREAM_MD5 + ".js");
        factory.getCache().put(new VersionedClientLibraryMd5CacheKey("/etc/clientlibs/some.path", LibraryType.JS), INPUTSTREAM_MD5);

        HtmlLibrary htmlLibrary = mock(HtmlLibrary.class);
        when(htmlLibrary.getLibraryPath()).thenReturn("/etc/clientlibs/some.path");
        when(htmlLibraryManager.getLibrary(LibraryType.JS, "/etc/clientlibs/some.path")).thenReturn(htmlLibrary);

        filter.doFilter(slingRequest, slingResponse, filterChain);

        verifyNo404();
    }

    @Test
    public void doFilter_foundInCache_md5MisMatch() throws Exception {
        when(slingRequest.getRequestURI()).thenReturn("/etc/clientlibs/some.min.ACSHASHfoobar.js");
        factory.getCache().put(new VersionedClientLibraryMd5CacheKey("/etc/clientlibs/some", LibraryType.JS), INPUTSTREAM_MD5);

        HtmlLibrary htmlLibrary = mock(HtmlLibrary.class);
        when(htmlLibrary.getLibraryPath()).thenReturn("/etc/clientlibs/some");
        when(htmlLibraryManager.getLibrary(LibraryType.JS, "/etc/clientlibs/some")).thenReturn(htmlLibrary);

        filter.doFilter(slingRequest, slingResponse, filterChain);

        verify404();
    }

    @Test
    public void doFilter_foundInCacheWithDot_md5MisMatch() throws Exception {
        when(slingRequest.getRequestURI()).thenReturn("/etc/clientlibs/some.path.min.ACSHASHfoobar.js");
        factory.getCache().put(new VersionedClientLibraryMd5CacheKey("/etc/clientlibs/some.path", LibraryType.JS), INPUTSTREAM_MD5);

        HtmlLibrary htmlLibrary = mock(HtmlLibrary.class);
        when(htmlLibrary.getLibraryPath()).thenReturn("/etc/clientlibs/some.path");
        when(htmlLibraryManager.getLibrary(LibraryType.JS, "/etc/clientlibs/some.path")).thenReturn(htmlLibrary);


        filter.doFilter(slingRequest, slingResponse, filterChain);

        verify404();
    }

    @Test
    public void doFilter_noMd5() throws Exception {
        when(slingRequest.getRequestURI()).thenReturn("/etc/clientlibs/some.min.js");
        filter.doFilter(slingRequest, slingResponse, filterChain);

        verifyNo404();
    }

    private void verifyNothingHappened() throws IOException, ServletException {
        verifyZeroInteractions(htmlLibraryManager);
        verifyNo404();
    }

    private void verifyNo404() throws IOException, ServletException {
        verify(filterChain).doFilter(slingRequest, slingResponse);
        verify(slingResponse, never()).sendError(anyInt());
    }

    private void verify404() throws IOException, ServletException {
        verify(filterChain, never()).doFilter(slingRequest, slingResponse);
        verify(slingResponse).sendError(HttpServletResponse.SC_NOT_FOUND);
    }
}
