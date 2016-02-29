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
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import javax.management.DynamicMBean;
import javax.management.NotCompliantMBeanException;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.rewriter.Transformer;
import org.apache.sling.rewriter.TransformerFactory;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.adobe.acs.commons.rewriter.AbstractTransformer;
import com.adobe.acs.commons.util.impl.AbstractGuavaCacheMBean;
import com.adobe.acs.commons.util.impl.GenericCacheMBean;
import com.day.cq.widget.HtmlLibrary;
import com.day.cq.widget.HtmlLibraryManager;
import com.day.cq.widget.LibraryType;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * ACS AEM Commons - Versioned Clientlibs (CSS/JS) Rewriter
 * Re-writes paths to CSS and JS clientlibs to include the md5 checksum as a "
 * selector; in the form: /path/to/clientlib.123456789.css
 */
@Component(metatype = true, label = "Versioned Clientlibs Transformer Factory",
    description = "Sling Rewriter Transformer Factory to add auto-generated checksums to client library references")
@Properties({
    @Property(name = "pipeline.type",
        value = "versioned-clientlibs", propertyPrivate = true),
    @Property(name = EventConstants.EVENT_TOPIC,
        value = "com/adobe/granite/ui/librarymanager/INVALIDATED", propertyPrivate = true),
    @Property(name = "jmx.objectname",
        value = "com.adobe.acs.commons.rewriter:type=VersionedClientlibsTransformerMd5Cache", propertyPrivate = true)
})
@Service(value = {DynamicMBean.class, TransformerFactory.class, EventHandler.class})
public final class VersionedClientlibsTransformerFactory extends AbstractGuavaCacheMBean<VersionedClientLibraryMd5CacheKey, String> implements TransformerFactory, EventHandler, GenericCacheMBean {

    private static final Logger log = LoggerFactory.getLogger(VersionedClientlibsTransformerFactory.class);
    
    private static final int DEFAULT_MD5_CACHE_SIZE = 300;
    
    @Property(label="MD5 Cache Size", description="Maximum size of the md5 cache.", intValue = DEFAULT_MD5_CACHE_SIZE)
    private static final String PROP_MD5_CACHE_SIZE = "md5cache.size";

    private static final String ATTR_JS_PATH = "src";
    private static final String ATTR_CSS_PATH = "href";

    private static final String CSS_TYPE = "text/css";
    private static final String JS_TYPE = "text/javascript";

    private static final String MIN_SELECTOR = "min";
    private static final String MIN_SELECTOR_SEGMENT = "." + MIN_SELECTOR;

    private Cache<VersionedClientLibraryMd5CacheKey, String> md5Cache;

    @Reference
    private HtmlLibraryManager htmlLibraryManager;

    public VersionedClientlibsTransformerFactory() throws NotCompliantMBeanException {
        super(GenericCacheMBean.class);
    }

    @Activate
    protected void activate(Map<String, Object> props) {
        final int size = PropertiesUtil.toInteger(props.get(PROP_MD5_CACHE_SIZE), DEFAULT_MD5_CACHE_SIZE);
        this.md5Cache = CacheBuilder.newBuilder().recordStats().maximumSize(size).build();
    }

    @Deactivate
    protected void deactivate() {
        this.md5Cache = null;
    }

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
        final String rel = attrs.getValue("", "rel");
        final String type = attrs.getValue("", "type");
        final String href = attrs.getValue("", "href");

        if (StringUtils.equals("link", elementName)
                && StringUtils.equals(rel, "stylesheet")
                && StringUtils.equals(type, CSS_TYPE)
                && StringUtils.startsWith(href, "/")
                && !StringUtils.startsWith(href, "//")
                && StringUtils.endsWith(href, LibraryType.CSS.extension)) {
            return true;
        }

        return false;
    }

    private boolean isJavaScript(final String elementName, final Attributes attrs) {
        final String type = attrs.getValue("", "type");
        final String src = attrs.getValue("", "src");

        if (StringUtils.equals("script", elementName)
                && StringUtils.equals(type, JS_TYPE)
                && StringUtils.startsWith(src, "/")
                && !StringUtils.startsWith(src, "//")
                && StringUtils.endsWith(src, LibraryType.JS.extension)) {
            return true;
        }

        return false;
    }

    private String getVersionedPath(final String originalPath, final LibraryType libraryType) {
        try {
            boolean appendMinSelector = false;
            String libraryPath = StringUtils.substringBeforeLast(originalPath, ".");
            if (libraryPath.endsWith(MIN_SELECTOR_SEGMENT)) {
                appendMinSelector = true;
                libraryPath = StringUtils.substringBeforeLast(libraryPath, ".");
            }

            final HtmlLibrary htmlLibrary = htmlLibraryManager.getLibrary(libraryType, libraryPath);

            if (htmlLibrary != null) {
                StringBuilder builder = new StringBuilder();
                builder.append(htmlLibrary.getLibraryPath());
                builder.append(".");

                if (appendMinSelector) {
                    builder.append(MIN_SELECTOR).append(".");
                }
                builder.append(getMd5(htmlLibrary));
                builder.append(libraryType.extension);

                return builder.toString();
            } else {
                log.debug("Could not find HtmlLibrary at path: {}", libraryPath);
                return null;
            }
        } catch (Exception ex) {
            // Handle unexpected formats of the original path
            log.error("Attempting to get a versioned path for [ {} ] but could not because of: {}", originalPath,
                    ex.getMessage());
            return originalPath;
        }
    }

    private String getMd5(final HtmlLibrary htmlLibrary) throws IOException, ExecutionException {
        return md5Cache.get(new VersionedClientLibraryMd5CacheKey(htmlLibrary), new Callable<String>() {

            @Override
            public String call() throws Exception {
                return DigestUtils.md5Hex(htmlLibrary.getInputStream());
            }
        });
    }

    private class VersionableClientlibsTransformer extends AbstractTransformer {
        public void startElement(final String namespaceURI, final String localName, final String qName,
                                 final Attributes attrs)
                throws SAXException {
            getContentHandler().startElement(namespaceURI, localName, qName, versionClientLibs(localName, attrs));
        }
    }

    @Override
    public void handleEvent(Event event) {
        String path = (String) event.getProperty(SlingConstants.PROPERTY_PATH);
        md5Cache.invalidate(new VersionedClientLibraryMd5CacheKey(path, LibraryType.JS));
        md5Cache.invalidate(new VersionedClientLibraryMd5CacheKey(path, LibraryType.CSS));
    }

    @Override
    protected Cache<VersionedClientLibraryMd5CacheKey, String> getCache() {
        return md5Cache;
    }

    @Override
    protected long getBytesLength(String cacheObj) {
        return cacheObj.getBytes().length;
    }

    @Override
    protected void addCacheData(Map<String, Object> data, String cacheObj) {
        data.put("Value", cacheObj);
    }

    @Override
    protected String toString(String cacheObj) throws Exception {
        return cacheObj;
    }

    @Override
    protected CompositeType getCacheEntryType() throws OpenDataException {
        return new CompositeType("Cache Entry", "Cache Entry",
                new String[] { "Cache Key", "Value" },
                new String[] { "Cache Key", "Value" },
                new OpenType[] { SimpleType.STRING, SimpleType.STRING });
    }
}