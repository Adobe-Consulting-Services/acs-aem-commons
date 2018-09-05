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
package com.adobe.acs.commons.mcp.util;

import java.util.Arrays;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Sanity check for value map serializer methods.
 */
public class ValueMapSerializerTest {

    public ValueMapSerializerTest() {
    }

    /**
     * Test of serializeToStringArray method, of class ValueMapSerializer.
     */
    @Test
    public void testSerializeToStringArray() {
        Object value = Arrays.asList("one", "two", "three", 4, 5.0D, 6L, new Integer(7), new Long(8));
        String[] expResult = new String[]{"one", "two", "three", "4", "5.0", "6", "7", "8"};
        String[] result = ValueMapSerializer.serializeToStringArray(value);
        assertArrayEquals(expResult, result);
    }

    Long lvalue1 = 1234L;
    long lvalue2 = 1234L;
    Integer ivalue1 = 5678;
    int ivalue2 = 5678;

    @Test
    public void testPrimitiveHandling() throws SecurityException, NoSuchFieldException {
        assertTrue("Should recognize unboxed Long primitive",
                IntrospectionUtil.isPrimitive(getClass().getDeclaredField("lvalue2")));
        assertTrue("Should recognize unboxed Integer primitive",
                IntrospectionUtil.isPrimitive(getClass().getDeclaredField("ivalue2")));
        assertTrue("Should recognize boxed Long primitive",
                IntrospectionUtil.isPrimitive(getClass().getDeclaredField("lvalue1")));
        assertTrue("Should recognize boxed Integer primitive",
                IntrospectionUtil.isPrimitive(getClass().getDeclaredField("ivalue1")));
    }
}
