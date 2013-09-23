package com.adobe.acs.commons.util;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;

/**
 * Util class to help with parsing URIs and PathInfos
 */
public class PathInfoUtil {

    private PathInfoUtil() {
    }

    /**
     * Get a named Query Parameter from the Request
     *
     * @param request
     * @param key
     * @return
     */
    public static String getQueryParam(SlingHttpServletRequest request, String key) {
        return request.getParameter(key);
    }

    /**
     * Get a named Query Parameter from the Request
     *
     * @param request
     * @param key
     * @param dfault  Value to return if Query Parameter value is blank
     * @return
     */
    public static String getQueryParam(SlingHttpServletRequest request, String key, String dfault) {
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
    public static String getSelector(SlingHttpServletRequest request, int index) {
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
    public static String getSelector(SlingHttpServletRequest request, int index, String defaultValue) {
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
    public static String getSuffixSegment(SlingHttpServletRequest request, int index) {
        RequestPathInfo pathInfo = request.getRequestPathInfo();
        if (pathInfo == null || pathInfo.getSuffix() == null) {
            return null;
        }

        String[] suffixes = StringUtils.split(pathInfo.getSuffix(), '/');

        if (index >= 0 && index < suffixes.length) {
            return suffixes[index];
        } else {
            return null;
        }
    }

    /**
     * Get the entire suffix
     *
     * @param request
     * @return Returns null if Request's pathInfo or Suffix is null
     */
    public static String getSuffix(SlingHttpServletRequest request) {
        RequestPathInfo pathInfo = request.getRequestPathInfo();
        if (pathInfo == null || pathInfo.getSuffix() == null) {
            return null;
        }

        return pathInfo.getSuffix();
    }
}
