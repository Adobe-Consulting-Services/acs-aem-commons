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

import com.day.cq.widget.HtmlLibrary;
import com.day.cq.widget.HtmlLibraryManager;
import com.day.cq.widget.LibraryType;

import junitx.util.PrivateAccessor;

import org.apache.sling.rewriter.Transformer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.configuration.InjectingAnnotationEngine;
import org.mockito.runners.MockitoJUnitRunner;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

@RunWith(MockitoJUnitRunner.class)
public class VersionedClientlibsTransformerFactoryTest {
    @Mock
    private HtmlLibraryManager htmlLibraryManager;

    @Mock
    private HtmlLibrary htmlLibrary;

    @Mock
    private ContentHandler handler;

    private VersionedClientlibsTransformerFactory factory;

    private Transformer transformer;

    private final String PATH = "/etc/clientlibs/test";
    private final String FAKE_STREAM_CHECKSUM="fcadcfb01c1367e9e5b7f2e6d455ba8f";

    @Before
    public void setUp() throws Exception {
        factory = new VersionedClientlibsTransformerFactory();
        PrivateAccessor.setField(factory, "htmlLibraryManager", htmlLibraryManager);
        factory.activate(Collections.<String, Object>emptyMap());

        when(htmlLibrary.getLibraryPath()).thenReturn(PATH);
        when(htmlLibrary.getInputStream()).thenReturn(new java.io.ByteArrayInputStream("I love strings".getBytes()));
        //when(htmlLibrary.getLastModified()).thenReturn(123L);

        transformer = factory.createTransformer();
        transformer.setContentHandler(handler);
    }

    @After
    public void tearDown() throws Exception {
        reset(htmlLibraryManager, htmlLibrary, handler);
        transformer = null;
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
}
