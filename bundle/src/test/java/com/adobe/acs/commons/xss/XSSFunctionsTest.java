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
package com.adobe.acs.commons.xss;

import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Random;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.sling.xss.XSSAPI;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Note - these do not test the actual XSS functionality. They only test that
 * the EL functions pass through to XSSAPI correctly.
 */
@SuppressWarnings("checkstyle:abbreviationaswordinname")
@RunWith(MockitoJUnitRunner.class)
public class XSSFunctionsTest {

    @Mock
    private XSSAPI xssAPI;
    @Mock
    private com.adobe.granite.xss.XSSAPI graniteXssAPI;

    @Test
    public void testEncodeForHTML() {
        final String test = new String();
        XSSFunctions.encodeForHTML(xssAPI, test);
        verify(xssAPI, only()).encodeForHTML(test);
    }

    @Test
    public void testEncodeForHTML_granite() {
        final String test = new String();
        XSSFunctions.encodeForHTML(graniteXssAPI, test);
        verify(graniteXssAPI, only()).encodeForHTML(test);
    }

    @Test
    public void testEncodeForHTMLAttr() {
        final String test = new String();
        XSSFunctions.encodeForHTMLAttr(xssAPI, test);
        verify(xssAPI, only()).encodeForHTMLAttr(test);
    }

    @Test
    public void testEncodeForHTMLAttr_granite() {
        final String test = new String();
        XSSFunctions.encodeForHTMLAttr(graniteXssAPI, test);
        verify(graniteXssAPI, only()).encodeForHTMLAttr(test);
    }

    @Test
    public void testEncodeForJSString() {
        final String test = new String();
        XSSFunctions.encodeForJSString(xssAPI, test);
        verify(xssAPI, only()).encodeForJSString(test);
    }

    @Test
    public void testEncodeForJSString_granite() {
        final String test = new String();
        XSSFunctions.encodeForJSString(graniteXssAPI, test);
        verify(graniteXssAPI, only()).encodeForJSString(test);
    }

    @Test
    public void testFilterHTML() {
        final String test = new String();
        XSSFunctions.filterHTML(xssAPI, test);
        verify(xssAPI, only()).filterHTML(test);
    }

    @Test
    public void testFilterHTML_granite() {
        final String test = new String();
        XSSFunctions.filterHTML(graniteXssAPI, test);
        verify(graniteXssAPI, only()).filterHTML(test);
    }

    @Test
    public void testGetValidHref() {
        final String test = "/content/foo.html";
        when(xssAPI.getValidHref(test)).thenReturn(test);
        XSSFunctions.getValidHref(xssAPI, test);
        verify(xssAPI, only()).getValidHref(test);
    }

    @Test
    public void testGetValidHref_granite() {
        final String test = "/content/foo.html";
        when(graniteXssAPI.getValidHref(test)).thenReturn(test);
        XSSFunctions.getValidHref(graniteXssAPI, test);
        verify(graniteXssAPI, only()).getValidHref(test);
    }

    @Test
    public void testGetValidDimension() {
        final String dimension = RandomStringUtils.randomAlphanumeric(10);
        final String defaultValue = RandomStringUtils.randomAlphanumeric(10);
        XSSFunctions.getValidDimension(xssAPI, dimension, defaultValue);
        verify(xssAPI, only()).getValidDimension(dimension, defaultValue);
    }

    @Test
    public void testGetValidDimension_granite() {
        final String dimension = RandomStringUtils.randomAlphanumeric(10);
        final String defaultValue = RandomStringUtils.randomAlphanumeric(10);
        XSSFunctions.getValidDimension(graniteXssAPI, dimension, defaultValue);
        verify(graniteXssAPI, only()).getValidDimension(dimension, defaultValue);
    }

    @Test
    public void testGetValidInteger() {
        final String integer = RandomStringUtils.randomAlphanumeric(10);
        final int defaultValue = new Random().nextInt();
        XSSFunctions.getValidInteger(xssAPI, integer, defaultValue);
        verify(xssAPI, only()).getValidInteger(integer, defaultValue);

    }

    @Test
    public void testGetValidInteger_granite() {
        final String integer = RandomStringUtils.randomAlphanumeric(10);
        final int defaultValue = new Random().nextInt();
        XSSFunctions.getValidInteger(graniteXssAPI, integer, defaultValue);
        verify(graniteXssAPI, only()).getValidInteger(integer, defaultValue);

    }

    @Test
    public void testGetValidJSToken() {
        final String token = RandomStringUtils.randomAlphanumeric(10);
        final String defaultValue = RandomStringUtils.randomAlphanumeric(10);
        XSSFunctions.getValidJSToken(xssAPI, token, defaultValue);
        verify(xssAPI, only()).getValidJSToken(token, defaultValue);
    }

    @Test
    public void testGetValidJSToken_granite() {
        final String token = RandomStringUtils.randomAlphanumeric(10);
        final String defaultValue = RandomStringUtils.randomAlphanumeric(10);
        XSSFunctions.getValidJSToken(graniteXssAPI, token, defaultValue);
        verify(graniteXssAPI, only()).getValidJSToken(token, defaultValue);
    }

}
