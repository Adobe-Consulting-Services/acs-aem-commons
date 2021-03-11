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
package com.adobe.acs.commons.ccvar.util;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Util class used to provide helper methods for finding and replacing the tokens used in this feature.
 */
public class ContentVariableReplacementUtil {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{([a-zA-Z0-9_:\\-]+\\.[a-zA-Z0-9_:\\-]+)}}");
    private static final String PLACEHOLDER_BEGIN = "{{";
    private static final String PLACEHOLDER_END = "}}";

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
     * Takes the current string and returns the placeholder value. Ex: {{value}}
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
}
