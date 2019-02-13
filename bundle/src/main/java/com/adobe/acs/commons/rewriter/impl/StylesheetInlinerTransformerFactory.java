/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.adobe.acs.commons.rewriter.AbstractTransformer;
import com.adobe.granite.ui.clientlibs.HtmlLibrary;
import com.adobe.granite.ui.clientlibs.HtmlLibraryManager;
import com.adobe.granite.ui.clientlibs.LibraryType;

/**
 * ACS AEM Commons - Stylesheet inliner removes stylesheet links the output adds
 * them as <style> elements. Links found in <head> are added to the beginning of
 * <body>, whereas those in <body> are included where they're found.
 */
@Component(
        metatype = false,
        label = "Stylesheet Inliner Transformer Factory",
        description = "Sling Rewriter Transformer Factory which inlines CSS references")
@Properties({
    @Property(name = "pipeline.type", value = "inline-css", propertyPrivate = true)})
@Service(value = {TransformerFactory.class})
public final class StylesheetInlinerTransformerFactory implements TransformerFactory {

    private static final char[] NEWLINE = new char[]{'\n'};
    private static final String STYLE = "style";
    private static final String HEAD = "head";

    private static final Logger log = LoggerFactory.getLogger(StylesheetInlinerTransformerFactory.class);

    @Reference
    private HtmlLibraryManager htmlLibraryManager;

    public Transformer createTransformer() {
        return new CssInlinerTransformer();
    }

    private class CssInlinerTransformer extends AbstractTransformer {

        protected boolean afterHeadElement = false;
        protected List<String> stylesheetsInHead = new ArrayList<String>();
        private SlingHttpServletRequest slingRequest;

        @Override
        public void init(ProcessingContext context, ProcessingComponentConfiguration config) throws IOException {
            super.init(context, config);
            slingRequest = context.getRequest();
            log.debug("Inlining Stylesheet references for {}", slingRequest.getRequestURL().toString());
        }

        public void startElement(final String namespaceURI, final String localName, final String qName,
                final Attributes attrs) throws SAXException {
            try {
                if (SaxElementUtils.isCss(localName, attrs)) {
                    String sheet = attrs.getValue("", "href");
                    if (!afterHeadElement) {
                        stylesheetsInHead.add(sheet);
                    } else {
                        log.debug("Inlining stylesheet link found in BODY: '{}'", sheet);
                        inlineSheet(namespaceURI, sheet);
                    }
                } else {
                    getContentHandler().startElement(namespaceURI, localName, qName, attrs);
                }
            } catch (Exception e) {
                log.error("Exception in stylesheet inliner", e);
                throw new SAXException(e);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (localName.equalsIgnoreCase(HEAD)) {
                afterHeadElement = true;
                try {
                    // add each of the accumulated stylesheet references
                    for (String sheet : stylesheetsInHead) {
                        log.debug("Inlining sheet found in HEAD: '{}'", sheet);
                        inlineSheet(uri, sheet);
                    }
                } catch (Exception e) {
                    log.error("Exception in stylesheet inliner", e);
                    throw new SAXException(e);
                }
            }
            getContentHandler().endElement(uri, localName, qName);
        }

        private void inlineSheet(final String namespaceURI, String s) throws IOException, SAXException {
            InputStream inputStream = null;

            String withoutExtension = s.substring(0, s.indexOf(LibraryType.CSS.extension));
            HtmlLibrary library = htmlLibraryManager.getLibrary(LibraryType.CSS, withoutExtension);
            if (library != null) {
                inputStream = library.getInputStream();
            } else {
                Resource resource = slingRequest.getResourceResolver().getResource(s);

                if (resource != null) {
                    inputStream = resource.adaptTo(InputStream.class);
                }
            }

            if (inputStream != null) {
                char[] chars = IOUtils.toCharArray(inputStream, "UTF-8");

                getContentHandler().startElement(namespaceURI, STYLE, null, new AttributesImpl());
                getContentHandler().characters(NEWLINE, 0, 1);
                getContentHandler().characters(chars, 0, chars.length);
                getContentHandler().endElement(namespaceURI, STYLE, null);
            }
        }
    }
}
