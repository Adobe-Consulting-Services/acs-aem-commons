/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2021 Adobe
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

import com.adobe.acs.commons.ccvar.PropertyAggregatorService;
import com.adobe.acs.commons.ccvar.util.ContentVariableReplacementUtil;
import com.adobe.acs.commons.rewriter.ContentHandlerBasedTransformer;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.rewriter.ProcessingComponentConfiguration;
import org.apache.sling.rewriter.ProcessingContext;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * {@link org.apache.sling.rewriter.Transformer} used to process HTML requests and replace content tokens found in the
 * rendered HTML.
 */
public class ContentVariableTransformer extends ContentHandlerBasedTransformer {

    private Map<String, Object> contentVariableReplacements;
    private PropertyAggregatorService aggregatorService;

    public ContentVariableTransformer() {
    }

    public ContentVariableTransformer(PropertyAggregatorService propertyAggregatorService) {
        this.aggregatorService = propertyAggregatorService;
    }

    @Override
    public void init(ProcessingContext processingContext, ProcessingComponentConfiguration processingComponentConfiguration) throws IOException {
        SlingHttpServletRequest request = processingContext.getRequest();

        contentVariableReplacements = aggregatorService.getProperties(request);
    }

    public void startElement(String uri, String localName, String quaName, Attributes atts) throws SAXException {
        if (shouldRun() && localName.equals("a")) {
            AttributesImpl newAttrs = new AttributesImpl(atts);
            for (int i = 0; i < newAttrs.getLength(); i++) {
                String currentAttribute = newAttrs.getValue(i);
                final List<String> keys = ContentVariableReplacementUtil.getKeys(currentAttribute);
                for (String key : keys) {
                    // If the placeholder key is in the map then replace it
                    if (contentVariableReplacements.containsKey(key)) {
                        String replaceValue = (String) contentVariableReplacements.get(key);
                        newAttrs.setValue(i, currentAttribute.replace(ContentVariableReplacementUtil.getPlaceholder(key), replaceValue));
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
            final List<String> keys = ContentVariableReplacementUtil.getKeys(currentChunk);
            for (String key : keys) {
                // If the placeholder key is in the map then replace it
                if (contentVariableReplacements.containsKey(key)) {
                    final String placeholderReplacement = String.valueOf(contentVariableReplacements.get(key));
                    final String chunkWithReplacements = currentChunk.replace(ContentVariableReplacementUtil.getPlaceholder(key), placeholderReplacement);
                    chunkLength = chunkWithReplacements.length();
                    currentString = replaceOnceAfterStart(currentString, start, currentChunk, chunkWithReplacements);
                    currentChunk = chunkWithReplacements;
                }
            }
        }

        getContentHandler().characters(currentString.toCharArray(), start, chunkLength);
    }

    /**
     * Replaces the first instance of the supplied string after the supplied start value.
     * Example:
     * input = "{{page_properties.pageTitle}} and {{page_properties.pageTitle}}"
     * start = 30
     *
     * output = "{{page_properties.pageTitle}} and actualPageTitle"
     *
     * @param input Full input string with potentially multiple applicable replacements
     * @param start int for the string start
     * @param searchString The string to search for to be replaced
     * @param replacement The string to replace the string found in the search
     * @return The string with the replacement after the specified start
     */
    private String replaceOnceAfterStart(String input, int start, String searchString, String replacement) {
        String afterStart = input.substring(start);
        return input.substring(0, start) + StringUtils.replaceOnce(afterStart, searchString, replacement);
    }

    private boolean shouldRun() {
        return aggregatorService != null && contentVariableReplacements != null;
    }
}
