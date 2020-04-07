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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.rewriter.ProcessingContext;
import org.apache.sling.rewriter.Transformer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.adobe.acs.commons.rewriter.ContentHandlerBasedTransformer;
import com.adobe.acs.commons.rewriter.DelegatingTransformer;
import com.adobe.granite.ui.clientlibs.HtmlLibrary;
import com.adobe.granite.ui.clientlibs.HtmlLibraryManager;
import com.adobe.granite.ui.clientlibs.LibraryType;

import junitx.util.PrivateAccessor;

@RunWith(MockitoJUnitRunner.class)
public final class StylesheetInlinerTransformerFactoryTest {

    private static final String CDATA = "CDATA";
    private static final String HTML = "html";
    private static final String HEAD = "head";
    private static final String BODY = "body";
    private static final String STYLE = "style";
    private static final String DIV = "div";
    private static final String LINK = "link";

    private static final String CLIENTLIB_PATH = "/etc/clientlibs/test";
    private static final String CSS_RESOURCE_PATH = "/etc/assets/somecss";
    private static final String NON_EXISTING_PATH = "/etc/assets/doesntexist";

    private static final String CSS_CONTENTS = "div {display:block;}";
    private static final String NEWLINE = "\n";

    private static final String TEST_DATA = "some test data";

    private StylesheetInlinerTransformerFactory factory = new StylesheetInlinerTransformerFactory();

    private final Transformer transformer = factory.createTransformer();

    private final Attributes empty = new AttributesImpl();

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
    private RequestPathInfo requestPathInfo;

    @Mock
    private ResourceResolver resourceResolver;

    @Mock
    private Resource resource;

    @Before
    public void setUp() throws NoSuchFieldException, IOException {
        PrivateAccessor.setField(factory, "htmlLibraryManager", htmlLibraryManager);

        when(htmlLibrary.getInputStream()).thenReturn(new java.io.ByteArrayInputStream(CSS_CONTENTS.getBytes()));
        when(htmlLibraryManager.getLibrary(eq(LibraryType.CSS), eq(CLIENTLIB_PATH))).thenReturn(htmlLibrary);
        when(htmlLibraryManager.getLibrary(eq(LibraryType.CSS), not(eq(CLIENTLIB_PATH)))).thenReturn(null);
        when(slingRequest.getRequestURL()).thenReturn(new StringBuffer("testing"));
        when(resource.adaptTo(eq(InputStream.class))).thenReturn(new java.io.ByteArrayInputStream(CSS_CONTENTS.getBytes()));
        when(resourceResolver.getResource(eq(CSS_RESOURCE_PATH + ".css" ))).thenReturn(resource);
        when(slingRequest.getResourceResolver()).thenReturn(resourceResolver);
        when(processingContext.getRequest()).thenReturn(slingRequest);
        when(slingRequest.getRequestPathInfo()).thenReturn(requestPathInfo);
        when(requestPathInfo.getSelectors()).thenReturn(new String[] { "inline-css" });

        transformer.init(processingContext, null);
        transformer.setContentHandler(handler);
    }

    @Test
    public void testNoop() throws SAXException {
        startHeadSection();
        startBodySection();
        endBodySection();

        verify(handler).startElement(isNull(), eq(HTML), isNull(), eq(empty));
        verify(handler).startElement(isNull(), eq(HEAD), isNull(), eq(empty));
        verify(handler).endElement(isNull(), eq(HEAD), isNull());
        verify(handler).startElement(isNull(), eq(BODY), isNull(), eq(empty));
        verify(handler).endElement(isNull(), eq(BODY), isNull());
        verify(handler).endElement(isNull(), eq(HTML), isNull());
    }

    @Test
    public void testClientLibReferenceInHead() throws SAXException {
        startHeadSection();
        addStylesheetLink(CLIENTLIB_PATH);
        startBodySection();
        endBodySection();

        verify(handler).startElement(isNull(), eq(HTML), isNull(), eq(empty));
        verify(handler).startElement(isNull(), eq(HEAD), isNull(), eq(empty));
        verifyInlineStyle();
        verify(handler).endElement(isNull(), eq(HEAD), isNull());
        verify(handler).startElement(isNull(), eq(BODY), isNull(), eq(empty));
        verify(handler).endElement(isNull(), eq(BODY), isNull());
        verify(handler).endElement(isNull(), eq(HTML), isNull());
    }

    @Test
    public void testClientLibReferenceInBody() throws SAXException {
        startHeadSection();
        startBodySection();
        addDiv();
        addStylesheetLink(CLIENTLIB_PATH);
        endBodySection();

        verify(handler).startElement(isNull(), eq(HTML), isNull(), eq(empty));
        verify(handler).startElement(isNull(), eq(HEAD), isNull(), eq(empty));
        verify(handler).endElement(isNull(), eq(HEAD), isNull());
        verify(handler).startElement(isNull(), eq(BODY), isNull(), eq(empty));
        verifyDiv();
        verifyInlineStyle();
        verify(handler).endElement(isNull(), eq(BODY), isNull());
        verify(handler).endElement(isNull(), eq(HTML), isNull());
    }

