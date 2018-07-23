package com.adobe.acs.commons.adobeio.core.util;

import com.drew.lang.annotations.NotNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.Cookie;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * This class contains methods to retrive and store cookies
 */
public final class CookieUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(CookieUtil.class);

    private static final String COOKIE_PATH = "/";
    private static final int MAX_AGE = 60 * 60 * 8;  //TODO Use configuration here?
    private static final int ZERO_MAX_AGE = 0;

    private CookieUtil() {
    }

    /**
     * Extract cookie value by name from request.
     *
     * @param request    sling http request
     * @param cookieName cookie name
     * @return cookie value
     */
    public static String extractCookieValue(@NotNull String cookieName, @NotNull SlingHttpServletRequest request) {
        String cookieValue = StringUtils.EMPTY;
        if (request == null) {
            return "";
        }
        Cookie cookie = request.getCookie(cookieName);

        if (cookie != null && StringUtils.isNotBlank(cookie.getValue())) {
            cookieValue = StringUtils.isNotBlank(cookie.getValue()) ? cookie.getValue() : StringUtils.EMPTY;
            // Decrypt if encrypted cookie
            try {
                cookieValue = URLDecoder.decode(cookieValue, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                LOGGER.error(ExceptionUtils.getStackTrace(e));
            }

        }

        return cookieValue;
    }

    public static void storeCookie(String cookieName, String value, SlingHttpServletResponse response) {
        response.addCookie(createCookie(cookieName, value, MAX_AGE, COOKIE_PATH));

    }

    public static boolean hasCookie(String cookieName, SlingHttpServletRequest request) {
        return request.getCookie(cookieName) != null;
    }

    /**
     * Remove cookie from response.
     *
     * @param name     cookie name
     * @param response response
     */
    public static void removeCookie(String name, SlingHttpServletResponse response) {
        Cookie removeCookie = createCookie(name, null, ZERO_MAX_AGE, COOKIE_PATH);
        response.addCookie(removeCookie);

    }

    private static Cookie createCookie(String name, String value, int maxAge, String path) {
        Cookie cookie = new Cookie(name, value);
        cookie.setMaxAge(maxAge);
        cookie.setPath(path);
        return cookie;
    }
}
