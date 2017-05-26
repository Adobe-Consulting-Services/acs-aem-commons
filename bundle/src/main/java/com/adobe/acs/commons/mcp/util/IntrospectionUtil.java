/*
 * Copyright 2017 Adobe.
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
package com.adobe.acs.commons.mcp.util;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

/**
 * General introspection utilities
 */
public class IntrospectionUtil {
    public static boolean isListOrArray(Field field) {
        return field.getType().isArray() || Collection.class.isAssignableFrom(field.getType());
    }
    
    public static Class<?> getCollectionComponentType(Field field) {
        if (Collection.class.isAssignableFrom(field.getType())) {
            Type genericType = field.getGenericType();
            if (genericType instanceof ParameterizedType) {
                ParameterizedType t = (ParameterizedType) genericType;
                if (t.getActualTypeArguments().length == 1) {
                    return (Class) t.getActualTypeArguments()[0];
                } else {
                    return null;
                }
            } else {
                return Object.class;
            }
        } else if (field.getType().isArray()) {
            return field.getType().getComponentType();
        } else {
            return field.getType();
        }
    }
    
    public static boolean isPrimitive(Field field) {
        Class basicType = getCollectionComponentType(field);
        return basicType.isPrimitive() || basicType.getPackage().toString().startsWith("java.lang");
    }
    
    private IntrospectionUtil() {
        // Utility class, not to be instantiated directly
    }
}
