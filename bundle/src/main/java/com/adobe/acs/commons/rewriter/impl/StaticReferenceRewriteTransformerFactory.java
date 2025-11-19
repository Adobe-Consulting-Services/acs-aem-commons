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
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Activate;
import com.adobe.acs.commons.util.ParameterUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.rewriter.Transformer;
import org.apache.sling.rewriter.TransformerFactory;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rewriter pipeline component which rewrites static references.
 */
@Component(+ "for cookie-less domain support", configurationFactory = true)
public final class StaticReferenceRewriteTransformerFactory implements TransformerFactory {

    public final class StaticReferenceRewriteTransformer extends ContentHandlerBasedTransformer {

        public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
                throws SAXException {
            getContentHandler().startElement(namespaceURI, localName, qName, rebuildAttributes(localName, atts));
        }
    }

    private static final Logger log = LoggerFactory.getLogger(StaticReferenceRewriteTransformerFactory.class);

    private static final String ATTR_CLASS = "class";

    private static final String CLASS_NOSTATIC = "nostatic";

    private static final String[] DEFAULT_ATTRIBUTES = new String[]{"img:src", "link:href", "script:src"};

    private static final int DEFAULT_HOST_COUNT = 1;

        private static final String PROP_TAG_ATTRIBUTE_SEPARATOR = "tag.attribute.separator";

        private static final String PROP_LIST_SEPARATOR = "list.separator";

        private static final String PROP_ATTRIBUTES = "attributes";

        private static final String PROP_MATCHING_PATTERNS = "matchingPatterns";

        private static final String PROP_HOST_COUNT = "host.count";

        private static final String PROP_HOST_NAME_PATTERN = "host.pattern";

        private static final String PROP_HOST_SCHEME = "host.scheme";

        private static final String PROP_PREFIXES = "prefixes";

        private static final String PROP_REPLACE_HOST = "replaceHost";

    private Map<String, String[]> attributes;

    private Map<String, Pattern> matchingPatterns;

    private String[] prefixes;

    private int staticHostCount;

    private String[] staticHostPattern;
    private String staticHostScheme;

    private boolean replaceHost;

    public Transformer createTransformer() {
        return new StaticReferenceRewriteTransformer();
    }

    private static String getShardValue(final String filePath, final int shardCount, final ShardNameProvider sharder) {
        int result = 1;
        if (shardCount > 1) {
            final int fileHash = ((filePath.hashCode() & Integer.MAX_VALUE) % shardCount) + 1;
            String hostNumberString = Integer.toString(fileHash);
            if (hostNumberString.length() >= 2) {
                // get the 2nd digit as the 1st digit will not contain "0"
                char c = hostNumberString.charAt(1);
                hostNumberString = Character.toString(c);
                // If there are more than 10 hosts, convert it back to base10
                // so we do not have alpha
                hostNumberString = Integer.toString(Integer.parseInt(hostNumberString, shardCount));

                result = Integer.parseInt(hostNumberString) + 1;
            } else {
                result = fileHash;
            }
        }

        return sharder.lookup(result);
    }

    private String prependHostName(String value) {
        if (staticHostPattern != null && staticHostPattern.length > 0) {
            final String host;
            if (staticHostPattern.length == 1) {
                final String hostNum = getShardValue(value, staticHostCount, toStringShardNameProvider);
                host = staticHostPattern[0].replace("{}", hostNum);
            } else {
                host = getShardValue(value, staticHostPattern.length, lookupShardNameProvider);
            }
            if(StringUtils.isNotBlank(staticHostScheme)) {
                return String.format("%s://%s%s", staticHostScheme, host, value);
            }
            return String.format("//%s%s", host, value);
        } else {
            return value;
        }
    }

    private Attributes rebuildAttributes(final String elementName, final Attributes attrs) {
        if (attributes.containsKey(elementName)) {
            final String[] modifyableAttributes = attributes.get(elementName);

            // first - check for the nostatic class
            boolean rewriteStatic = true;
            for (int i = 0; i < attrs.getLength(); i++) {
                final String attrName = attrs.getLocalName(i);
                if (ATTR_CLASS.equals(attrName)) {
                    String attrValue = attrs.getValue(i);
                    if (attrValue.contains(CLASS_NOSTATIC)) {
                        rewriteStatic = false;
                    }
                }
            }

            if (rewriteStatic) {
                return rebuildAttributes(elementName, attrs, modifyableAttributes);
            }
        }

        return attrs;
    }

