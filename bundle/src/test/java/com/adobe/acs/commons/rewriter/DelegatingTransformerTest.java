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

import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.apache.sling.rewriter.Transformer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.xml.sax.SAXException;

@RunWith(MockitoJUnitRunner.class)
public final class DelegatingTransformerTest {

    private final DelegatingTransformer delegating = new DelegatingTransformer();

    @Mock
    private Transformer transformer;

    @Before
    public void setUp() {
        delegating.setDelegate(transformer);
    }

    @Test
    public void init() throws IOException {
        delegating.init(null, null);
        verify(transformer).init(null, null);
    }

    @Test
    public void setContentHandler() {
        delegating.setContentHandler(null);
        verify(transformer).setContentHandler(null);
    }

    @Test
    public void dispose() {
        delegating.dispose();
        verify(transformer).dispose();
    }

    @Test
    public void setDocumentLocator() {
        delegating.setDocumentLocator(null);
        verify(transformer).setDocumentLocator(null);
    }

    @Test
    public void startDocument() throws SAXException {
        delegating.startDocument();
        verify(transformer).startDocument();
    }

    @Test
    public void endDocument() throws SAXException {
        delegating.endDocument();
        verify(transformer).endDocument();
    }

    @Test
    public void startPrefixMapping() throws SAXException {
        delegating.startPrefixMapping(null, null);
        verify(transformer).startPrefixMapping(null, null);
    }

    @Test
    public void endPrefixMapping() throws SAXException {
        delegating.endPrefixMapping(null);
        verify(transformer).endPrefixMapping(null);
    }

    @Test
    public void startElement() throws SAXException {
        delegating.startElement(null, null, null, null);
        verify(transformer).startElement(null, null, null, null);
    }

    @Test
    public void endElement() throws SAXException {
        delegating.endElement(null, null, null);
        verify(transformer).endElement(null, null, null);
    }

    @Test
    public void characters() throws SAXException {
        delegating.characters(null, 0, 0);
        verify(transformer).characters(null, 0, 0);
    }

    @Test
    public void ignorableWhitespace() throws SAXException {
        delegating.ignorableWhitespace(null, 0, 0);
        verify(transformer).ignorableWhitespace(null, 0, 0);
    }

    @Test
    public void processingInstruction() throws SAXException {
        delegating.processingInstruction(null, null);
        verify(transformer).processingInstruction(null, null);
    }

    @Test
    public void skippedEntity() throws SAXException {
        delegating.skippedEntity(null);
        verify(transformer).skippedEntity(null);
    }

}
