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

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

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
    public static AbstractMap.SimpleEntry<String, String> toSimpleEntry(final String value, final String separator) {
        final String[] tmp = StringUtils.split(value, separator);

        if (tmp == null) {
            return null;
        }

        if (tmp.length == 2) {
            return new AbstractMap.SimpleEntry<String, String>(tmp[0], tmp[1]);
        } else {
            return null;
        }
    }

    /**
     * Util for parsing Arrays of Service properties in the form &gt;value&lt;&gt;separator&lt;&gt;value&lt;
     *
     * @param values    Array of key/value pairs in the format => [ a<separator>b, x<separator>y ] ... ex. ["dog:woof", "cat:meow"]
     * @param separator separator between the values
     * @return Map of key/value pairs; map.get("dog") => "woof", map.get("cat") => "meow"
     */
    public static Map<String, String> toMap(final String[] values, final String separator) {
        final Map<String, String> map = new HashMap<String, String>();

        if (values == null || values.length < 1) {
            return map;
        }

        for (final String value : values) {
            final String[] tmp = StringUtils.split(value, separator);
            if (tmp.length == 2) {
                map.put(tmp[0], tmp[1]);
            }
        }

        return map;
    }
}