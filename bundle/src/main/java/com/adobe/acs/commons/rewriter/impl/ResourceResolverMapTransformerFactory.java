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

import com.adobe.acs.commons.rewriter.AbstractTransformer;
import com.adobe.acs.commons.util.ParameterUtil;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.rewriter.ProcessingComponentConfiguration;
import org.apache.sling.rewriter.ProcessingContext;
import org.apache.sling.rewriter.Transformer;
import org.apache.sling.rewriter.TransformerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
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
        configurationPolicy = ConfigurationPolicy.REQUIRE, service=TransformerFactory.class, property= {
        "pipeline.type" + "=" + "resourceresolver-map",
        "webconsole.configurationFactory.nameHint"  + "=" + "Pipeline Type: {pipeline.type}, for element:attributes [{attributes}]"
        })
@Designate(ocd=ResourceResolverMapTransformerFactory.Config.class,factory=true)
public final class ResourceResolverMapTransformerFactory implements TransformerFactory {

    private static final Logger log = LoggerFactory.getLogger(ResourceResolverMapTransformerFactory.class);
    
    @ObjectClassDefinition(name = "ACS AEM Commons - Resource Resolver Map Rewriter",
        description = "Rewriter pipeline component which resourceResolver.map's any element/attribute.")
    public @interface Config {
    
        @AttributeDefinition(name = "Rewrite Attributes",
                description = "List of element/attribute pairs to rewrite",
                defaultValue = {"img:src"})
        String[] attributes();
    }

    private static final String[] DEFAULT_ATTRIBUTES = new String[]{"img:src"};
    private Map<String, String[]> attributes;

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
                if (StringUtils.startsWith(attrValue, "/") && !StringUtils.startsWith(attrValue, "//")) {
                    // Only map absolute paths (starting w /), avoid relative-scheme URLs starting w //
                    try {
                        final String attrValueDecoded = new URLCodec().decode(attrValue);
                        newAttrs.setValue(i, slingRequest.getResourceResolver().map(slingRequest, attrValueDecoded));
                    } catch (DecoderException e) {
                        log.error("Could not decode the attribute value", e);
                        newAttrs.setValue(i, slingRequest.getResourceResolver().map(slingRequest, attrValue));
                    }
                }
            }
        }
        return newAttrs;
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

    public final class ResourceResolverMapTransformer extends AbstractTransformer {

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