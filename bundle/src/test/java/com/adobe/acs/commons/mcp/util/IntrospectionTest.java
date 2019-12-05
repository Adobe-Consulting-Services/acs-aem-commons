/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2019 Adobe
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

import java.util.Optional;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Verify the introspection utility classes not tested elsewhere
 */
public class IntrospectionTest {

    @Test
    public void testDefaults() {
        assertEquals("Should read default string value", Optional.of("defaultValue"), IntrospectionUtil.getDeclaredValue(FieldUtils.getDeclaredField(SimpleTest.class, "testString", true)));
        assertEquals("Should read default boolean value", Optional.of(Boolean.TRUE), IntrospectionUtil.getDeclaredValue(FieldUtils.getDeclaredField(SimpleTest.class, "testBoolean", true)));
    }

    @Test
    public void testNoDefaults() {
        assertEquals("Should return empty for no default value", Optional.empty(), IntrospectionUtil.getDeclaredValue(FieldUtils.getDeclaredField(SimpleTest.class, "testNull", true)));
    }

    @Test
    public void testParameterizedConstructor() {
        assertEquals("Should read default string value", Optional.of("Test"), IntrospectionUtil.getDeclaredValue(FieldUtils.getDeclaredField(ComplexConstructorTest.class, "val1", true)));
        assertEquals("Should read default boolean value", Optional.of(Boolean.TRUE), IntrospectionUtil.getDeclaredValue(FieldUtils.getDeclaredField(ComplexConstructorTest.class, "val2", true)));
    }

    @Test
    public void testFailingConstructor() {
        assertEquals("Should read no default string value", Optional.empty(), IntrospectionUtil.getDeclaredValue(FieldUtils.getDeclaredField(ErrorConstructorTest.class, "val1", true)));
        assertEquals("Should read no default boolean value", Optional.empty(), IntrospectionUtil.getDeclaredValue(FieldUtils.getDeclaredField(ErrorConstructorTest.class, "val2", true)));
    }

    public static class SimpleTest {
        private String testString = "defaultValue";
        private boolean testBoolean = true;
        private String testNull;
    }

    public static class ComplexConstructorTest {
        private String val1;
        private Boolean val2;

        public ComplexConstructorTest(Object param1, Object param2) {
            val1 = "Test";
            val2 = true;
        }
    }

    public static class ErrorConstructorTest {
        private String val1 = "Test";
        private Boolean val2 = true;

        public ErrorConstructorTest(Object param1, Object param2) {
            throw new NullPointerException("I didn't guard against null inputs");
        }
    }
}
