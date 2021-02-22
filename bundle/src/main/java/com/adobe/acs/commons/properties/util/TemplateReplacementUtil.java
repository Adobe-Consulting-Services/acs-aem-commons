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
package com.adobe.acs.commons.properties.util;

import java.util.List;

import com.google.common.collect.Lists;

import org.apache.commons.lang.StringUtils;

/**
 * Util class used to provide helper methods for finding and replacing the tokens used in this feature.
 */
public class TemplateReplacementUtil {

    private TemplateReplacementUtil() {
    }

    private static final String PLACEHOLDER_BEGIN = "{{";
    private static final String PLACEHOLDER_END = "}}";

    /**
     * Checks the current string for whether or not it contains a placeholder.
     *
     * @param string The string to check for a placeholder
     * @return Whether or not the string contains the placeholder values
     */
    public static boolean hasPlaceholder(String string) {
        return string.contains(PLACEHOLDER_BEGIN) && string.contains(PLACEHOLDER_END)
                && string.indexOf(PLACEHOLDER_BEGIN) < string.indexOf(PLACEHOLDER_END);
    }

    /**
     * Takes the current string and returns the first placeholder value. Ex: {{value}}
     *
     * @param string The full input string
     * @return The substring of the placeholder
     */
    public static String getPlaceholder(String string) {
        return string.substring(
                string.indexOf(PLACEHOLDER_BEGIN),
                string.indexOf(PLACEHOLDER_END) + PLACEHOLDER_END.length());
    }

    /**
     * Takes the current string and returns the placeholder value. Ex: {{value}}
     *
     * @param string The full input string
     * @return A list of placeholders
     */
    public static List<String> getPlaceholders(String string) {
        String[] placeholders = StringUtils.substringsBetween(
                StringUtils.defaultString(string),
                PLACEHOLDER_BEGIN,
                PLACEHOLDER_END);

        // StringUtils strips off the delimiters so add them back
        if (placeholders != null) {
            for (int i = 0; i < placeholders.length; i++) {
                placeholders[i] = PLACEHOLDER_BEGIN + placeholders[i] + PLACEHOLDER_END;
            }
        }

        return placeholders != null ? Lists.newArrayList(placeholders) : Lists.newArrayList();
    }

    /**
     * Takes the current placeholder value and returns the key inside of it. This is used in
     * conjunction with the {@link com.adobe.acs.commons.properties.PropertyAggregatorService}
     * properties.
     *
     * @param placeholder The placeholder input
     * @return The key present inside the placeholder
     */
    public static String getKey(String placeholder) {
        return placeholder.replace(PLACEHOLDER_BEGIN, "").replace(PLACEHOLDER_END, "");
    }
}
