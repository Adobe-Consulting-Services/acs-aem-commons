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

import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.rewriter.ProcessingComponentConfiguration;
import org.apache.sling.rewriter.ProcessingContext;
import org.apache.sling.rewriter.Transformer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.adobe.granite.ui.clientlibs.HtmlLibrary;
import com.adobe.granite.ui.clientlibs.HtmlLibraryManager;
import com.adobe.granite.ui.clientlibs.LibraryType;

import junitx.util.PrivateAccessor;

@RunWith(MockitoJUnitRunner.class)
public class StylesheetInlinerTransformerFactoryTest {
    

    @Mock
    private HtmlLibraryManager htmlLibraryManager;

    @Mock
    private HtmlLibrary htmlLibrary;

    @Mock
    private ContentHandler handler;
    
    @Mock
    private ProcessingContext processingContext;
    
    @Mock
    private SlingHttpServletRequest slingRequest;
    
    @Mock
    private ResourceResolver resourceResolver;
    
    @Mock 
    private Resource resource;
    
    
    private StylesheetInlinerTransformerFactory factory;
    
    private Transformer transformer;

    private Attributes empty = new AttributesImpl();
    
    private static final String CLIENTLIB_PATH = "/etc/clientlibs/test";
    private static final String CSS_RESOURCE_PATH = "/etc/assets/somecss";
    private static final String NON_EXISTING_PATH = "/etc/assets/doesntexist";
    
    private static final String CSS_CONTENTS = "div {display:block;}";
    private static final String NEWLINE = "\n";

    
    private static final String TEST_DATA = "some test data";
    

    @Before
    public void setUp() throws Throwable {
        factory = new StylesheetInlinerTransformerFactory();
        PrivateAccessor.setField(factory, "htmlLibraryManager", htmlLibraryManager);

        when(htmlLibrary.getInputStream()).thenReturn(new java.io.ByteArrayInputStream(CSS_CONTENTS.getBytes()));
        when(htmlLibraryManager.getLibrary(eq(LibraryType.CSS), eq(CLIENTLIB_PATH))).thenReturn(htmlLibrary);
        when(htmlLibraryManager.getLibrary(eq(LibraryType.CSS), not(eq(CLIENTLIB_PATH)))).thenReturn(null);
        when( slingRequest.getRequestURL()).thenReturn(new StringBuffer("testing"));
        when(resource.adaptTo(eq(InputStream.class))).thenReturn(new java.io.ByteArrayInputStream(CSS_CONTENTS.getBytes()));
        when(resourceResolver.getResource(eq(CSS_RESOURCE_PATH + ".css" ))).thenReturn(resource);
        when(resourceResolver.getResource(eq(NON_EXISTING_PATH))).thenReturn(null);
        when(slingRequest.getResourceResolver()).thenReturn(resourceResolver);
        when(processingContext.getRequest()).thenReturn(slingRequest);
        
        transformer = factory.createTransformer();
        PrivateAccessor.invoke(transformer, "init", 
                new Class[] {ProcessingContext.class, ProcessingComponentConfiguration.class}, 
                new Object[] {processingContext, null} );
        transformer.setContentHandler(handler);
    }

    @After
    public void tearDown() throws Exception {
        reset(htmlLibraryManager, htmlLibrary, handler);
        transformer = null;
    }

    @Test
    public void testNoop() throws Exception {

        startHeadSection(empty);
        startBodySection(empty);
        endBodySection();

        verify(handler).startElement(isNull(String.class), eq("html"), isNull(String.class), eq(empty));
        verify(handler).startElement(isNull(String.class), eq("head"), isNull(String.class), eq(empty));
        verify(handler).endElement(isNull(String.class), eq("head"), isNull(String.class));
        verify(handler).startElement(isNull(String.class), eq("body"), isNull(String.class), eq(empty));
        verify(handler).endElement(isNull(String.class), eq("body"), isNull(String.class));
        verify(handler).endElement(isNull(String.class), eq("html"), isNull(String.class));
    }

    
    @Test
    public void testClientLibReferenceInHead() throws Exception {

        startHeadSection(empty);
        addStylesheetLink(CLIENTLIB_PATH);
        startBodySection(empty);
        endBodySection();

        verify(handler).startElement(isNull(String.class), eq("html"), isNull(String.class), eq(empty));
        verify(handler).startElement(isNull(String.class), eq("head"), isNull(String.class), eq(empty));
        verifyInlineStyle();
        verify(handler).endElement(isNull(String.class), eq("head"), isNull(String.class));
        verify(handler).startElement(isNull(String.class), eq("body"), isNull(String.class), eq(empty));
        verify(handler).endElement(isNull(String.class), eq("body"), isNull(String.class));
        verify(handler).endElement(isNull(String.class), eq("html"), isNull(String.class));
    }

