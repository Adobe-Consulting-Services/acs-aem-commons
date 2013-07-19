package com.adobe.acs.commons.util;

import org.junit.*;

import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.HashMap;
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
        Map expResult = new HashMap<String, String>();
        expResult.put("key1", "value1");
        expResult.put("key2", "value2");
        expResult.put("key3", "value3");

        Map result = TypeUtil.ArrayToMap(list);
        assertEquals(expResult, result);
    }

    @Test
    public void testArrayToMap_oddNummberArray() {
        String[] list = new String[]{"key1", "value1", "key2", "value2", "value3"};

        // Expect and exception to be thrown
        try {
            TypeUtil.ArrayToMap(list);
            assertTrue(false);
        } catch (IllegalArgumentException ex) {
            assertTrue(true);
        }
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
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    /* Custom method for testing */
    private class CustomToString {
        private String val;

        public CustomToString(String val) {
            this.val = val;
        }

        public String giveMeAString() {
            return this.val + "!";
        }
    }
}
