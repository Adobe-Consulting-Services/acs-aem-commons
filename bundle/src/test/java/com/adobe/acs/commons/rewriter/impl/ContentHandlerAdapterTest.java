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

import static org.mockito.Mockito.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

@RunWith(MockitoJUnitRunner.class)
public class ContentHandlerAdapterTest {
    
    @Mock
    private ContentHandler handler;
    
    @Mock
    private Locator locator;
    
    @Test
    public void test_adapted_methods() throws Exception {
        final Attributes attrs = new AttributesImpl();
        final char[] characters = new char[0];
        final char[] whitespace = new char[0];
        
        DefaultHandler adapter = new ContentHandlerAdapter(handler);
        adapter.setDocumentLocator(locator);
        adapter.startDocument();
        adapter.startPrefixMapping("prefix", "uri");
        adapter.endPrefixMapping("prefix");
        adapter.startElement("uri", "localName", "qName", attrs);
        adapter.endElement("uri", "localName", "qName");
        adapter.characters(characters, 1, 2);
        adapter.ignorableWhitespace(whitespace, 3, 4);
        adapter.processingInstruction("target", "data");
        adapter.skippedEntity("name");
        adapter.endDocument();

        verify(handler).setDocumentLocator(locator);
        verify(handler).startDocument();
        verify(handler).startPrefixMapping("prefix", "uri");
        verify(handler).endPrefixMapping("prefix");
        verify(handler).startElement("uri", "localName", "qName", attrs);
        verify(handler).endElement("uri", "localName", "qName");
        verify(handler).characters(characters, 1, 2);
        verify(handler).ignorableWhitespace(whitespace, 3, 4);
        verify(handler).processingInstruction("target", "data");
        verify(handler).skippedEntity("name");
        verify(handler).endDocument();
        verifyNoMoreInteractions(handler);
    }

}
