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
package com.adobe.acs.commons.util;

import aQute.bnd.annotation.ProviderType;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @deprecated use ParameterUtil instead.
 */
@Deprecated
@ProviderType
public class OsgiPropertyUtil {
    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(OsgiPropertyUtil.class);

    private OsgiPropertyUtil() {

    }

    /**
     * Util for parsing Service properties in the form &gt;value&lt;&gt;separator&lt;&gt;value&lt;
     *
     * @param value     must be in the format => x<separator>y  ... ex. foo:bar
     * @param separator separator between the values
     * @return Returns a SimpleEntry representing the key/value pair
     */
    @Deprecated
    public static AbstractMap.SimpleEntry<String, String> toSimpleEntry(final String value, final String separator) {
        return ParameterUtil.toSimpleEntry(value, separator);
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
    @Deprecated
    public static Map<String, String> toMap(final String[] values, final String separator) {
        return ParameterUtil.toMap(values, separator, false, null);
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
    @Deprecated
    public static Map<String, String> toMap(final String[] values, final String separator,
                                            final boolean allowValuelessKeys, final String defaultValue) {
        return ParameterUtil.toMap(values, separator, allowValuelessKeys, defaultValue, false);
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
    @Deprecated
    public static Map<String, String> toMap(final String[] values, 
                                            final String separator,
                                            final boolean allowValuelessKeys, 
                                            final String defaultValue,
                                            final boolean allowMultipleSeparators) {

       return ParameterUtil.toMap(values, separator, allowValuelessKeys, defaultValue, allowMultipleSeparators);
    }

    /**
     * Util for parsing Arrays of Service properties in the form &gt;value&lt;&gt;map-separator&lt;&gt;value&gt;list-separator&lt;&gt;value&lt;&lt;
     *
     * @param values    Array of key/value pairs in the format => [ a<map-separator>b, x<map-separator>y<list-separator>z ] ... ex. ["dog:woof", "cat:meow,purr"]
     * @param mapSeparator separator between the key/values in the amp
     * @param listSeparator separator between the values in each list
     * @return Map of key/value pairs; map.get("dog") => "woof", map.get("cat") => "meow"
     */
    @Deprecated
    public static Map<String, String[]> toMap(final String[] values, 
                                              final String mapSeparator, 
                                              final String listSeparator) {
        return ParameterUtil.toMap(values, mapSeparator, listSeparator);
    }
}