    @Test
    public void testResourceReferenceInHead() throws SAXException {
        startHeadSection();
        addStylesheetLink(CSS_RESOURCE_PATH);
        startBodySection();
        endBodySection();

        verify(handler).startElement(isNull(), eq(HTML), isNull(), eq(empty));
        verify(handler).startElement(isNull(), eq(HEAD), isNull(), eq(empty));
        verifyInlineStyle();
        verify(handler).endElement(isNull(), eq(HEAD), isNull());
        verify(handler).startElement(isNull(), eq(BODY), isNull(), eq(empty));
        verify(handler).endElement(isNull(), eq(BODY), isNull());
        verify(handler).endElement(isNull(), eq(HTML), isNull());
    }

    @Test
    public void testResourceReferenceInBody() throws SAXException {
        startHeadSection();
        startBodySection();
        addDiv();
        addStylesheetLink(CSS_RESOURCE_PATH);
        endBodySection();

        verify(handler).startElement(isNull(), eq(HTML), isNull(), eq(empty));
        verify(handler).startElement(isNull(), eq(HEAD), isNull(), eq(empty));
        verify(handler).endElement(isNull(), eq(HEAD), isNull());
        verify(handler).startElement(isNull(), eq(BODY), isNull(), eq(empty));
        verifyDiv();
        verifyInlineStyle();
        verify(handler).endElement(isNull(), eq(BODY), isNull());
        verify(handler).endElement(isNull(), eq(HTML), isNull());
    }

    @Test
    public void testNonExistingResource() throws SAXException {
        startHeadSection();
        addStylesheetLink(NON_EXISTING_PATH);
        startBodySection();
        endBodySection();

        verify(handler).startElement(isNull(), eq(HTML), isNull(), eq(empty));
        verify(handler).startElement(isNull(), eq(HEAD), isNull(), eq(empty));
        verify(handler).endElement(isNull(), eq(HEAD), isNull());
        verify(handler).startElement(isNull(), eq(BODY), isNull(), eq(empty));
        verify(handler).endElement(isNull(), eq(BODY), isNull());
        verify(handler).endElement(isNull(), eq(HTML), isNull());
    }

    @Test
    public void testUnsuccessfulInlineSheet() throws SAXException {
        startHeadSection();
        startBodySection();
        addStylesheetLink(CSS_RESOURCE_PATH + "-incorrect");
        verify(handler).startElement(isNull(), eq(LINK), isNull(), any(Attributes.class));
    }

    @Test
    public void testDefaultTransformer() throws IOException {
        when(requestPathInfo.getSelectors()).thenReturn(new String[] { });
        if (transformer instanceof DelegatingTransformer) {
            final DelegatingTransformer transformer = (DelegatingTransformer) this.transformer;
            transformer.init(processingContext, null);
            Assert.assertTrue(ContentHandlerBasedTransformer.class.equals(transformer.getDelegate().getClass()));
        } else {
            Assert.fail("The transformer should be of a certain inner type.");
        }
    }

    private void endBodySection() throws SAXException {
        transformer.endElement(null, BODY, null);
        transformer.endElement(null, HTML, null);
    }

    private void startBodySection(final Attributes atts) throws SAXException {
        transformer.endElement(null, HEAD, null);
        transformer.startElement(null, BODY, null, atts);
    }

    private void startBodySection() throws SAXException {
        startBodySection(empty);
    }

    private void addDiv(final Attributes atts) throws SAXException {
        transformer.startElement(null, DIV, null, atts);
        transformer.characters( TEST_DATA.toCharArray(), 0, TEST_DATA.length());
        transformer.endElement(null, DIV, null);
    }

    private void addDiv() throws SAXException {
        addDiv(empty);
    }

    private void verifyDiv() throws SAXException {
        verify(handler).startElement(isNull(), eq(DIV), isNull(), eq(empty));
        verify(handler).characters(TEST_DATA.toCharArray(), 0, TEST_DATA.length());
        verify(handler).endElement(isNull(), eq(DIV), isNull());
    }

    private void verifyInlineStyle() throws SAXException {
        verify(handler).startElement(isNull(), eq(STYLE), isNull(), any(Attributes.class));
        verify(handler).characters(NEWLINE.toCharArray(), 0, NEWLINE.length());
        verify(handler).characters(CSS_CONTENTS.toCharArray(), 0, CSS_CONTENTS.length());
        verify(handler).endElement(isNull(), eq(STYLE), isNull());
    }

    private void startHeadSection(final Attributes atts) throws SAXException {
        transformer.startElement(null, HTML, null, atts);
        transformer.startElement(null, HEAD, null, atts);
    }

    private void startHeadSection() throws SAXException {
        startHeadSection(empty);
    }

    private void addStylesheetLink(final String path) throws SAXException {
        final AttributesImpl in = new AttributesImpl();
        in.addAttribute("", "href", "", CDATA, path + ".css");
        in.addAttribute("", "type", "", CDATA, "text/css");
        in.addAttribute("", "rel", "", CDATA, "stylesheet");

        transformer.startElement(null, LINK, null, in);
        transformer.endElement(null, LINK, null);
    }
}
