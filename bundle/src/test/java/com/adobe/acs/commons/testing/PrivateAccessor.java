/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.testing;

import java.lang.reflect.Field;

/**
 * Utility class providing reflective access to private fields, replacing the dependency on
 * {@code junitx.util.PrivateAccessor} from the {@code junit-addons} library.
 */
public final class PrivateAccessor {

    private PrivateAccessor() {
    }

    /**
     * Sets the value of a (possibly private) field on an object, traversing the class hierarchy if needed.
     *
     * @param obj       the object whose field should be set
     * @param fieldName the name of the field
     * @param value     the value to set
     * @throws NoSuchFieldException   if no field with the given name is found in the class hierarchy
     * @throws IllegalAccessException if field access is not permitted
     */
    public static void setField(Object obj, String fieldName, Object value)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = findField(obj.getClass(), fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }

    /**
     * Gets the value of a (possibly private) field from an object, traversing the class hierarchy if needed.
     *
     * @param obj       the object from which the field value should be read
     * @param fieldName the name of the field
     * @return the field value
     * @throws NoSuchFieldException   if no field with the given name is found in the class hierarchy
     * @throws IllegalAccessException if field access is not permitted
     */
    public static Object getField(Object obj, String fieldName)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = findField(obj.getClass(), fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }

    private static Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(
                "Field '" + fieldName + "' not found in class hierarchy of " + clazz.getName());
    }
}
