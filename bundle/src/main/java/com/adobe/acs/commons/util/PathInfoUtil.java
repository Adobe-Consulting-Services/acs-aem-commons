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
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;

import aQute.bnd.annotation.ProviderType;

/**
 * Util class to help with parsing URIs and PathInfos.
 */
@ProviderType
public final class PathInfoUtil {

    private PathInfoUtil() {
    }

    /**
     * Get a named Query Parameter from the Request.
     *
     * @param request
     * @param key
     * @return
     */
    public static String getQueryParam(final SlingHttpServletRequest request, final String key) {
        return request.getParameter(key);
    }

    /**
     * Get a named Query Parameter from the Request.
     *
     * @param request
     * @param key
     * @param dfault  Value to return if Query Parameter value is blank
     * @return
     */
    public static String getQueryParam(final SlingHttpServletRequest request, final String key, final String dfault) {
        String tmp = request.getParameter(key);

        if (StringUtils.isBlank(tmp)) {
            return dfault;
        }

        return tmp;
    }

    /**
     * <p>
     * Gets the selector at the supplied index.
     * </p><p>
     * Given: /content/page.selA.selB.html
     * <br/>
     * getSelector(request, 0) // --> "selA"
     * <br/>
     * getSelector(request, 1) // --> "selB"
     * </p>
     *
     * @param request
     * @param index
     * @return null if selector cannot be found at the specified index
     */
    public static String getSelector(final SlingHttpServletRequest request, final int index) {
        return getSelector(request, index, null);
    }

    /**
     * <p>
     * Gets the selector at the supplied index, using a default if
     * there is no selector at that index.
     * </p><p>
     * Given: /content/page.selA.html
     * <br/>
     * getSelector(request, 0, "default") // --> "selA"
     * <br/>
     * getSelector(request, 1, "default2") // --> "default2"
     * </p>
     *
     * @param request the request
     * @param index the index
     * @param defaultValue the default value
     * @return the selector value or the default
     */
    public static String getSelector(final SlingHttpServletRequest request,
                                     final int index, final String defaultValue) {
        RequestPathInfo pathInfo = request.getRequestPathInfo();
        if (pathInfo == null) {
            return null;
        }

        String[] selectors =  pathInfo.getSelectors();
        if (selectors == null) {
            return null;
        }

        if (index >= 0 && index < selectors.length) {
            return selectors[index];
        } else {
            return defaultValue;
        }
    }

    /**
     * Gets the suffixes as an array; each segment is the text between the /'s.
     *
     * /segment-0/segment-1/segment-2
     *
     * @param request
     * @return and array of the suffix segments or empty array
     */
    public static String[] getSuffixSegments(final SlingHttpServletRequest request) {
        RequestPathInfo pathInfo = request.getRequestPathInfo();
        if (pathInfo == null || pathInfo.getSuffix() == null) {
            return new String[] {};
        }

        return StringUtils.split(pathInfo.getSuffix(), '/');
    }

    /**
     * <p>
     * Gets the suffix segment at the supplied index.
     * </p><p>
     * Given: /content/page.html/suffixA/suffixB
     * <br/>
     * getSuffixSegment(request, 0) // --> "suffixA"
     * <br/>
     * getSuffixSegment(request, 1) // --> "suffixB"
     * </p>
     *
     * @param request
     * @param index
     * @return null if suffix segment cannot be found at the specified index
     */
    public static String getSuffixSegment(final SlingHttpServletRequest request, int index) {
        final String[] suffixes = getSuffixSegments(request);

        if (index >= 0 && index < suffixes.length) {
            return suffixes[index];
        } else {
            return null;
        }
    }

    /**
     * Get the entire suffix.
     *
     * @param request
     * @return Returns null if Request's pathInfo or Suffix is null
     */
    public static String getSuffix(final SlingHttpServletRequest request) {
        RequestPathInfo pathInfo = request.getRequestPathInfo();
        if (pathInfo == null || pathInfo.getSuffix() == null) {
            return null;
        }

        return pathInfo.getSuffix();
    }

    /**
     * Get the first suffix segment.
     *
     * @param request
     * @return the String in the first suffix segment or null if no suffix
     */
    public static String getFirstSuffixSegment(final SlingHttpServletRequest request) {
        return getSuffixSegment(request, 0);
    }

    /**
     * Gets the last suffix segment.
     *
     * @param request
     * @return the String in the last suffix segment or null if no suffix
     */
    public static String getLastSuffixSegment(final SlingHttpServletRequest request) {
        final String[] suffixes = getSuffixSegments(request);

        if (suffixes.length < 1) {
            return null;
        } else {
            return suffixes[suffixes.length - 1];
        }
    }
}