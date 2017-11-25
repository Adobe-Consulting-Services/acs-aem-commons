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

import aQute.bnd.annotation.ProviderType;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ProviderType
public class CookieUtil {

    private CookieUtil() {
    }

    /**
     * Add the provided HTTP Cookie to the Response
     *
     * @param cookie   Cookie to add
     * @param response Response to add Cookie to
     * @return true unless cookie or response is null
     */
    public static boolean addCookie(final Cookie cookie, final HttpServletResponse response) {
        if (cookie == null || response == null) {
            return false;
        }

        response.addCookie(cookie);
        return true;
    }

    /**
     * Get the named cookie from the HTTP Request
     *
     * @param request    Request to get the Cookie from
     * @param cookieName name of Cookie to get
     * @return the named Cookie, null if the named Cookie cannot be found
     */
    public static Cookie getCookie(final HttpServletRequest request, final String cookieName) {
        if (StringUtils.isBlank(cookieName)) {
            return null;
        }

        final Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        if (cookies.length > 0) {
            for (final Cookie cookie : cookies) {
                if (StringUtils.equals(cookieName, cookie.getName())) {
                    return cookie;
                }
            }
        }

        return null;
    }

    /**
     * Gets Cookies from the Request whose names match the provides Regex
     *
     * @param request Request to get the Cookie from
     * @param regex   Regex to match against Cookie names
     * @return Cookies which match the Regex
     */
    public static List<Cookie> getCookies(final HttpServletRequest request, final String regex) {
        final ArrayList<Cookie> foundCookies = new ArrayList<Cookie>();
        if (StringUtils.isBlank(regex)) {
            return foundCookies;
        }

        final Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Collections.emptyList();
        }

        final Pattern p = Pattern.compile(regex);
        for (final Cookie cookie : cookies) {
            final Matcher m = p.matcher(cookie.getName());
            if (m.matches()) {
                foundCookies.add(cookie);
            }
        }

        return foundCookies;
    }

    /**
     * <p>
     * Extend the cookie life.
     * <p></p>
     * This can be used when a cookie should be valid for X minutes from the last point of activity.
     * <p></p>
     * This method will leave expired or deleted cookies alone.
     * </p>
     *
     * @param request    Request to get the Cookie from
     * @param response   Response to write the extended Cookie to
     * @param cookieName Name of Cookie to extend the life of
     * @param expiry     New Cookie expiry
     */
    public static boolean extendCookieLife(final HttpServletRequest request, final HttpServletResponse response,
                                           final String cookieName, final String cookiePath, final int expiry) {
        final Cookie cookie = getCookie(request, cookieName);
        if (cookie == null) {
            return false;
        }

        if (cookie.getMaxAge() <= 0) {
            return false;
        }

        final Cookie responseCookie = (Cookie) cookie.clone();
        responseCookie.setMaxAge(expiry);
        responseCookie.setPath(cookiePath);

        addCookie(responseCookie, response);

        return true;
    }

    /**
     * Remove the named Cookies from Response
     *
     * @param request     Request to get the Cookies to drop
     * @param response    Response to expire the Cookies on
     * @param cookieNames Names of cookies to drop
     * @return Number of Cookies dropped
     */
    public static int dropCookies(final HttpServletRequest request, final HttpServletResponse response, final String cookiePath, final String... cookieNames) {
        int count = 0;
        if (cookieNames == null) {
            return count;
        }

        final List<Cookie> cookies = new ArrayList<Cookie>();
        for (final String cookieName : cookieNames) {
            cookies.add(getCookie(request, cookieName));
        }

        return dropCookies(response, cookies.toArray(new Cookie[cookies.size()]), cookiePath);
    }

    /**
     * Internal method used for dropping cookies
     *
     * @param response
     * @param cookies
     * @param cookiePath
     * @return
     */
    private static int dropCookies(final HttpServletResponse response, final Cookie[] cookies, final String cookiePath) {
        int count = 0;

        for (final Cookie cookie : cookies) {
            if (cookie == null) {
                continue;
            }

            final Cookie responseCookie = (Cookie) cookie.clone();
            responseCookie.setMaxAge(0);
            responseCookie.setPath(cookiePath);
            responseCookie.setValue("");

            addCookie(responseCookie, response);
            count++;
        }

        return count;
    }

    /**
     * Remove the Cookies whose names match the provided Regex from Response
     *
     * @param request  Request to get the Cookies to drop
     * @param response Response to expire the Cookies on
     * @param regexes  Regex to find Cookies to drop
     * @return Number of Cookies dropped
     */
    public static int dropCookiesByRegex(final HttpServletRequest request, final HttpServletResponse response, final String cookiePath, final String... regexes) {
        return dropCookiesByRegexArray(request, response, cookiePath, regexes);
    }

    /**
     * Remove the Cookies whose names match the provided Regex from Response
     *
     * @param request  Request to get the Cookies to drop
     * @param response Response to expire the Cookies on
     * @param regexes  Regex to find Cookies to drop
     * @return Number of Cookies dropped
     */
    public static int dropCookiesByRegexArray(final HttpServletRequest request, final HttpServletResponse response, final String cookiePath, final String[] regexes) {
        int count = 0;
        if (regexes == null) {
            return count;
        }
        final List<Cookie> cookies = new ArrayList<Cookie>();

        for (final String regex : regexes) {
            cookies.addAll(getCookies(request, regex));
        }

        return dropCookies(response, cookies.toArray(new Cookie[cookies.size()]), cookiePath);
    }

    /**
     * Removes all cookies for the domain
     *
     * @param request  Request to get the Cookies to drop
     * @param response Response to expire the Cookies on
     */
    public static int dropAllCookies(final HttpServletRequest request, final HttpServletResponse response, final String cookiePath) {
        final Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return 0;
        }

        return dropCookies(response, cookies, cookiePath);
    }
}