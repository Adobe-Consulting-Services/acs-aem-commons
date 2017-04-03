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
import java.nio.charset.Charset;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.management.DynamicMBean;
import javax.management.NotCompliantMBeanException;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

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
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.rewriter.ProcessingComponentConfiguration;
import org.apache.sling.rewriter.ProcessingContext;
import org.apache.sling.rewriter.Transformer;
import org.apache.sling.rewriter.TransformerFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
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
 * selector; in the form: /path/to/clientlib.123456789.css or /path/to/clientlib.min.1234589.css (if minification is enabled)
 * If the Enforce MD5 filter is enabled, the paths will be like /path/to/clientlib.ACSHASH123456789.css or /path/to/clientlib.min.ACSHASH1234589.css (if minification is enabled)
 */
@Component(metatype = true, label = "ACS AEM Commons - Versioned Clientlibs Transformer Factory",
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

    private static final boolean DEFAULT_DISABLE_VERSIONING = false;

    private static final boolean DEFAULT_ENFORCE_MD5 = false;

    @Property(label="MD5 Cache Size", description="Maximum size of the md5 cache.", intValue = DEFAULT_MD5_CACHE_SIZE)
    private static final String PROP_MD5_CACHE_SIZE = "md5cache.size";

    @Property(label="Disable Versioning", description="Should versioning of clientlibs be disabled", boolValue = DEFAULT_DISABLE_VERSIONING)
    private static final String PROP_DISABLE_VERSIONING = "disable.versioning";

    @Property(label="Enforce MD5", description="Enables a filter which returns a 404 error if the MD5 in the request does not match the expected value",
        boolValue = DEFAULT_ENFORCE_MD5)
    private static final String PROP_ENFORCE_MD5 = "enforce.md5";

    private static final String ATTR_JS_PATH = "src";
    private static final String ATTR_CSS_PATH = "href";

    private static final String CSS_TYPE = "text/css";
    private static final String JS_TYPE = "text/javascript";

    private static final String MIN_SELECTOR = "min";
    private static final String MIN_SELECTOR_SEGMENT = "." + MIN_SELECTOR;
    private static final String MD5_PREFIX = "ACSHASH";

    // pattern used to parse paths in the filter - group 1 = path; group 2 = md5; group 3 = extension
    private static final Pattern FILTER_PATTERN = Pattern.compile("(.*?)\\.(?:min.)?" + MD5_PREFIX + "([a-zA-Z0-9]+)\\.(js|css)");

    private Cache<VersionedClientLibraryMd5CacheKey, String> md5Cache;

    private boolean disableVersioning;

    private boolean enforceMd5;

    @Reference
    private HtmlLibraryManager htmlLibraryManager;

    private ServiceRegistration filterReg;

    public VersionedClientlibsTransformerFactory() throws NotCompliantMBeanException {
        super(GenericCacheMBean.class);
    }

    @Activate
    protected void activate(ComponentContext componentContext) {
        Dictionary<?, ?> props = componentContext.getProperties();
        final int size = PropertiesUtil.toInteger(props.get(PROP_MD5_CACHE_SIZE), DEFAULT_MD5_CACHE_SIZE);
        this.md5Cache = CacheBuilder.newBuilder().recordStats().maximumSize(size).build();
        this.disableVersioning = PropertiesUtil.toBoolean(props.get(PROP_DISABLE_VERSIONING), DEFAULT_DISABLE_VERSIONING);
        this.enforceMd5 = PropertiesUtil.toBoolean(props.get(PROP_ENFORCE_MD5), DEFAULT_ENFORCE_MD5);
        if (enforceMd5) {
            Dictionary<Object, Object> filterProps = new Hashtable<Object, Object>();
            filterProps.put("sling.filter.scope", "REQUEST");
            filterProps.put("service.ranking", Integer.valueOf(0));

            filterReg = componentContext.getBundleContext().registerService(Filter.class.getName(),
                    new BadMd5VersionedClientLibsFilter(), filterProps);
        }
    }

    @Deactivate
    protected void deactivate() {
        this.md5Cache = null;
        if (filterReg != null) {
            filterReg.unregister();;
            filterReg = null;
        }
    }

    public Transformer createTransformer() {
        return new VersionableClientlibsTransformer();
    }

    private Attributes versionClientLibs(final String elementName, final Attributes attrs, final String contextPath) {
        if (this.isCSS(elementName, attrs)) {
            return this.rebuildAttributes(new AttributesImpl(attrs), attrs.getIndex("", ATTR_CSS_PATH),
                    attrs.getValue("", ATTR_CSS_PATH), LibraryType.CSS, contextPath);

        } else if (this.isJavaScript(elementName, attrs)) {
            return this.rebuildAttributes(new AttributesImpl(attrs), attrs.getIndex("", ATTR_JS_PATH),
                    attrs.getValue("", ATTR_JS_PATH), LibraryType.JS, contextPath);

        } else {
            return attrs;
        }
    }

    private Attributes rebuildAttributes(final AttributesImpl newAttributes, final int index, final String path,
                                         final LibraryType libraryType, final String contextPath) {
        String libraryPath = path;
        if (StringUtils.isNotBlank(contextPath)) {
            libraryPath = path.substring(contextPath.length());
        }

        String versionedPath = this.getVersionedPath(libraryPath, libraryType);

        if (StringUtils.isNotBlank(versionedPath)) {
            if(StringUtils.isNotBlank(contextPath)) {
                versionedPath = contextPath + versionedPath;
            }
            log.debug("Rewriting to: {}", versionedPath);
            newAttributes.setValue(index, versionedPath);
        } else {
            log.debug("Versioned Path could not be created properly");
        }

        return newAttributes;
    }

    private boolean isCSS(final String elementName, final Attributes attrs) {
        final String type = attrs.getValue("", "type");
        final String href = attrs.getValue("", "href");

        if (StringUtils.equals("link", elementName)
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
                if (enforceMd5) {
                    builder.append(MD5_PREFIX);
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

    @Nonnull private String getMd5(@Nonnull final HtmlLibrary htmlLibrary) throws IOException, ExecutionException {
        return md5Cache.get(new VersionedClientLibraryMd5CacheKey(htmlLibrary), new Callable<String>() {

            @Override
            public String call() throws Exception {
                return calculateMd5(htmlLibrary);
            }
        });
    }

    @Nonnull private String calculateMd5(@Nonnull final HtmlLibrary htmlLibrary) throws IOException {
        return DigestUtils.md5Hex(htmlLibrary.getInputStream());
    }

    @Nullable private String calculateMd5(SlingHttpServletRequest slingRequest) throws IOException {
        HtmlLibrary library = htmlLibraryManager.getLibrary(slingRequest);
        if (library == null) {
            log.debug("No Client Library found for {}", slingRequest.getRequestURI());
            return null;
        } else {
            return calculateMd5(library);
        }
    }

    private class VersionableClientlibsTransformer extends AbstractTransformer {

        private String contextPath;

        @Override
        public void init(ProcessingContext context, ProcessingComponentConfiguration config) throws IOException {
            super.init(context, config);
            this.contextPath = context.getRequest().getContextPath();
        }

        public void startElement(final String namespaceURI, final String localName, final String qName,
                                 final Attributes attrs)
                throws SAXException {
            final Attributes nextAttributes;
            if (disableVersioning) {
                nextAttributes = attrs;
            } else {
                nextAttributes = versionClientLibs(localName, attrs, contextPath);
            }
            getContentHandler().startElement(namespaceURI, localName, qName, nextAttributes);
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
        return cacheObj.getBytes(Charset.forName("UTF-8")).length;
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

    @Nonnull
    static UriInfo getUriInfo(@Nullable final String uri) {
        if (uri != null) {
            Matcher matcher = FILTER_PATTERN.matcher(uri);
            if (matcher.matches()) {
                return new UriInfo(matcher.group(1) + "." + matcher.group(3), matcher.group(2));
            } else {
                return new UriInfo(uri, "");
            }
        } else {
            return new UriInfo("", "");
        }
    }

    class BadMd5VersionedClientLibsFilter implements Filter {

        @Override
        public void doFilter(final ServletRequest request,
                             final ServletResponse response,
                             final FilterChain filterChain) throws IOException, ServletException {
            if (request instanceof SlingHttpServletRequest && response instanceof SlingHttpServletResponse) {
                final SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;
                final SlingHttpServletResponse slingResponse = (SlingHttpServletResponse) response;
                String uri = slingRequest.getRequestURI();
                if (uri.endsWith(".js") || uri.endsWith(".css")) {
                    UriInfo uriInfo = getUriInfo(uri);
                    if ("".equals(uriInfo.md5)) {
                        log.debug("MD5 is blank for '{}' in Versioned ClientLibs cache, allowing {} to pass", uriInfo.cleanedUri, uri);
                        filterChain.doFilter(request, response);
                        return;
                    }

                    String md5FromCache = null;
                    try {
                        md5FromCache = getCacheEntry(uriInfo.cleanedUri);
                    } catch (Exception e) {
                        md5FromCache = null;
                    }

                    // this static value "Invalid cache key parameter." happens when the cache key can't be
                    // found in the cache
                    if ("Invalid cache key parameter.".equals(md5FromCache)) {
                        md5FromCache = calculateMd5(slingRequest);
                    }

                    if (md5FromCache == null) {
                        // something went bad during the cache access
                        log.warn("Failed to fetch data from Versioned ClientLibs cache, allowing {} to pass", uri);
                        filterChain.doFilter(request, response);
                    } else {
                        // the file is in the cache, compare the md5 from cache with the one in the request
                        if (md5FromCache.equals(uriInfo.md5)) {
                            log.debug("MD5 equals for '{}' in Versioned ClientLibs cache, allowing {} to pass", uriInfo.cleanedUri, uri);
                            filterChain.doFilter(request, response);
                        } else {
                            log.info("MD5 differs for '{}' in Versioned ClientLibs cache. Expected {}. Sending 404 for '{}'",
                                    new Object[] { uriInfo.cleanedUri, uriInfo.md5, uri });
                            slingResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
                        }
                    }
                } else {
                    filterChain.doFilter(request, response);
                }
            } else {
                filterChain.doFilter(request, response);
            }
        }

        @Override
        public void init(final FilterConfig filterConfig) throws ServletException {}

        @Override
        public void destroy() {}
    }

    static class UriInfo {
        private final String cleanedUri;
        private final String md5;

        UriInfo(String cleanedUri, String md5) {
            this.cleanedUri = cleanedUri;
            this.md5 = md5;
        }
    }
}