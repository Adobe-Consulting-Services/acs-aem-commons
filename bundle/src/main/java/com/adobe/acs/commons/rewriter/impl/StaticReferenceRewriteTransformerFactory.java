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

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.rewriter.Transformer;
import org.apache.sling.rewriter.TransformerFactory;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.adobe.acs.commons.rewriter.AbstractTransformer;
import com.adobe.acs.commons.util.ParameterUtil;

/**
 * Rewriter pipeline component which rewrites static references.
 *
 */
@Component(
        label = "ACS AEM Commons - Static Reference Rewriter",
        description = "Rewriter pipeline component which rewrites host name on static references "
                + "for cookie-less domain support",
        metatype = true, configurationFactory = true, policy = ConfigurationPolicy.REQUIRE)
@Service
@Properties({
    @Property(
            name = "pipeline.type", label = "Rewriter Pipeline Type",
            description = "Type identifier to be referenced in rewriter pipeline configuration."),
    @Property(
            name = "webconsole.configurationFactory.nameHint",
            value = "Pipeline: {pipeline.type}")
})

public final class StaticReferenceRewriteTransformerFactory implements TransformerFactory {

    public final class StaticReferenceRewriteTransformer extends AbstractTransformer {

        public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
                throws SAXException {
            getContentHandler().startElement(namespaceURI, localName, qName, rebuildAttributes(localName, atts));
        }
    }

    private static final Logger log = LoggerFactory.getLogger(StaticReferenceRewriteTransformerFactory.class);

    private static final String ATTR_CLASS = "class";

    private static final String CLASS_NOSTATIC = "nostatic";

    private static final String[] DEFAULT_ATTRIBUTES = new String[] { "img:src", "link:href", "script:src" };

    private static final int DEFAULT_HOST_COUNT = 1;

    @Property(label = "Rewrite Attributes", description = "List of element/attribute pairs to rewrite", value = {
            "img:src", "link:href", "script:src" })
    private static final String PROP_ATTRIBUTES = "attributes";

    @Property(label = "Matching Patterns", description = "List of patterns how to find url to prepend host to for more complex values. The url must be the first matching group within the pattern.")
    private static final String PROP_MATCHING_PATTERNS = "matchingPatterns";

    @Property(intValue = DEFAULT_HOST_COUNT, label = "Static Host Count",
            description = "Number of static hosts available.")
    private static final String PROP_HOST_COUNT = "host.count";

    @Property(label = "Static Host Pattern", description = "Pattern for generating static host domain names. "
            + "'{}' will be replaced with the host number. If more than one is provided, the host count is ignored.", unbounded = PropertyUnbounded.ARRAY)
    private static final String PROP_HOST_NAME_PATTERN = "host.pattern";

    @Property(unbounded = PropertyUnbounded.ARRAY, label = "Path Prefixes",
            description = "Path prefixes to rewrite.")
    private static final String PROP_PREFIXES = "prefixes";

    private Map<String, String[]> attributes;

    private Map<String, Pattern> matchingPatterns;

    private String[] prefixes;

    private int staticHostCount;

    private String[] staticHostPattern;

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
                Character c = hostNumberString.charAt(1);
                hostNumberString = c.toString();
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
        String unescapedValue = StringEscapeUtils.unescapeHtml(attrValue);
        Matcher m = pattern.matcher(unescapedValue);
        StringBuffer sb = new StringBuffer(unescapedValue.length());

        while (m.find()) {
            String url = m.group(1);
            for (String prefix : prefixes) {
                if (url.startsWith(prefix)) {
                    // prepend host
                    url = prependHostName(url);
                    m.appendReplacement(sb, Matcher.quoteReplacement(url));
                    // First prefix match wins
                    break;
                }
            }

        }
        m.appendTail(sb);

        return sb.toString();
    }

    @Activate
    protected void activate(final ComponentContext componentContext) {
        final Dictionary<?, ?> properties = componentContext.getProperties();

        final String[] attrProp = PropertiesUtil
                .toStringArray(properties.get(PROP_ATTRIBUTES), DEFAULT_ATTRIBUTES);
        this.attributes = ParameterUtil.toMap(attrProp, ":", ",");

        final String[] matchingPatternsProp = PropertiesUtil.toStringArray(properties.get(PROP_MATCHING_PATTERNS));
        this.matchingPatterns = initializeMatchingPatterns(matchingPatternsProp);

        this.prefixes = PropertiesUtil.toStringArray(properties.get(PROP_PREFIXES), new String[0]);
        this.staticHostPattern = PropertiesUtil.toStringArray(properties.get(PROP_HOST_NAME_PATTERN), null);
        this.staticHostCount = PropertiesUtil.toInteger(properties.get(PROP_HOST_COUNT), DEFAULT_HOST_COUNT);
    }

    private static Map<String, Pattern> initializeMatchingPatterns(String[] matchingPatternsProp) {
        Map<String, Pattern> result = new HashMap<String, Pattern>();

        Map<String, String> map = ParameterUtil.toMap(matchingPatternsProp, ";");

        Iterator<String> iterator = map.keySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            String matchingPatternString = map.get(key);
            try {
                Pattern compiled = Pattern.compile(matchingPatternString);
                result.put(key, compiled);
            } catch (Exception e) {
                log.warn("Could not compile pattern {} for {}. Ignoring it", matchingPatternString, key);
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
    private ShardNameProvider lookupShardNameProvider = new ShardNameProvider() {

        @Override
        public String lookup(int idx) {
            return staticHostPattern[idx - 1];
        }
    };
}
