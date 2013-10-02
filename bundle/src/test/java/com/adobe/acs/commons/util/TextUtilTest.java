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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TextUtilTest {

    public TextUtilTest() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }


    @Test
    public void testGetFirstNonNull_String() {
        String first = null;
        String second = null;
        String third = "third";
        String fourth = null;

        String expResult = "third";
        String result = TextUtil.getFirstNonNull(first, second, third, fourth);
        assertEquals(expResult, result);
    }

    @Test
    public void testGetFirstNonNull_int() {
        int first = 1;
        int second = 2;
        int third = 3;
        int fourth = 4;

        int expResult = 1;
        int result = TextUtil.getFirstNonNull(first, second, third, fourth);
        assertEquals(expResult, result);
    }


    @Test
    public void testGetFirstNonEmpty() {
        String first = null;
        String second = "";
        String third = "third";
        String fourth = "fourth";

        String expResult = "third";
        String result = TextUtil.getFirstNonEmpty(first, second, third, fourth);
        assertEquals(expResult, result);
    }

    @Test
    public void testisRichText_PlainText() {
        boolean expResult = false;
        boolean result = TextUtil.isRichText("This is is not rich text");
        assertEquals(expResult, result);
    }

    @Test
    public void testisRichText_OpenCloseTag() {
        boolean expResult = true;
        boolean result = TextUtil.isRichText("<strong>This is strong text</strong>");
        assertEquals(expResult, result);
    }
}
