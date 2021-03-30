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
import com.adobe.acs.commons.ccvar.PropertyConfigService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link org.apache.sling.rewriter.Transformer} used to process HTML requests and replace content tokens found in the
 * rendered HTML.
 */
public class ContentVariableTransformer extends ContentHandlerBasedTransformer {
    private static final Map<String, String> REQUIRED_ESCAPE = escapeMap();

    private Map<String, Object> contentVariableReplacements;
    private PropertyAggregatorService aggregatorService;
    private PropertyConfigService propertyConfigService;

    public ContentVariableTransformer() {
    }

    public ContentVariableTransformer(PropertyAggregatorService propertyAggregatorService,
                                      PropertyConfigService propertyConfigService) {
        this.aggregatorService = propertyAggregatorService;
        this.propertyConfigService = propertyConfigService;
    }

    @Override
    public void init(ProcessingContext processingContext, ProcessingComponentConfiguration processingComponentConfiguration) throws IOException {
        SlingHttpServletRequest request = processingContext.getRequest();

        contentVariableReplacements = aggregatorService.getProperties(request);
    }

    public void startElement(String uri, String localName, String quaName, Attributes atts) throws SAXException {
        if (shouldRun()) {
            AttributesImpl newAttrs = new AttributesImpl(atts);
            for (int i = 0; i < newAttrs.getLength(); i++) {
                String currentAttribute = newAttrs.getValue(i);
                final List<String> keys = ContentVariableReplacementUtil.getKeys(currentAttribute);
                for (String key : keys) {
                    // If the placeholder key is in the map then replace it
                    if (ContentVariableReplacementUtil.hasKey(contentVariableReplacements, key)) {
                        String replaceValue =
                                (String) ContentVariableReplacementUtil.getValue(contentVariableReplacements, key);
                        String newAttrValue = ContentVariableReplacementUtil.doReplacement(currentAttribute, key,
                                baseEscaping(replaceValue), propertyConfigService.getAction(key));
                        newAttrs.setValue(i, newAttrValue);
                    }
                }
            }
            getContentHandler().startElement(uri, localName, quaName, newAttrs);
        } else {
            getContentHandler().startElement(uri, localName, quaName, atts);
        }
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        String currentString = new String(ch, start, length);

        if (shouldRun()) {

            // Get the current placeholders in the string
            final List<String> keys = ContentVariableReplacementUtil.getKeys(currentString);
            for (String key : keys) {
                // If the placeholder key is in the map then replace it
                if (ContentVariableReplacementUtil.hasKey(contentVariableReplacements, key)) {
                    final String placeholderReplacement =
                            String.valueOf(ContentVariableReplacementUtil.getValue(contentVariableReplacements, key));
                    currentString = ContentVariableReplacementUtil.doReplacement(currentString, key,
                            baseEscaping(placeholderReplacement), propertyConfigService.getAction(key));
                }
            }
        }

        getContentHandler().characters(currentString.toCharArray(), 0, currentString.length());
    }

    /**
     * Applies the base level escaping unless otherwise overridden.
     *
     * @param input String to escape
     * @return Escaped string
     */
    private String baseEscaping(String input) {
        if (propertyConfigService.disableBaseEscaping()) {
            return input;
        }
        for (Map.Entry<String, String> entry : REQUIRED_ESCAPE.entrySet()) {
            if (input.contains(entry.getKey())) {
                input = input.replace(entry.getKey(), entry.getValue());
            }
        }
        return input;
    }

    /**
     * Generates the map of characters to automatically escape
     *
     * @return Map of escape keys/values
     */
    private static Map<String, String> escapeMap() {
        Map<String, String> escapes = new HashMap<>();
        escapes.put("\"", "&quot;");
        escapes.put("'", "&apos;");
        escapes.put("<", "&lt;");
        escapes.put(">", "&gt;");
        return escapes;
    }

    private boolean shouldRun() {
        return aggregatorService != null && contentVariableReplacements != null && contentVariableReplacements.size() > 0;
    }
}
