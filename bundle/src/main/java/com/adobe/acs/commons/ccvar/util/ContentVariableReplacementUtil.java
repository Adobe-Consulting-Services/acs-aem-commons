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
package com.adobe.acs.commons.ccvar.util;

import com.adobe.acs.commons.ccvar.TransformAction;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.adobe.acs.commons.ccvar.impl.PropertyConfigServiceImpl.PARSER_SEPARATOR;

/**
 * Util class used to provide helper methods for finding and replacing the tokens used in this feature.
 */
public class ContentVariableReplacementUtil {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\(\\(([a-zA-Z0-9_:\\-]+\\.[a-zA-Z0-9_:\\-]+(![a-zA-Z0-9_:\\-]*)?)\\)\\)");
    private static final Map<String, String> REQUIRED_ESCAPE = escapeMap();
    private static final String PLACEHOLDER_BEGIN = "((";
    private static final String PLACEHOLDER_END = "))";

    private ContentVariableReplacementUtil() {
    }

    /**
     * Takes the current key and returns it wrapped with the placeholder containers
     *
     * @param key The full input key
     * @return The substring of the placeholder
     */
    public static String getPlaceholder(String key) {
        return PLACEHOLDER_BEGIN + key + PLACEHOLDER_END;
    }

    /**
     * Checks if the passed map of variable replacements contains the key passed. It has an additional check for when
     * the key contains an action.
     *
     * @param contentVariableReplacements Current map of content variable keys and values
     * @param key Current property key
     * @return Whether the map contains the key
     */
    public static boolean hasKey(Map<String, Object> contentVariableReplacements, String key) {
        if (StringUtils.contains(key, PARSER_SEPARATOR)) {
            String keyWithoutAction = StringUtils.substringBefore(key, PARSER_SEPARATOR);
            return contentVariableReplacements.containsKey(keyWithoutAction);
        }
        return contentVariableReplacements.containsKey(key);
    }

    /**
     * Returns the value from the map based on the key. Handles the special case of when the key contains an action.
     *
     * @param contentVariableReplacements Current map of content variable keys and values
     * @param key Current property key
     * @return The value in the map
     */
    public static Object getValue(Map<String, Object> contentVariableReplacements, String key) {
        if (StringUtils.contains(key, PARSER_SEPARATOR)) {
            String keyWithoutAction = StringUtils.substringBefore(key, PARSER_SEPARATOR);
            return contentVariableReplacements.get(keyWithoutAction);
        }
        return contentVariableReplacements.get(key);
    }

    /**
     * Takes the current string and returns the placeholder value. Ex: ((value))
     *
     * @param string The full input string
     * @return A list of placeholders
     */
    public static List<String> getKeys(String string) {
        List<String> keys = new ArrayList<>();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(string);
        while (matcher.find()) {
            keys.add(StringUtils.substringBetween(
                    StringUtils.defaultString(matcher.group()),
                    PLACEHOLDER_BEGIN,
                    PLACEHOLDER_END));
        }

        return keys;
    }

    /**
     * Utility method to replace values and optionally execute actions on the values to be replaced. The default output
     * of this method will include base escaping of volatile HTML entities. Actions can define whether they explicitly
     * disable this base escaping.
     *
     * @param input The input string containing the placeholders
     * @param key The key containing the property name and an optional action
     * @param replacement The value to be replaced and optionally transformed
     * @param action The action found in the placeholder key
     * @return The fully replaced value
     */
    public static String doReplacement(String input, String key, String replacement, TransformAction action) {
        if (action != null) {
            if (action.disableEscaping()) {
                return input.replace(getPlaceholder(key), action.execute(replacement));
            }
            return input.replace(getPlaceholder(key), baseEscaping(action.execute(replacement)));
        }
        return input.replace(getPlaceholder(key), baseEscaping(replacement));
    }

    /**
     * Applies the base level escaping unless otherwise overridden.
     *
     * @param input String to escape
     * @return Escaped string
     */
    private static String baseEscaping(String input) {
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
}
