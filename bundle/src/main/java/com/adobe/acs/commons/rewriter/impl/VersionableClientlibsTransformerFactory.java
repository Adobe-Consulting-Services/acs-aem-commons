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

import com.adobe.acs.commons.rewriter.AbstractTransformer;
import com.day.cq.commons.PathInfo;
import com.day.cq.widget.HtmlLibrary;
import com.day.cq.widget.HtmlLibraryManager;
import com.day.cq.widget.LibraryType;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.rewriter.Transformer;
import org.apache.sling.rewriter.TransformerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

@Component(
        label = "ACS AEM Commons - Versionable Clientlibs (CSS/JS) Rewriter",
        description = "Re-writes paths to CSS and JS clientlib includes to include the last modified timestamp as a "
                + "selector.",
        metatype = true)
@Property(name = "pipeline.type",
        value = "versionableclientlibs",
        label = "Rewriter Pipeline Type",
        propertyPrivate = true,
        description = "Type identifier to be referenced in rewriter pipeline configuration.")
@Service
public final class VersionableClientlibsTransformerFactory implements TransformerFactory {
    private static final Logger log = LoggerFactory.getLogger(VersionableClientlibsTransformerFactory.class);

    private static final String ATTR_JS_PATH = "src";
    private static final String ATTR_CSS_PATH = "href";

    private static final String CSS_TYPE = "text/css";
    private static final String JS_TYPE = "text/javascript";

    @Reference
    private HtmlLibraryManager htmlLibraryManager;

    public Transformer createTransformer() {
        return new VersionableClientlibsTransformer();
    }

    private Attributes versionClientLibs(final String elementName, final Attributes attrs) {
        if (this.isCSS(elementName, attrs)) {
            return this.rebuildAttributes(new AttributesImpl(attrs), attrs.getIndex("", ATTR_CSS_PATH),
                    attrs.getValue("", ATTR_CSS_PATH), LibraryType.CSS);

        } else if (this.isJavaScript(elementName, attrs)) {
            return this.rebuildAttributes(new AttributesImpl(attrs), attrs.getIndex("", ATTR_JS_PATH),
                    attrs.getValue("", ATTR_JS_PATH), LibraryType.JS);

        } else {
            return attrs;
        }
    }

    private Attributes rebuildAttributes(final AttributesImpl newAttributes, final int index, final String path,
                                         final LibraryType libraryType) {
        final String versionedPath = this.getVersionedPath(path, libraryType);

        if (StringUtils.isNotBlank(versionedPath)) {
            log.debug("Rewriting to: {}", versionedPath);
            newAttributes.setValue(index, versionedPath);
        } else {
            log.debug("Versioned Path could not be created properly");
        }

        return newAttributes;
    }

    private boolean isCSS(final String elementName, final Attributes attrs) {
        final String regex = "/\\S+\\" + LibraryType.CSS.extension + "$";

        if (StringUtils.equals("link", elementName)
                && this.hasValue(attrs, "rel", "stylesheet")
                && this.hasValue(attrs, "type", CSS_TYPE)
                && this.hasValue(attrs, "href", regex)) {
            return true;
        }

        return false;
    }

    private boolean isJavaScript(final String elementName, final Attributes attrs) {
        final String regex = "/\\S+\\" + LibraryType.JS.extension + "$";

        if (StringUtils.equals("script", elementName)
                && this.hasValue(attrs, "type", JS_TYPE)
                && this.hasValue(attrs, "src", regex)) {
            return true;
        }
        return false;
    }

    private String getVersionedPath(final String originalPath, final LibraryType libraryType) {
        final PathInfo pathInfo = new PathInfo(originalPath);

        final HtmlLibrary htmlLibrary = htmlLibraryManager.getLibrary(libraryType, pathInfo.getResourcePath());

        if (htmlLibrary != null) {
            return htmlLibrary.getLibraryPath() + "." + htmlLibrary.getLastModified() + libraryType.extension;
        } else {
            log.debug("Could not find HtmlLibrary fro path: {}", pathInfo.getResourcePath());
            return null;
        }
    }

    private boolean hasValue(final Attributes attributes, final String attributeName, final String regex) {
        final String value = attributes.getValue("", attributeName);
        if (value == null) { return false; }
        return value.matches(regex);
    }

    private class VersionableClientlibsTransformer extends AbstractTransformer {
        public void startElement(final String namespaceURI, final String localName, final String qName,
                                 final Attributes attrs)
                throws SAXException {
            getContentHandler().startElement(namespaceURI, localName, qName, versionClientLibs(localName, attrs));
        }
    }
}