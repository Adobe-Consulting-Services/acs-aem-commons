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
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.rewriter.ProcessingComponentConfiguration;
import org.apache.sling.rewriter.ProcessingContext;
import org.apache.sling.rewriter.Transformer;
import org.apache.sling.rewriter.TransformerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.adobe.acs.commons.rewriter.ContentHandlerBasedTransformer;
import com.adobe.acs.commons.rewriter.DelegatingTransformer;
import com.adobe.granite.ui.clientlibs.HtmlLibrary;
import com.adobe.granite.ui.clientlibs.HtmlLibraryManager;
import com.adobe.granite.ui.clientlibs.LibraryType;

/**
 * ACS AEM Commons - Stylesheet inliner removes stylesheet links the output adds
 * them as <style> elements. Links found in <head> are added to the beginning of
 * <body>, whereas those in <body> are included where they're found.
 */
@Component(metatype = false)
@Properties({
    @Property(name = "pipeline.type", value = "inline-css")})
@Service(value = {TransformerFactory.class})
public final class StylesheetInlinerTransformerFactory implements TransformerFactory {

    private static final Logger log = LoggerFactory.getLogger(StylesheetInlinerTransformerFactory.class);

    private static final char[] NEWLINE = new char[]{'\n'};

    @Reference
    private HtmlLibraryManager htmlLibraryManager;

    public Transformer createTransformer() {
        return new SelectorAwareCssInlinerTransformer();
    }

    private final class CssInlinerTransformer extends ContentHandlerBasedTransformer {

        private static final String HEAD = "head";
        private static final String STYLE = "style";

        private final Attributes attrs = new AttributesImpl();

        private final Map<String, char[]> stylesheetsInHead = new LinkedHashMap<>();

        private SlingHttpServletRequest slingRequest;
        private boolean afterHeadElement = false;

        @Override
        public void init(final ProcessingContext context, final ProcessingComponentConfiguration config) throws IOException {
            super.init(context, config);
            slingRequest = context.getRequest();
            log.debug("Inlining Stylesheet references for {}", slingRequest.getRequestURL());
        }

        @Override
        public void startElement(final String namespaceURI, final String localName, final String qName,
                                 final Attributes attrs) throws SAXException {
            try {
                final ContentHandler contentHandler = getContentHandler();
                if (SaxElementUtils.isCss(localName, attrs)) {
                    final String sheet = attrs.getValue("", "href");
                    if (afterHeadElement) {
                        log.debug("Inlining stylesheet link found in BODY: '{}'", sheet);
                        if (!inlineSheet(namespaceURI, sheet)) {
                            contentHandler.startElement(namespaceURI, localName, qName, attrs);
                        }
                    } else {
                        final Optional<char[]> contents = readSheetContent(sheet);
                        if (contents.isPresent()) {
                            stylesheetsInHead.put(sheet, contents.get());
                        } else {
                            contentHandler.startElement(namespaceURI, localName, qName, attrs);
                        }
                    }
                } else {
                    contentHandler.startElement(namespaceURI, localName, qName, attrs);
                }
            } catch (final IOException | SAXException e) {
                log.error("Exception in stylesheet inliner", e);
                throw new SAXException(e);
            }
        }

        @Override
        public void endElement(final String uri, final String localName, final String qName) throws SAXException {
            if (HEAD.equalsIgnoreCase(localName)) {
                afterHeadElement = true;
                try {
                    // add each of the accumulated stylesheet references
                    for (final Map.Entry<String, char[]> entry : stylesheetsInHead.entrySet()) {
                        log.debug("Inlining sheet found in HEAD: '{}'", entry.getKey());
                        inlineSheet(uri, entry.getValue());
                    }
                } catch (final SAXException e) {
                    log.error("Exception in stylesheet inliner", e);
                    throw new SAXException(e);
                }
            }

            getContentHandler().endElement(uri, localName, qName);
        }

        private Optional<char[]> readSheetContent(final String sheet) throws IOException, SAXException {
            InputStream inputStream = null;

            final String withoutExtension = sheet.substring(0, sheet.indexOf(LibraryType.CSS.extension));
            final HtmlLibrary library = htmlLibraryManager.getLibrary(LibraryType.CSS, withoutExtension);
            if (library != null) {
                inputStream = library.getInputStream();
            } else {
                final Resource resource = slingRequest.getResourceResolver().getResource(sheet);
                if (resource != null) {
                    inputStream = resource.adaptTo(InputStream.class);
                }
            }

            if (inputStream != null) {
                return Optional.of(IOUtils.toCharArray(inputStream, "UTF-8"));
            }

            return Optional.empty();
        }

        private boolean inlineSheet(final String namespaceURI, final char[] content) throws SAXException {
            if (content != null) {
                final ContentHandler contentHandler = getContentHandler();
                contentHandler.startElement(namespaceURI, STYLE, null, attrs);
                contentHandler.characters(NEWLINE, 0, 1);
                contentHandler.characters(content, 0, content.length);
                contentHandler.endElement(namespaceURI, STYLE, null);

                return true;
            }

            return false;
        }

        private boolean inlineSheet(final String namespaceURI, final String sheet) throws IOException, SAXException {
            return inlineSheet(namespaceURI, readSheetContent(sheet).orElse(null));
        }
    }

    final class SelectorAwareCssInlinerTransformer extends DelegatingTransformer {

        @Override
        public void init(final ProcessingContext context, final ProcessingComponentConfiguration componentConfiguration) throws IOException {
            final SlingHttpServletRequest request = context.getRequest();
            boolean inlineCss = false;
            final String[] selectors = request.getRequestPathInfo().getSelectors();
            for (int i = 0; !inlineCss && i < selectors.length; i++) {
                inlineCss = "inline-css".equals(selectors[i]);
            }

            if (inlineCss) {
                setDelegate(new CssInlinerTransformer());
            } else {
                setDelegate(new ContentHandlerBasedTransformer());
            }

            super.init(context, componentConfiguration);
        }
    }
}
