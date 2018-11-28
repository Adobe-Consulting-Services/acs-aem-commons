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

import com.google.gson.JsonObject;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TypeUtilTest {

    public TypeUtilTest() {
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
     * Test of ArrayToMap method, of class TypeUtil.
     */
    @Test
    public void testArrayToMap() {
        String[] list = new String[]{"key1", "value1", "key2", "value2", "key3", "value3"};
        Map<String, String> expResult = new HashMap<String, String>();
        expResult.put("key1", "value1");
        expResult.put("key2", "value2");
        expResult.put("key3", "value3");

        Map<String, String> result = TypeUtil.arrayToMap(list);
        assertEquals(expResult, result);
    }

    @Test
    public void testArrayToMap_oddNummberArray() {
        String[] list = new String[]{"key1", "value1", "key2", "value2", "value3"};

        // Expect and exception to be thrown
        try {
            TypeUtil.arrayToMap(list);
            assertTrue(false);
        } catch (IllegalArgumentException ex) {
            assertTrue(true);
        }
    }

    @Test
    public void testToMap() {
        final JsonObject json = new JsonObject();
        json.addProperty("one", "uno");
        json.addProperty("two", 2);
        json.addProperty("three", new Long(3));

        // TODO: Find a way to coerce GSON to not treat all numbers as Double, but that's what it does, unavoidably.
        final Map<String, Object> expResult = new HashMap<String, Object>();
        expResult.put("one", "uno");
        expResult.put("two", 2.0);
        expResult.put("three", 3.0);

        final Map<String, Object> actual = TypeUtil.toMap(json);

        assertEquals(expResult, actual);
    }

    @Test
    public void testToValueMap() {
        final Map<String, String> stringMap = new LinkedHashMap<String, String>();
        stringMap.put("one", "uno");
        stringMap.put("two", "dos");
        stringMap.put("three", "tres");

        final Map<String, Object> objectMap = new LinkedHashMap<String, Object>();
        objectMap.put("one", "uno");
        objectMap.put("two", "dos");
        objectMap.put("three", "tres");

        final ValueMap expResult = new ValueMapDecorator(objectMap);
        final ValueMap actual = TypeUtil.toValueMap(stringMap);

        assertEquals(expResult.size(), actual.size());
        assertEquals(expResult.get("one", "expected"), actual.get("one", "actual"));
        assertEquals(expResult.get("two", "expected"), actual.get("two", "actual"));
        assertEquals(expResult.get("three", "expected"), actual.get("three", "actual"));
    }


    @Test
    public void testGetType_Double() {
        assertEquals(TypeUtil.getType(new Double(10000.00001)), Double.class);
    }

    @Test
    public void testGetType_Float() {
        assertEquals(Double.class, TypeUtil.getType(new Float(100.001)));
    }

    @Test
    public void testGetType_Long() {
        assertEquals(Long.class, TypeUtil.getType(new Long(100000000)));
    }

    @Test
    public void testGetType_Boolean() {
        assertEquals(Boolean.class, TypeUtil.getType(Boolean.TRUE));
    }

    @Test
    public void testGetType_Date() {
        assertEquals(Date.class, TypeUtil.getType("1997-07-16T19:20:30.450+01:00"));
    }

    @Test
    public void testGetType_String() {
        assertEquals(String.class, TypeUtil.getType("Hello World!"));
    }

    @Test
    public void toObjectType_Double() {
        assertEquals(new Double(10.01), TypeUtil.toObjectType("10.01", Double.class));
    }

    @Test
    public void toObjectType_Long() {
        assertEquals(new Long(10), TypeUtil.toObjectType("10", Long.class));
    }

    @Test
    public void toObjectType_BooleanTrue() {
        assertEquals(Boolean.TRUE, TypeUtil.toObjectType("true", Boolean.class));
    }

    @Test
    public void toObjectType_BooleanFalse() {
        assertEquals(Boolean.FALSE, TypeUtil.toObjectType("false", Boolean.class));
    }

    @Test
    public void toObjectType_Date() {
        Date expResult = new Date();
        expResult.setTime(1000); // 1 second after the epoch
        assertEquals(expResult, TypeUtil.toObjectType("1970-01-01T00:00:01.000+00:00", Date.class));
    }

    @Test
    public void toObjectType_String() {
        String expResult = "Hello World";
        assertEquals(expResult, TypeUtil.toObjectType(expResult, String.class));
    }

    @Test
    public void toString_Double() {
        String expResult = "1000.0";
        Double doubleValue = new Double(1000);
        try {
            assertEquals(expResult, TypeUtil.toString(doubleValue, Double.class));
        } catch (IllegalAccessException e) {
            assertTrue(false);
        } catch (NoSuchMethodException e) {
            assertTrue(false);
        } catch (InvocationTargetException e) {
            assertTrue(false);
        }
    }

    @Test
    public void toString_Custom() {
        String expResult = "Hello World!";
        CustomToString custom = new CustomToString("Hello World");
        try {
            assertEquals(expResult, TypeUtil.toString(custom, CustomToString.class, "giveMeAString"));
        } catch (InvocationTargetException e) {
            assertTrue(false);
        } catch (IllegalAccessException e) {
            assertTrue(false);
        } catch (NoSuchMethodException e) {
            assertTrue(false);
        }
    }

    /* Custom method for testing */
    private class CustomToString {
        private String val;

        public CustomToString(String val) {
            this.val = val;
        }

        @SuppressWarnings("unused")
        public String giveMeAString() {
            return this.val + "!";
        }
    }
}