/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
import org.junit.Test;

import java.util.Calendar;

import static org.junit.Assert.assertEquals;

public class InfoWriterTest {
    InfoWriter iw = new InfoWriter();

    static final String LS = System.getProperty("line.separator");

    @Test
    public void testPrint_OnlyMessage() throws Exception {
        String expected = "hello world";
        iw.message(expected);

        assertEquals(expected.concat(LS), iw.toString());
    }

    @Test
    public void testPrint_StringVars() throws Exception {
        String expected = "hello world, from ira";

        iw.message("hello {}, from {}", "world", "ira");

        assertEquals(expected.concat(LS), iw.toString());
    }

    @Test
    public void testPrint_ComplexVars() throws Exception {
        final Calendar cal = Calendar.getInstance();
        cal.set(2015, Calendar.JANUARY, 1, 0, 0, 0);

        String expected = "String: hello "
                + "Integer: 10 "
                + "Long: 20 "
                + "Double: 30.1 "
                + "Boolean: true "
                + "Calendar: " + cal.getTime().toString() + " "
                + "Date: " + cal.getTime().toString();

        iw.message("String: {} "
                        + "Integer: {} "
                        + "Long: {} "
                        + "Double: {} "
                        + "Boolean: {} "
                        + "Calendar: {} "
                        + "Date: {}",
                "hello", 10, 20L, 30.1D, true, cal, cal.getTime());

        assertEquals(expected.concat(LS), iw.toString());
    }

    @Test
    public void testPrint_ArrayVars() throws Exception {
        String expected = "String Array [foo, bar], Integer Array [1, 2, 3]";

        iw.message("String Array {}, Integer Array {}", new String[]{ "foo", "bar" }, new int[]{ 1, 2, 3 });

        assertEquals(expected.concat(LS), iw.toString());
    }

    @Test
    public void testTitle_Message() throws Exception {
        String expected = LS + StringUtils.repeat("-", 80) + LS
                + "The Title".concat(LS)
                + StringUtils.repeat("=", 80);

        iw.title("The Title");

        assertEquals(expected.concat(LS), iw.toString());
    }


    @Test
    public void testTitle_NoMessage() throws Exception {
        String expected = LS + StringUtils.repeat("-", 80);

        iw.title();

        assertEquals(expected.concat(LS), iw.toString());
    }

    @Test
    public void testEnd() throws Exception {
        String expected = StringUtils.repeat("-", 80);

        iw.end();

        assertEquals(expected.concat(LS), iw.toString());
    }

    @Test
    public void testLine() throws Exception {
        String expected = StringUtils.repeat("-", 80);

        iw.line();

        assertEquals(expected.concat(LS), iw.toString());
    }

    @Test
    public void testLine_WithIndent() throws Exception {
        String expected = StringUtils.repeat(" ", 10) + StringUtils.repeat("-", 70);

        iw.line(10);

        assertEquals(expected.concat(LS), iw.toString());
    }

    @Test
    public void testGetInfo() throws Exception {
        final String expected = LS
                +  StringUtils.repeat("-", 80) + LS
                + "Info Title" + LS
                +  StringUtils.repeat("=", 80) + LS
                + "This is line 1" + LS
                + "This is line 2" + LS
                +  StringUtils.repeat("-", 80) + LS;

        iw.title("Info Title");
        iw.message("This is line 1");
        iw.message("This is line 2");
        iw.end();

        assertEquals(expected, iw.toString());
    }
}