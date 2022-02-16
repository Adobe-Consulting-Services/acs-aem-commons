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

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class CookieUtilTest {

    private static HttpServletRequest request;
    private static HttpServletResponse response;

    private static Cookie dogCookie;
    private static Cookie catCookie;
    private static Cookie frogCookie;
    private static Cookie tortoiseCookie;

    private static Cookie[] cookies;

    @Before
    public void setUp() {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);

        dogCookie = new Cookie("dog-mammal", "woof");
        catCookie = new Cookie("cat-mammal", "meow");
        frogCookie = new Cookie("frog-amphibian", "ribbit");
        tortoiseCookie = new Cookie("tortoise", "...?");
        tortoiseCookie.setMaxAge(100);

        cookies = new Cookie[]{frogCookie, catCookie, tortoiseCookie, dogCookie};

        when(request.getCookies()).thenReturn(cookies);

    }

    /**
     * Test of addCookie method, of class CookieUtil.
     */
    @Test
    public void testAddCookie() {
        boolean expResult = false;
        boolean result = CookieUtil.addCookie(null, response);
        assertEquals(expResult, result);

        expResult = true;
        result = CookieUtil.addCookie(dogCookie, response);
        assertEquals(expResult, result);
    }

    /**
     * Test of getCookie method, of class CookieUtil.
     */
    @Test
    public void testGetCookie() {
        String cookieName = "snake";
        Cookie expResult = null;
        Cookie result = CookieUtil.getCookie(request, cookieName);
        assertEquals(expResult, result);

        cookieName = "dog-mammal";
        expResult = dogCookie;
        result = CookieUtil.getCookie(request, cookieName);
        assertEquals(expResult, result);
    }

    /**
     * Test of getCookies method, of class CookieUtil.
     */
    @Test
    public void testGetCookies() {
        String regex = "(.*)mammal(.*)";
        List<Cookie> expResult = new ArrayList<Cookie>();
        expResult.add(catCookie);
        expResult.add(dogCookie);

        List<Cookie> result = CookieUtil.getCookies(request, regex);
        assertEquals(expResult, result);
    }

    /**
     * Test of extendCookieLife method, of class CookieUtil.
     */
    @Test
    public void testExtendCookieLife() {
        String cookieName = "tortoise";
        int expiry = 1000;
        boolean expResult = true;
        boolean result = CookieUtil.extendCookieLife(request, response, cookieName, "/", expiry);
        assertEquals(expResult, result);

        cookieName = "dodo";
        expResult = false;
        result = CookieUtil.extendCookieLife(request, response, cookieName, "/", expiry);
        assertEquals(expResult, result);
    }

    /**
     * Test of dropCookies method, of class CookieUtil.
     */
    @Test
    public void testDropCookies() {
        String[] cookieNames = {"dog-mammal", "cat-mammal"};
        CookieUtil.dropCookies(request, response, "/", cookieNames);
        assertTrue(true);

        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);

        verify(response, times(2)).addCookie(cookieCaptor.capture());

        for (Cookie cookie : cookieCaptor.getAllValues()) {
            assertEquals(0, cookie.getMaxAge());
            assertEquals("", cookie.getValue());
        }
    }

    /**
     * Test of dropCookiesByRegex method, of class CookieUtil.
     */
    @Test
    public void testDropCookiesByRegex() {
        int expResult = 3;
        int result = CookieUtil.dropCookiesByRegex(request, response, "/", "(.*)mammal(.*)", "^fr(.*)");
        assertEquals(expResult, result);

        expResult = 0;
        result = CookieUtil.dropCookiesByRegex(request, response, "/", "nothere");
        assertEquals(expResult, result);
    }

    /**
     * Test of dropCookiesByRegexArray method, of class CookieUtil.
     */
    @Test
    public void testDropCookiesByRegexArray() {
        String[] regexes = new String[]{"(.*)mammal(.*)", "^fr.*"};

        int expResult = 3;
        int result = CookieUtil.dropCookiesByRegex(request, response, "/", regexes);
        assertEquals(expResult, result);

        expResult = 0;
        result = CookieUtil.dropCookiesByRegex(request, response, "/", "nothere");
        assertEquals(expResult, result);
    }

    /**
     * Test of dropAllCookies method, of class CookieUtil.
     */
    @Test
    public void testDropAllCookies() {
        int expResult = cookies.length;
        int result = CookieUtil.dropAllCookies(request, response, "/");
        assertEquals(expResult, result);
    }
}