    @Test
    public void testClientLibReferenceInBody() throws Exception {

        startHeadSection(empty);
        startBodySection(empty);
        addDiv(empty);
        addStylesheetLink(CLIENTLIB_PATH);
        endBodySection();

        verify(handler).startElement(isNull(String.class), eq("html"), isNull(String.class), eq(empty));
        verify(handler).startElement(isNull(String.class), eq("head"), isNull(String.class), eq(empty));
        verify(handler).endElement(isNull(String.class), eq("head"), isNull(String.class));
        verify(handler).startElement(isNull(String.class), eq("body"), isNull(String.class), eq(empty));
        verifyDiv();
        verifyInlineStyle();
        verify(handler).endElement(isNull(String.class), eq("body"), isNull(String.class));
        verify(handler).endElement(isNull(String.class), eq("html"), isNull(String.class));
    }


    @Test
    public void testResourceReferenceInHead() throws Exception {

        startHeadSection(empty);
        addStylesheetLink(CSS_RESOURCE_PATH);
        startBodySection(empty);
        endBodySection();

        verify(handler).startElement(isNull(String.class), eq("html"), isNull(String.class), eq(empty));
        verify(handler).startElement(isNull(String.class), eq("head"), isNull(String.class), eq(empty));
        verifyInlineStyle();
        verify(handler).endElement(isNull(String.class), eq("head"), isNull(String.class));
        verify(handler).startElement(isNull(String.class), eq("body"), isNull(String.class), eq(empty));
        verify(handler).endElement(isNull(String.class), eq("body"), isNull(String.class));
        verify(handler).endElement(isNull(String.class), eq("html"), isNull(String.class));
    }

    @Test
    public void testResourceReferenceInBody() throws Exception {

        startHeadSection(empty);
        startBodySection(empty);
        addDiv(empty);
        addStylesheetLink(CSS_RESOURCE_PATH);
        endBodySection();

        verify(handler).startElement(isNull(String.class), eq("html"), isNull(String.class), eq(empty));
        verify(handler).startElement(isNull(String.class), eq("head"), isNull(String.class), eq(empty));
        verify(handler).endElement(isNull(String.class), eq("head"), isNull(String.class));
        verify(handler).startElement(isNull(String.class), eq("body"), isNull(String.class), eq(empty));
        verifyDiv();
        verifyInlineStyle();
        verify(handler).endElement(isNull(String.class), eq("body"), isNull(String.class));
        verify(handler).endElement(isNull(String.class), eq("html"), isNull(String.class));
    }


    @Test
    public void testNonExistingResource() throws Exception {

        startHeadSection(empty);
        addStylesheetLink(NON_EXISTING_PATH);
        startBodySection(empty);
        endBodySection();

        verify(handler).startElement(isNull(String.class), eq("html"), isNull(String.class), eq(empty));
        verify(handler).startElement(isNull(String.class), eq("head"), isNull(String.class), eq(empty));
        verify(handler).endElement(isNull(String.class), eq("head"), isNull(String.class));
        verify(handler).startElement(isNull(String.class), eq("body"), isNull(String.class), eq(empty));
        verify(handler).endElement(isNull(String.class), eq("body"), isNull(String.class));
        verify(handler).endElement(isNull(String.class), eq("html"), isNull(String.class));
    }


    private void endBodySection() throws SAXException {
        transformer.endElement(null, "body", null);
        transformer.endElement(null, "html", null);
    }

    private void startBodySection(Attributes atts) throws SAXException {
        transformer.endElement(null, "head", null);
        transformer.startElement(null, "body", null, atts);
    }

    private void addDiv(Attributes atts) throws SAXException {
        transformer.startElement(null, "div", null, atts);
        transformer.characters( TEST_DATA.toCharArray(), 0, TEST_DATA.length());
        transformer.endElement(null, "div", null);
    }

    private void verifyDiv() throws SAXException {
        verify(handler).startElement(isNull(String.class), eq("div"), isNull(String.class), eq(empty));
        verify(handler).characters(TEST_DATA.toCharArray(), 0, TEST_DATA.length());
        verify(handler).endElement(isNull(String.class), eq("div"), isNull(String.class));
    }

    private void verifyInlineStyle() throws SAXException {
        verify(handler).startElement(isNull(String.class), eq("style"), isNull(String.class), any(org.xml.sax.Attributes.class));
        verify(handler).characters(NEWLINE.toCharArray(), 0, NEWLINE.length());
        verify(handler).characters(CSS_CONTENTS.toCharArray(), 0, CSS_CONTENTS.length());
        verify(handler).endElement(isNull(String.class), eq("style"), isNull(String.class));
    }

    private void startHeadSection(Attributes atts) throws SAXException {
        transformer.startElement(null, "html", null, atts);
        transformer.startElement(null, "head", null, atts);
    }

    private void addStylesheetLink(String path) throws SAXException {
        final AttributesImpl in = new AttributesImpl();
        in.addAttribute("", "href", "", "CDATA", path + ".css");
        in.addAttribute("", "type", "", "CDATA", "text/css");
        in.addAttribute("", "rel", "", "CDATA", "stylesheet");

        transformer.startElement(null, "link", null, in);
        transformer.endElement(null, "link", null);
    }
}
