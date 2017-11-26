package com.adobe.acs.commons.components.longformtext.impl;

import org.apache.sling.commons.html.HtmlParser;
import org.apache.sling.commons.html.impl.DOMBuilder;
import org.ccil.cowan.tagsoup.Parser;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.powermock.modules.junit4.PowerMockRunner;
import org.w3c.dom.Document;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;


@RunWith(PowerMockRunner.class)
public class LongFormTextComponentImplTest {

    @Spy
    HtmlParser htmlParser = new SlingHtmlParser();

    @InjectMocks
    final LongFormTextComponentImpl longFormTextComponent = new LongFormTextComponentImpl();

    @Test
    public void testGetTextParagraphs_1() throws Exception {
        final String input = "<p>ira is a dog</p>\n<p> she barks a lot</p>";

        final String[] expected = new String[] {"<p>ira is a dog</p>", "<p> she barks a lot</p>"};
        final String[] result = longFormTextComponent.getTextParagraphs(input);

        Assert.assertArrayEquals(expected, result);
    }

    @Test
    public void testGetTextParagraphs_2() throws Exception {
        final String input = "<div class=\"dog-park\"><p>ira is a dog</p><p> she barks a lot</p></div>";

        final String[] expected = new String[] {"<div class=\"dog-park\"><p>ira is a dog</p><p> she barks a " +
                "lot</p></div>"};
        final String[] result = longFormTextComponent.getTextParagraphs(input);

        Assert.assertArrayEquals(expected, result);
    }

    @Test
    public void testGetTextParagraphs_3() throws Exception {
        final String input = "<span>ira looks like this: <img src=\"dog.png\"/></span><p>she barks a lot</p>";

        final String[] expected = new String[] {"<span>ira looks like this: <img src=\"dog.png\"/></span>",
                "<p>she barks a lot</p>"};
        final String[] result = longFormTextComponent.getTextParagraphs(input);

        Assert.assertArrayEquals(expected, result);
    }

    @Test
    public void testGetTextParagraphs_4() throws Exception {
        final String input = "<p>ira looks like this</p><img src=\"dog.png\"/>";

        final String[] expected = new String[] {"<p>ira looks like this</p>","<img src=\"dog.png\"/>"};
        final String[] result = longFormTextComponent.getTextParagraphs(input);

        Assert.assertArrayEquals(expected, result);
    }

    @Test
    public void testGetTextParagraphs_5() throws Exception {
        final String input = "random junk text <p> ira looks like this</p>  <img src=\"dog.png\"/>   other junk";

        final String[] expected = new String[] {"<p> ira looks like this</p>","<img src=\"dog.png\"/>"};
        final String[] result = longFormTextComponent.getTextParagraphs(input);

        Assert.assertArrayEquals(expected, result);
    }

    /*
     * Licensed to the Apache Software Foundation (ASF) under one
     * or more contributor license agreements.  See the NOTICE file
     * distributed with this work for additional information
     * regarding copyright ownership.  The ASF licenses this file
     * to you under the Apache License, Version 2.0 (the
     * "License"); you may not use this file except in compliance
     * with the License.  You may obtain a copy of the License at
     *
     *   http://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing,
     * software distributed under the License is distributed on an
     * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
     * KIND, either express or implied.  See the License for the
     * specific language governing permissions and limitations
     * under the License.
     */
    
    /**
     * This is the private Impl of the Sling HtmlParser which is the OSGi Service used to part
     * the HTML and created the Document in this Component.
     *
     * https://github.com/apache/sling/blob/43528d39840cdf011dea5b2768686cc96ee3326e/contrib/commons/html/src/main/java/org/apache/sling/commons/html/impl/HtmlParserImpl.java
     */
    private class SlingHtmlParser implements HtmlParser {

        public void parse(InputStream stream, String encoding, ContentHandler ch)
                throws SAXException {
            throw new UnsupportedOperationException("This method is not supported for this Test");
        }

        /**
         * @see org.apache.sling.commons.html.HtmlParser#parse(java.lang.String, java.io.InputStream, java.lang.String)
         */
        public Document parse(String systemId, InputStream stream, String encoding) throws IOException {
            final Parser parser = new Parser();
            final DOMBuilder builder = new DOMBuilder();
            final InputSource source = new InputSource(stream);
            source.setEncoding(encoding);
            source.setSystemId(systemId);
            try {
                parser.setProperty("http://xml.org/sax/properties/lexical-handler", builder);
                parser.setContentHandler(builder);
                parser.parse(source);
            } catch (SAXException se) {
                if ( se.getCause() instanceof IOException ) {
                    throw (IOException) se.getCause();
                }
                throw (IOException) new IOException("Unable to parse xml.").initCause(se);
            }
            return builder.getDocument();
        }
    }
}
