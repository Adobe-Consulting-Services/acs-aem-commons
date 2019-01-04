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
package com.adobe.acs.commons.util;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.ProviderType;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@ProviderType
public class ParameterUtil {
    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(ParameterUtil.class);

    private ParameterUtil() {

    }

    /**
     * Util for parsing Service properties in the form {@code <value><separator><value>}
     *
     * @param value     must be in the format => {@code x<separator>y}  ... ex. {@code foo:bar}
     * @param separator separator between the values
     * @return Returns a {@link Map.Entry} representing the key/value pair. The entry's value may be {@code null} in case no separator is found.
     */
    public static Map.Entry<String, String> toMapEntryWithOptionalValue(final String value, final String separator) {
        return toSimpleEntry(value, separator, true);
    }
    
    /**
     * Util for parsing Service properties in the form {@code <value><separator><value>}
     *
     * @param value     must be in the format => {@code x<separator>y}  ... ex. {@code foo:bar}
     * @param separator separator between the values
     * @return Returns a {@link Map.Entry} representing the key/value pair. It may be {@code null} in case no separator is found.
     */
    public static Map.Entry<String, String> toMapEntry(final String value, final String separator) {
        return toSimpleEntry(value, separator, false);
    }

    /**
     * Util for parsing Service properties in the form {@code <value><separator><value>}
     *
     * @param value     must be in the format => {@code x<separator>y}  ... ex. {@code foo:bar}
     * @param separator separator between the values
     * @param isValueOptional if {@code false} returns {@code null} in case there is not at least one separator found (not at the last position) 
     * @return Returns a SimpleEntry representing the key/value pair. The value may be {@null} in case no separator is found and {@code isValueOptional} is {@code true}.
     */
    private static AbstractMap.SimpleEntry<String, String> toSimpleEntry(final String value, final String separator, boolean isValueOptional) {
        final String[] tmp = StringUtils.split(value, separator, 2);

        if (tmp == null) {
            return null;
        }

        if (tmp.length == 2) {
            return new AbstractMap.SimpleEntry<String, String>(tmp[0], tmp[1]);
        } else {
            if (isValueOptional && tmp.length == 1) {
                return new AbstractMap.SimpleEntry<String, String>(tmp[0], null);
            }
            return null;
        }
    }
    

    /**
     * Util for parsing Arrays of Service properties in the form &gt;value&lt;&gt;separator&lt;&gt;value&lt;
     *
     * If a value is missing from a key/value pair, the entry is rejected. To keep valueless keys used the
     * overloaded version of this function with allowValuelessKeys = true
     *
     * @param values    Array of key/value pairs in the format => [ a<separator>b, x<separator>y ] ... ex. ["dog:woof", "cat:meow"]
     * @param separator separator between the values
     * @return Map of key/value pairs; map.get("dog") => "woof", map.get("cat") => "meow"
     */
    public static Map<String, String> toMap(final String[] values, final String separator) {
        return toMap(values, separator, false, null);
    }

    /**
     * Util for parsing Arrays of Service properties in the form &gt;value&lt;&gt;separator&lt;&gt;value&lt;
     *
     * If a value is missing from a key/value pair, the entry is rejected only if allowValuelessKyes is false.
     * To keep the valueless keys pass in allowValuelessKeys => true
     *
     * *
     * @param values Array of key/value pairs in the format => [ a<separator>b, x<separator>y ] ... ex. ["dog:woof", "cat:meow"]
     * @param separator separator between the values
     * @param allowValuelessKeys true is keys are allowed without associated values
     * @param defaultValue default value to use if a value for a key is not present and allowValuelessKeys is true
     * @return
     */
    public static Map<String, String> toMap(final String[] values, final String separator,
                                            final boolean allowValuelessKeys, final String defaultValue) {
        return toMap(values, separator, allowValuelessKeys, defaultValue, false);
    }

    /**
     * Util for parsing Arrays of Service properties in the form &gt;value&lt;&gt;separator&lt;&gt;value&lt;
     *
     * If a value is missing from a key/value pair, the entry is rejected only if allowValuelessKyes is false.
     * To keep the valueless keys pass in allowValuelessKeys => true
     *
     * *
     * @param values Array of key/value pairs in the format => [ a<separator>b, x<separator>y ] ... ex. ["dog:woof", "cat:meow"]
     * @param separator separator between the values
     * @param allowValuelessKeys true is keys are allowed without associated values
     * @param defaultValue default value to use if a value for a key is not present and allowValuelessKeys is true
     * @param allowMultipleSeparators if true, multiple separators are allowed per entry in which case only the first is considered.
     *                                If false, entries with multiple separators are considered invalid
     * @return
     */
    public static Map<String, String> toMap(final String[] values, final String separator,
                                            final boolean allowValuelessKeys, final String defaultValue,
                                            final boolean allowMultipleSeparators) {

        final Map<String, String> map = new LinkedHashMap<String, String>();

        if (values == null || values.length < 1) {
            return map;
        }

        for (final String value : values) {
            final String[] tmp = StringUtils.split(value, separator, allowMultipleSeparators ? 2 : -1);

            if(tmp.length == 1 && allowValuelessKeys) {
                if(StringUtils.startsWith(value, separator)) {
                    // Skip keyless values
                    continue;
                }

                map.put(tmp[0], defaultValue);
            } else if (tmp.length == 2) {
                map.put(tmp[0], tmp[1]);
            }
        }

        return map;
    }

    /**
     * Util for parsing Arrays of Service properties in the form &gt;value&lt;&gt;map-separator&lt;&gt;value&gt;list-separator&lt;&gt;value&lt;&lt;
     *
     * @param values    Array of key/value pairs in the format => [ a<map-separator>b, x<map-separator>y<list-separator>z ] ... ex. ["dog:woof", "cat:meow,purr"]
     * @param mapSeparator separator between the key/values in the amp
     * @param listSeparator separator between the values in each list
     * @return Map of key/value pairs; map.get("dog") => "woof", map.get("cat") => "meow"
     */
    public static Map<String, String[]> toMap(final String[] values, final String mapSeparator, final String listSeparator) {
        final Map<String, String> map = toMap(values, mapSeparator);
        final Map<String, String[]> result = new LinkedHashMap<String, String[]>(map.size());
        for (final Map.Entry<String, String> entry : map.entrySet()) {
            result.put(entry.getKey(), StringUtils.split(entry.getValue(), listSeparator));
        }
        return result;
    }

    /**
     * Util for converting a String[] into a List of compiled Patterns. Empty/blank strings will be skipped.
     * @param values the Strings to convert to patterns.
     * @return a List of Patterns
     */
    public static List<Pattern> toPatterns(String[] values) {
        List<Pattern> patterns = new ArrayList<Pattern>();

        if(values == null) {
            return patterns;
        }

        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                patterns.add(Pattern.compile(value));
            }
        }

        return patterns;
    }
}