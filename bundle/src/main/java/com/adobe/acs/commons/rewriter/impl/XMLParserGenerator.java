/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.rewriter.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.sling.rewriter.Generator;
import org.apache.sling.rewriter.ProcessingComponentConfiguration;
import org.apache.sling.rewriter.ProcessingContext;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public final class XMLParserGenerator implements Generator {

    private final StringWriter writer;

    private final PrintWriter printWriter;

    private final SAXParser saxParser;

    private ContentHandler contentHandler;

    @SuppressWarnings("java:S2755")
    public XMLParserGenerator() throws ParserConfigurationException, SAXException {
        // XXE prevention is done in the other constructor
        this(SAXParserFactory.newInstance());
    }

    public XMLParserGenerator(final SAXParserFactory factory) throws ParserConfigurationException, SAXException {
        factory.setNamespaceAware(true);

        saxParser = factory.newSAXParser();
        saxParser.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, ""); 
        saxParser.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, ""); 
        this.writer = new StringWriter();
        this.printWriter = new PrintWriter(writer);
    }

    public void finished() throws IOException, SAXException {
        this.printWriter.flush();
        final ContentHandlerAdapter handler = new ContentHandlerAdapter(contentHandler);
        final String documentString = this.writer.toString();
        if (!documentString.isEmpty()) {
            final InputSource source = new InputSource(new StringReader(documentString));
            this.saxParser.parse(source, handler);
        }
    }

    public PrintWriter getWriter() {
        return printWriter;
    }

    public void init(ProcessingContext context, ProcessingComponentConfiguration config) throws IOException {
        // nothing to do
    }

    public void setContentHandler(ContentHandler handler) {
        this.contentHandler = handler;
    }

    public void dispose() {
        // nothing to do
    }

}
