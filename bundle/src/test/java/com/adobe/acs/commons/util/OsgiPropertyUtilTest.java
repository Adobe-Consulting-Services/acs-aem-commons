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

import org.junit.*;

import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class OsgiPropertyUtilTest {

    public OsgiPropertyUtilTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of toSimpleEntry method, of class OsgiPropertyUtil.
     */
    @Test
    public void testToSimpleEntry() {
        String value = "key:value";
        String separator = ":";
        SimpleEntry expResult = new SimpleEntry("key", "value");
        SimpleEntry result = OsgiPropertyUtil.toSimpleEntry(value, separator);
        assertEquals(expResult, result);
    }

    @Test
    public void testToSimpleEntryWithOnlyKey1() {
        String value = "key:";
        String separator = ":";
        SimpleEntry expResult = null;
        SimpleEntry result = OsgiPropertyUtil.toSimpleEntry(value, separator);
        assertEquals(expResult, result);
    }

    @Test
    public void testToSimpleEntryWithOnlyKey2() {
        String value = "key";
        String separator = ":";
        SimpleEntry expResult = null;
        SimpleEntry result = OsgiPropertyUtil.toSimpleEntry(value, separator);
        assertEquals(expResult, result);
    }

    @Test
    public void testToSimpleEntryWithOnlyValue() {
        String value = ":value";
        String separator = ":";
        SimpleEntry expResult = null;
        SimpleEntry result = OsgiPropertyUtil.toSimpleEntry(value, separator);
        assertEquals(expResult, result);
    }

    @Test
    public void testToSimpleEntryWithMultipleSeparators() {
        String value = "key:val:ue";
        String separator = ":";
        SimpleEntry expResult = null;
        SimpleEntry result = OsgiPropertyUtil.toSimpleEntry(value, separator);
        assertEquals(expResult, result);
    }

    @Test
    public void testToSimpleEntryWithMismatchSeparators() {
        String value = "key:value";
        String separator = "-";
        SimpleEntry expResult = null;
        SimpleEntry result = OsgiPropertyUtil.toSimpleEntry(value, separator);
        assertEquals(expResult, result);
    }

    /**
     * Test of toMap method, of class OsgiPropertyUtil.
     */
    @Test
    public void testToMap() {
        String[] values = {"key1:value1", "key2:value2", "key3:value3"};
        String separator = ":";
        Map expResult = new HashMap<String, String>();
        expResult.put("key1", "value1");
        expResult.put("key2", "value2");
        expResult.put("key3", "value3");

        Map result = OsgiPropertyUtil.toMap(values, separator);
        assertEquals(expResult, result);
    }

    @Test
    public void testToMapWithMultipleSeparators() {
        String[] values = {"key1:value1", "key2:val:ue2", "key3:value3"};
        String separator = ":";
        Map expResult = new HashMap<String, String>();
        expResult.put("key1", "value1");
        expResult.put("key3", "value3");

        Map result = OsgiPropertyUtil.toMap(values, separator);
        assertEquals(expResult, result);
    }

    @Test
    public void testToMapWithOnlyKey1() {
        String[] values = {"key1:value1", "key2:", "key3:value3"};
        String separator = ":";
        Map expResult = new HashMap<String, String>();
        expResult.put("key1", "value1");
        expResult.put("key3", "value3");

        Map result = OsgiPropertyUtil.toMap(values, separator);
        assertEquals(expResult, result);
    }

    @Test
    public void testToMapWithOnlyKey2() {
        String[] values = {"key1:value1", "key2:", "key3:value3"};
        String separator = ":";
        Map expResult = new HashMap<String, String>();
        expResult.put("key1", "value1");
        expResult.put("key3", "value3");

        Map result = OsgiPropertyUtil.toMap(values, separator);
        assertEquals(expResult, result);
    }

    @Test
    public void testToMapWithOnlyValue() {
        String[] values = {"key1:value1", ":value2", "key3:value3"};
        String separator = ":";
        Map expResult = new HashMap<String, String>();
        expResult.put("key1", "value1");
        expResult.put("key3", "value3");

        Map result = OsgiPropertyUtil.toMap(values, separator);
        assertEquals(expResult, result);
    }

    @Test
    public void testToMapWithMismatchSeparator() {
        String[] values = {"key1:value1", "key2-value2", "key3:value3"};
        String separator = ":";
        Map expResult = new HashMap<String, String>();
        expResult.put("key1", "value1");
        expResult.put("key3", "value3");

        Map result = OsgiPropertyUtil.toMap(values, separator);
        assertEquals(expResult, result);
    }
}
