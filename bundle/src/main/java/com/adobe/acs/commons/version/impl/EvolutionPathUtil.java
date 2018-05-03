/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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
package com.adobe.acs.commons.version.impl;

import org.apache.commons.lang.StringUtils;

public class EvolutionPathUtil {

    private EvolutionPathUtil() {}

    public static int getDepthForPath(String path) {
        return StringUtils.countMatches(StringUtils.substringAfterLast(path, "jcr:frozenNode"), "/");
    }

    public static String getRelativePropertyName(String path) {
        return StringUtils.substringAfterLast(path, "jcr:frozenNode").replaceFirst("/", "");
    }

    public static String getRelativeResourceName(String path) {
        return StringUtils.substringAfterLast(path, "jcr:frozenNode/");
    }

    public static int getLastDepthForPath(String path) {
        return StringUtils.countMatches(StringUtils.substringAfterLast(path, "jcr:content"), "/");
    }

    public static String getLastRelativePropertyName(String path) {
        return StringUtils.substringAfterLast(path, "jcr:content").replaceFirst("/", "");
    }

    public static String getLastRelativeResourceName(String path) {
        return StringUtils.substringAfterLast(path, "jcr:content/");
    }
}
