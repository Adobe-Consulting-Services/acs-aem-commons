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
package com.adobe.acs.commons.rewriter;

import java.io.IOException;

import org.apache.sling.rewriter.ProcessingComponentConfiguration;
import org.apache.sling.rewriter.ProcessingContext;
import org.apache.sling.rewriter.Transformer;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import aQute.bnd.annotation.ConsumerType;

/**
 * Abstract base class to ease creating transformer pipeline components.
 * All methods are impelemented as pass-throughs to the next content handler.
 * Similar to Cocoon's AbstractSAXPipe.
 */
@ConsumerType
public abstract class AbstractTransformer implements Transformer {

    private ContentHandler contentHandler;

    /**
     * {@inheritDoc}
     */
    public void characters(final char[] ch, final int start, final int length) throws SAXException {
        contentHandler.characters(ch, start, length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
    }

    /**
     * {@inheritDoc}
     */
    public void endDocument() throws SAXException {
        contentHandler.endDocument();
    }

    /**
     * {@inheritDoc}
     */
    public void endElement(final String uri, final String localName, final String qName) throws SAXException {
        contentHandler.endElement(uri, localName, qName);
    }

    /**
     * {@inheritDoc}
     */
    public void endPrefixMapping(final String prefix) throws SAXException {
        contentHandler.endPrefixMapping(prefix);
    }

    /**
     * {@inheritDoc}
     */
    public void ignorableWhitespace(final char[] ch, final int start, final int length) throws SAXException {
        contentHandler.ignorableWhitespace(ch, start, length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final ProcessingContext context, final ProcessingComponentConfiguration config)
            throws IOException {
    }

    /**
     * {@inheritDoc}
     */
    public void processingInstruction(final String target, final String data) throws SAXException {
        contentHandler.processingInstruction(target, data);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void setContentHandler(final ContentHandler contentHandler) {
        this.contentHandler = contentHandler;
    }

    /**
     * {@inheritDoc}
     */
    public void setDocumentLocator(final Locator locator) {
        contentHandler.setDocumentLocator(locator);
    }

    /**
     * {@inheritDoc}
     */
    public void skippedEntity(final String name) throws SAXException {
        contentHandler.skippedEntity(name);
    }

    /**
     * {@inheritDoc}
     */
    public void startDocument() throws SAXException {
        contentHandler.startDocument();
    }

    /**
     * {@inheritDoc}
     */
    public void startElement(final String uri, final String localName, final String qName, final Attributes atts)
            throws SAXException {
        contentHandler.startElement(uri, localName, qName, atts);
    }

    /**
     * {@inheritDoc}
     */
    public void startPrefixMapping(final String prefix, final String uri)
            throws SAXException {
        contentHandler.startPrefixMapping(prefix, uri);
    }

    protected final ContentHandler getContentHandler() {
        return contentHandler;
    }

}