    @SuppressWarnings("squid:S3776")
    private Attributes rebuildAttributes(String elementName, Attributes attrs, String[] modifyableAttributes) {
        // clone the attributes
        final AttributesImpl newAttrs = new AttributesImpl(attrs);

        for (int i = 0; i < newAttrs.getLength(); i++) {
            final String attrName = newAttrs.getLocalName(i);
            if (ArrayUtils.contains(modifyableAttributes, attrName)) {
                final String attrValue = newAttrs.getValue(i);

                String key = elementName + ":" + attrName;
                if (matchingPatterns.containsKey(key)) {
                    // Find value based on matching pattern
                    Pattern matchingPattern = matchingPatterns.get(key);
                    try {
                        newAttrs.setValue(i, handleMatchingPatternAttribute(matchingPattern, attrValue));
                    } catch (Exception e) {
                        log.error("Could not perform replacement based on matching pattern", e);
                    }
                } else {
                    for (String prefix : prefixes) {
                        if (attrValue.startsWith(prefix)) {
                            newAttrs.setValue(i, prependHostName(attrValue));
                        }
                    }
                }
            }
        }

        return newAttrs;
    }

    private String handleMatchingPatternAttribute(Pattern pattern, String attrValue) {
        String unescapedValue = StringEscapeUtils.unescapeHtml4(attrValue);
        Matcher m = pattern.matcher(unescapedValue);
        StringBuffer sb = new StringBuffer(unescapedValue.length());

        while (m.find()) {
            String url = m.group(1);
            for (String prefix : prefixes) {
                if (url.startsWith(prefix)) {
                    // prepend host
                    url = prependHostName(url);
                    // Added check to determine whether the existing host has to be replaced
                    if (this.replaceHost) {
                        sb.setLength(0);
                        if (!url.contains("://")) {
                            String reuseScheme = attrValue.substring(0, attrValue.indexOf("://") + 1);
                            sb.append(reuseScheme);
                        }
                        sb.append(url);
                    } else {
                        m.appendReplacement(sb, Matcher.quoteReplacement(url));
                        // First prefix match wins
                    }
                    break;
                }
            }
        }

        if (!this.replaceHost) {
            m.appendTail(sb);
        }

        return sb.toString();
    }

    @Activate
    protected void activate(final ComponentContext componentContext) {
        final Dictionary<?, ?> properties = componentContext.getProperties();

        final String tagAttributeSeparator = PropertiesUtil.toString(properties.get(PROP_TAG_ATTRIBUTE_SEPARATOR), ":");
        final String listSeparator = PropertiesUtil.toString(properties.get(PROP_LIST_SEPARATOR), ",");

        final String[] attrProp = PropertiesUtil
                .toStringArray(properties.get(PROP_ATTRIBUTES), DEFAULT_ATTRIBUTES);
        this.attributes = ParameterUtil.toMap(attrProp, tagAttributeSeparator, listSeparator);

        final String[] matchingPatternsProp = PropertiesUtil.toStringArray(properties.get(PROP_MATCHING_PATTERNS));
        this.matchingPatterns = initializeMatchingPatterns(matchingPatternsProp);

        this.prefixes = PropertiesUtil.toStringArray(properties.get(PROP_PREFIXES), new String[0]);
        this.staticHostPattern = PropertiesUtil.toStringArray(properties.get(PROP_HOST_NAME_PATTERN), null);
        this.staticHostScheme = PropertiesUtil.toString(properties.get(PROP_HOST_SCHEME), "");
        this.staticHostCount = PropertiesUtil.toInteger(properties.get(PROP_HOST_COUNT), DEFAULT_HOST_COUNT);
        this.replaceHost = PropertiesUtil.toBoolean(properties.get(PROP_REPLACE_HOST), false);

        if (!this.replaceHost && matchingPatterns.values().stream().noneMatch(str -> str.toString().startsWith("^"))) {
            log.warn("BEWARE! Replace host is false and your regex is not anchored to the start of the string, this may result in a double host.");
        }
    }

    private static Map<String, Pattern> initializeMatchingPatterns(String[] matchingPatternsProp) {
        Map<String, Pattern> result = new HashMap<>();

        Map<String, String> map = ParameterUtil.toMap(matchingPatternsProp, ";");

        for (Map.Entry<String, String> entry : map.entrySet()) {
            String matchingPatternString = entry.getValue();
            try {
                Pattern compiled = Pattern.compile(matchingPatternString);
                result.put(entry.getKey(), compiled);
            } catch (Exception e) {
                log.warn("Could not compile pattern {} for {}. Ignoring it", matchingPatternString, entry.getKey());
            }
        }
        return result;
    }

    private interface ShardNameProvider {
        String lookup(int idx);
    }

    @SuppressWarnings("squid:S1604")
    private static final ShardNameProvider toStringShardNameProvider = new ShardNameProvider() {

        @Override
        public String lookup(int idx) {
            return Integer.toString(idx);
        }
    };

    @SuppressWarnings("squid:S1604")
    private final ShardNameProvider lookupShardNameProvider = new ShardNameProvider() {

        @Override
        public String lookup(int idx) {
            return staticHostPattern[idx - 1];
        }
    };
}
