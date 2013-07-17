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
