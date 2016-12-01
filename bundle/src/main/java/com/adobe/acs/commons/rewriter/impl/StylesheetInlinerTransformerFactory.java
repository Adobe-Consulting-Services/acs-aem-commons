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

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
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
        metatype = true, 
        label = "Stylesheet Inliner Transformer Factory", 
        description = "Sling Rewriter Transformer Factory which inlines CSS references")
@Properties({ 
    @Property( name = "pipeline.type", value = StylesheetInlinerTransformerFactory.SELECTOR, propertyPrivate = true) } )
@Service(value = { TransformerFactory.class })
public final class StylesheetInlinerTransformerFactory implements TransformerFactory {

    public static final String SELECTOR = "inline-css";
    
    private static final String NEWLINE = "\n";
    private static final String STYLE = "style";
    private static final String BODY = "body";
    private static final String HEAD = "head";

    private static final Logger log = LoggerFactory.getLogger(StylesheetInlinerTransformerFactory.class);

    @Reference
    private HtmlLibraryManager htmlLibraryManager;


    public Transformer createTransformer() {
        return new CSSInlinerTransformer();
    }

    private class CSSInlinerTransformer extends AbstractTransformer {

        protected boolean enabled = false;
        protected boolean inHead = false;
        protected List<String> stylesheetsInHead = new ArrayList<String>();
        private SlingHttpServletRequest slingRequest;

        @Override
        public void init(ProcessingContext context, ProcessingComponentConfiguration config) throws IOException {
            super.init(context, config);
            slingRequest = context.getRequest();

            String[] selectors = slingRequest.getRequestPathInfo().getSelectors();

            if (selectors.length > 0) {
                enabled = Arrays.asList(selectors).contains(SELECTOR);
                if (enabled) {
                    log.info("Inlining Stylesheet references for {}", slingRequest.getRequestURL().toString());
                }
            }
        }

        public void startElement(final String namespaceURI, final String localName, final String qName,
                final Attributes attrs) throws SAXException {
            if (enabled) {
                try {
                    if (isCSS(localName, attrs)) {
                        String sheet = attrs.getValue("", "href");
                        if (inHead) {
                            stylesheetsInHead.add(sheet);
                        } else {
                            log.debug("Inlining stylesheet link found in BODY: '{}'", sheet);
                            inlineSheet(namespaceURI, sheet);
                        }
                    } else {
                        getContentHandler().startElement(namespaceURI, localName, qName, attrs);

                        if (localName.equalsIgnoreCase(HEAD)) {
                            inHead = true;
                        } else if (localName.equalsIgnoreCase(BODY)) {

                            // add each of the accumulated stylesheet references
                            for (String sheet : stylesheetsInHead) {
                                log.debug("Inlining sheet found in HEAD: '{}'", sheet);
                                inlineSheet(namespaceURI, sheet);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("Exception in stylesheet inliner", e);
                    throw new SAXException(e);
                }
            } else {
                getContentHandler().startElement(namespaceURI, localName, qName, attrs);
            }
        }

        private void inlineSheet(final String namespaceURI, String s) throws Exception {
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
                InputStreamReader isr = new InputStreamReader(inputStream);

                CharArrayWriter caw = new CharArrayWriter(inputStream.available());
                caw.write(NEWLINE);
                int read = -1;
                while ((read = isr.read()) > -1) {
                    caw.write(read);
                }
                getContentHandler().startElement(namespaceURI, STYLE, null, new AttributesImpl());
                getContentHandler().characters(caw.toCharArray(), 0, caw.size());
                super.endElement(namespaceURI, STYLE, null);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (inHead && localName.equalsIgnoreCase(HEAD)) {
                inHead = false;
            }
            super.endElement(uri, localName, qName);
        }
    }

    private static boolean isCSS(final String elementName, final Attributes attrs) {
        final String rel = attrs.getValue("", "rel");
        final String type = attrs.getValue("", "type");
        final String href = attrs.getValue("", "href");

        return StringUtils.equals("link", elementName) && StringUtils.equals(rel, "stylesheet")
                && StringUtils.equals(type, LibraryType.CSS.contentType) && StringUtils.startsWith(href, "/")
                && !StringUtils.startsWith(href, "//") && StringUtils.endsWith(href, LibraryType.CSS.extension);
    }

    @Activate
    protected void activate(Map<String, Object> props) {
    }

    @Deactivate
    protected void deactivate() {
    }
}