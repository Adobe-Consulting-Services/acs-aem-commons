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

import com.day.cq.commons.jcr.JcrConstants;

public final class EvolutionPathUtil {

    private static final String SEP = "/";

	private EvolutionPathUtil() {}

    public static int getDepthForPath(final String path) {
        return StringUtils.countMatches(StringUtils.substringAfterLast(path, JcrConstants.JCR_FROZENNODE), SEP);
    }

    public static String getRelativePropertyName(final String path) {
        return StringUtils.substringAfterLast(path, JcrConstants.JCR_FROZENNODE).replaceFirst(SEP, "");
    }

    public static String getRelativeResourceName(final String path) {
        return StringUtils.substringAfterLast(path, JcrConstants.JCR_FROZENNODE + SEP);
    }

    public static int getLastDepthForPath(final String path) {
        return StringUtils.countMatches(StringUtils.substringAfterLast(path, JcrConstants.JCR_CONTENT), SEP);
    }

    public static String getLastRelativePropertyName(final String path) {
        return StringUtils.substringAfterLast(path, JcrConstants.JCR_CONTENT).replaceFirst(SEP, "");
    }

    public static String getLastRelativeResourceName(final String path) {
        return StringUtils.substringAfterLast(path, JcrConstants.JCR_CONTENT + SEP);
    }
}
