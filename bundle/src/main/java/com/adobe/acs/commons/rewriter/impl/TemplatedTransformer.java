/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2019 Adobe
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
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import com.adobe.acs.commons.properties.PropertyAggregatorService;
import com.adobe.acs.commons.properties.util.TemplateReplacementUtil;
import com.adobe.acs.commons.rewriter.ContentHandlerBasedTransformer;
import com.adobe.granite.rest.Constants;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.rewriter.ProcessingComponentConfiguration;
import org.apache.sling.rewriter.ProcessingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * {@link org.apache.sling.rewriter.Transformer} used to process HTML requests and replace content tokens found in the
 * rendered HTML.
 */
public class TemplatedTransformer extends ContentHandlerBasedTransformer {

    private static final Logger log = LoggerFactory.getLogger(TemplatedTransformer.class);

    private Map<String, Object> properties;
    private PropertyAggregatorService aggregatorService;

    public TemplatedTransformer() {
    }

    public TemplatedTransformer(PropertyAggregatorService propertyAggregatorService) {
        this.aggregatorService = propertyAggregatorService;
    }

    @Override
    public void init(ProcessingContext processingContext, ProcessingComponentConfiguration processingComponentConfiguration) throws IOException {
        SlingHttpServletRequest request = processingContext.getRequest();
        PageManager pageManager = request.getResourceResolver().adaptTo(PageManager.class);

        // Get the combined properties via service
        Page page = pageManager.getContainingPage(request.getResource());
        if (page != null) {
            properties = aggregatorService.getProperties(page);
        }
    }

    public void startElement(String uri, String localName, String quaName, Attributes atts) throws SAXException {
        if (shouldRun() && localName.equals("a")) {
            AttributesImpl newAttrs = new AttributesImpl(atts);
            for (int i = 0; i < newAttrs.getLength(); i++) {
                String currentAttribute = newAttrs.getValue(i);
                if (TemplateReplacementUtil.hasPlaceholder(currentAttribute)) {

                    // Get the current placeholder in the string
                    String placeholder = TemplateReplacementUtil.getPlaceholder(currentAttribute);

                    // Transform it to the key in the property map
                    String key = TemplateReplacementUtil.getKey(placeholder);

                    // If the placeholder key is in the map then replace it
                    if (properties.containsKey(key)) {
                        String replaceValue = (String) properties.get(key);
                        newAttrs.setValue(i, currentAttribute.replace(placeholder, replaceValue));
                    }
                }
            }
            getContentHandler().startElement(uri, localName, quaName, newAttrs);
        } else {
            getContentHandler().startElement(uri, localName, quaName, atts);
        }
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        String currentString = new String(ch);
        int chunkLength = length;

        if (shouldRun()) {
            String currentChunk = currentString.substring(start, start + length);

            // Get the current placeholders in the string
            final List<String> placeholders = TemplateReplacementUtil.getPlaceholders(currentChunk);

            for (String placeholder : placeholders) {
                // Transform it to the key in the property map
                final String key = TemplateReplacementUtil.getKey(placeholder);

                // If the placeholder key is in the map then replace it
                if (properties.containsKey(key)) {
                    final String placeholderReplacement = String.valueOf(properties.get(key));
                    final String replacedChunk = currentChunk.replace(placeholder, placeholderReplacement);
                    chunkLength = replacedChunk.length();
                    currentString = currentString.replace(currentChunk, replacedChunk);
                    currentChunk = replacedChunk;
                }
            }
        }

        getContentHandler().characters(currentString.toCharArray(), start, chunkLength);
    }

    private boolean shouldRun() {
        return aggregatorService != null && properties != null;
    }
}
