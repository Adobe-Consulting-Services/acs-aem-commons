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

import java.io.IOException;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.sling.rewriter.ProcessingComponentConfiguration;
import org.apache.sling.rewriter.ProcessingContext;
import org.apache.sling.rewriter.Serializer;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

public class PlainXMLSerializer implements Serializer {

    private final TransformerHandler handler;

    public PlainXMLSerializer() throws TransformerConfigurationException {
        TransformerFactory factory = TransformerFactory.newInstance();
        if (factory.getFeature(SAXTransformerFactory.FEATURE)) {
            SAXTransformerFactory saxFactory = (SAXTransformerFactory) factory;
            this.handler = saxFactory.newTransformerHandler();
        } else {
            throw new UnsupportedOperationException("compatibility error");
        }
    }

    public void setDocumentLocator(Locator locator) {
        handler.setDocumentLocator(locator);
    }

    public void startDocument() throws SAXException {
        handler.startDocument();
    }

    public void endDocument() throws SAXException {
        handler.endDocument();
    }

    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        handler.startPrefixMapping(prefix, uri);
    }

    public void endPrefixMapping(String prefix) throws SAXException {
        handler.endPrefixMapping(prefix);
    }

    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        handler.startElement(uri, localName, qName, atts);
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        handler.endElement(uri, localName, qName);
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        handler.characters(ch, start, length);
    }

    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        handler.ignorableWhitespace(ch, start, length);
    }

    public void processingInstruction(String target, String data) throws SAXException {
        handler.processingInstruction(target, data);
    }

    public void skippedEntity(String name) throws SAXException {
        handler.skippedEntity(name);
    }

    @Override
    public void init(ProcessingContext context, ProcessingComponentConfiguration config) throws IOException {
        handler.setResult(new StreamResult(context.getOutputStream()));
    }

    @Override
    public void dispose() {
        // no-op
    }

}
