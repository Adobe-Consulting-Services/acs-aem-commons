/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
package com.adobe.acs.commons.mcp.util;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;

/**
 * String utility methods.
 */
public class StringUtil {

    public static String getFriendlyName(String orig) {
        String[] parts = org.apache.commons.lang.StringUtils.split(orig, "._-");
        if (parts.length == 1) {
            parts = org.apache.commons.lang.StringUtils.splitByCharacterTypeCamelCase(orig);
        }
       return Stream.of(parts).map(String::toLowerCase).map(StringUtils::capitalize).collect(Collectors.joining(" "));
    }    
    
    public static boolean isHex(String str) {
        return str.matches("^[0-9A-Fa-f-]+$");
    }
    
    private StringUtil() {
        // Utility class has no constructuor.
    }
}
