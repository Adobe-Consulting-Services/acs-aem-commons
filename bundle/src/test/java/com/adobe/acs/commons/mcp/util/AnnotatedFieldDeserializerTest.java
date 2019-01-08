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

import com.adobe.acs.commons.mcp.form.FormField;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.sling.api.wrappers.ModifiableValueMapDecorator;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class AnnotatedFieldDeserializerTest {

    private NumberFormat numberFormat;

    @Before
    public void setup() {
        numberFormat = NumberFormat.getNumberInstance();
    }

    public static class PrimitivesTest {

        @FormField(name = "int")
        int intValue;
        @FormField(name = "double")
        double doubleValue;
        @FormField(name = "float")
        float floatValue;
        @FormField(name = "long")
        long longValue;
        @FormField(name = "boolean")
        boolean booleanValue;
    }

    /**
     * Check if primitives are deserialized properly.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testPrimitiveInputs() throws Exception {

        final PrimitivesTest target = new PrimitivesTest();
        Map<String, Object> params = new HashMap<>();
        params.put("intValue", "123");
        params.put("doubleValue", numberFormat.format(123.456));
        params.put("floatValue",  numberFormat.format(234.567f));
        params.put("longValue", "1234567890");
        params.put("booleanValue", "true");
        AnnotatedFieldDeserializer.deserializeFormFields(target, new ModifiableValueMapDecorator(params));
        assertEquals(123, target.intValue);
        assertEquals(123.456D, target.doubleValue, 0);
        assertEquals(234.567F, target.floatValue, 0);
        assertEquals(1234567890L, target.longValue);
        assertEquals(true, target.booleanValue);
    }

    public static class PrimitiveArrayTest {

        @FormField(name = "int")
        int[] intValue;
        @FormField(name = "double")
        double[] doubleValue;
        @FormField(name = "float")
        List<Float> floatValue;
    }

    /**
     * Check if primitives are deserialized properly.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testPrimitiveArrayInputs() throws Exception {
        final PrimitiveArrayTest target = new PrimitiveArrayTest();
        Map<String, Object> params = new HashMap<>();
        params.put("intValue", new String[]{"123", "456", "789"});
        params.put("doubleValue",  numberFormat.format(123.456));
        params.put("floatValue", new String[]{
                numberFormat.format(234.567f),
                numberFormat.format(111.222f),
                numberFormat.format(333.444f),
                numberFormat.format(555.666f)});
        AnnotatedFieldDeserializer.deserializeFormFields(target, new ModifiableValueMapDecorator(params));
        assertArrayEquals(new int[]{123, 456, 789}, target.intValue);
        assertArrayEquals(new double[]{123.456D}, target.doubleValue, 0);
        assertNotNull(target.floatValue);
        assertEquals(4, target.floatValue.size());
        assertEquals(234.567F, target.floatValue.get(0), 0);
        assertEquals(111.222F, target.floatValue.get(1), 0);
        assertEquals(333.444F, target.floatValue.get(2), 0);
        assertEquals(555.666F, target.floatValue.get(3), 0);
    }

    /**
     * Check if booleans are assumed false if missing.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testBooleanFalseByDefault() throws Exception {
        final PrimitivesTest target = new PrimitivesTest();
        Map<String, Object> params = new HashMap<>();
        params.put("intValue", "123");
        params.put("doubleValue", numberFormat.format(123.456));
        params.put("floatValue",  numberFormat.format(234.567f));
        params.put("longValue", "1234567890");
        target.booleanValue = true;
        AnnotatedFieldDeserializer.deserializeFormFields(target, new ModifiableValueMapDecorator(params));
        assertEquals(123, target.intValue);
        assertEquals(123.456D, target.doubleValue, 0);
        assertEquals(234.567F, target.floatValue, 0);
        assertEquals(1234567890L, target.longValue);
        assertEquals(false, target.booleanValue);
    }

    
}
