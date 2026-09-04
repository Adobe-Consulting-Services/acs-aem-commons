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

import com.adobe.acs.commons.rewriter.ContentHandlerBasedTransformer;
import com.adobe.acs.commons.util.ParameterUtil;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.rewriter.ProcessingComponentConfiguration;
import org.apache.sling.rewriter.ProcessingContext;
import org.apache.sling.rewriter.Transformer;
import org.apache.sling.rewriter.TransformerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import java.io.IOException;
import java.util.Map;

/**
 * Rewriter pipeline component which maps attribute values.
 */
@Component(
        label = "ACS AEM Commons - Resource Resolver Map Rewriter",
        description = "Rewriter pipeline component which resourceResolver.map's any element/attribute.",
        metatype = true,
        configurationFactory = true,
        policy = ConfigurationPolicy.REQUIRE)
@Properties({ 
    @Property(
            label = "Rewriter Pipeline Type",
            description = "Type identifier to be referenced in rewriter pipeline configuration.",
            name = "pipeline.type",
            value = "resourceresolver-map",
            propertyPrivate = true),
    @Property(
            name = "webconsole.configurationFactory.nameHint",
            value = "Pipeline Type: {pipeline.type}, for element:attributes [{attributes}]")
})
@Service
public final class ResourceResolverMapTransformerFactory implements TransformerFactory {

    private static final Logger log = LoggerFactory.getLogger(ResourceResolverMapTransformerFactory.class);

    private static final String[] DEFAULT_ATTRIBUTES = new String[]{"img:src"};
    private Map<String, String[]> attributes;
    @Property(label = "Rewrite Attributes",
            description = "List of element/attribute pairs to rewrite",
            cardinality = Integer.MAX_VALUE,
            value = {"img:src"})
    private static final String PROP_ATTRIBUTES = "attributes";

    public Transformer createTransformer() {
        return new ResourceResolverMapTransformer();
    }

    protected Attributes rebuildAttributes(final SlingHttpServletRequest slingRequest,
                                         final String elementName, final Attributes attrs) {
        if (slingRequest == null || !attributes.containsKey(elementName)) {
            // element is not defined as a candidate to rewrite
            return attrs;
        }
        final String[] modifiableAttributes = attributes.get(elementName);

        // clone the attributes
        final AttributesImpl newAttrs = new AttributesImpl(attrs);
        final int len = newAttrs.getLength();

        for (int i = 0; i < len; i++) {
            final String attrName = newAttrs.getLocalName(i);
            if (ArrayUtils.contains(modifiableAttributes, attrName)) {
                final String attrValue = newAttrs.getValue(i);
                if (isMappableAbsolutePath(attrValue)) {
                    newAttrs.setValue(i, mapAttributeValue(slingRequest, attrValue));
                }
            }
        }
        return newAttrs;
    }

    /**
     * Only absolute paths (starting with a single {@code /}) are mapped; relative-scheme URLs
     * starting with {@code //} are left untouched.
     */
    private static boolean isMappableAbsolutePath(final String attrValue) {
        return StringUtils.startsWith(attrValue, "/") && !StringUtils.startsWith(attrValue, "//");
    }

    private static String mapAttributeValue(final SlingHttpServletRequest slingRequest, final String attrValue) {
        final int suffixIndex = indexOfQueryOrFragment(attrValue);
        final String path = suffixIndex == -1 ? attrValue : attrValue.substring(0, suffixIndex);
        // Never decode (or otherwise touch) the query string / fragment: ResourceResolver#map
        // passes it through unchanged, so decoding it here would let it carry unescaped
        // characters (e.g., a double quote) straight into the rewritten HTML attribute.
        final String suffix = suffixIndex == -1 ? "" : attrValue.substring(suffixIndex);
        return mapPath(slingRequest, path) + suffix;
    }

    private static String mapPath(final SlingHttpServletRequest slingRequest, final String path) {
        try {
            // The path may already contain percent-encoded characters (eg. a thumbnail
            // rendition path with an encoded "jcr:content"); decode it first so that
            // ResourceResolver#map, which encodes the path it is given, doesn't double-encode it.
            final String pathDecoded = new URLCodec().decode(path);
            return slingRequest.getResourceResolver().map(slingRequest, pathDecoded);
        } catch (DecoderException e) {
            log.error("Could not decode the attribute value", e);
            return slingRequest.getResourceResolver().map(slingRequest, path);
        }
    }

    /**
     * Finds the index of the first query string ({@code ?}) or fragment ({@code #}) marker in the given URL.
     *
     * @param url the URL to inspect
     * @return the index of the first {@code ?} or {@code #}, or {@code -1} if the URL has neither
     */
    private static int indexOfQueryOrFragment(final String url) {
        final int queryIndex = url.indexOf('?');
        final int fragmentIndex = url.indexOf('#');
        if (queryIndex == -1) {
            return fragmentIndex;
        }
        if (fragmentIndex == -1) {
            return queryIndex;
        }
        return Math.min(queryIndex, fragmentIndex);
    }

    @Activate
    protected void activate(final Map<String, Object> config) {
        final String[] test = PropertiesUtil.toStringArray(config.get(PROP_ATTRIBUTES), new String[]{});

        String[] normalizedValue = PropertiesUtil.toStringArray(config.get(PROP_ATTRIBUTES), DEFAULT_ATTRIBUTES);

        if (test.length == 1 && StringUtils.contains(test[0], ",")) {
            normalizedValue = StringUtils.split(test[0], ",");
        }

        this.attributes = ParameterUtil.toMap(normalizedValue, ":", ",");
    }

    public final class ResourceResolverMapTransformer extends ContentHandlerBasedTransformer {

        private SlingHttpServletRequest slingRequest;

        @Override
        public void init(ProcessingContext context, ProcessingComponentConfiguration config) throws IOException {
            super.init(context, config);
            this.slingRequest = context.getRequest();
        }

        @Override
        public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
                throws SAXException {
            getContentHandler().startElement(namespaceURI, localName, qName,
                    rebuildAttributes(this.slingRequest, localName, atts));
        }
    }
}