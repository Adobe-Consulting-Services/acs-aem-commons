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

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

/**
 * General introspection utilities
 */
public class IntrospectionUtil {
    /**
     * Figure out if field represents multiple values
     * @param clazz Field to evaluate
     * @return true if field is an array or collection
     */
    public static boolean hasMultipleValues(Class clazz) {
        return clazz.isArray() || Collection.class.isAssignableFrom(clazz);
    }
    
    /**
     * Determine if the field is a list or array and return its component type if so.
     * @param field Field to evaluate
     * @return List/Array component type or field type if not a list or array
     */
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

    /**
     * A primitive field is one which is a single or array/list of primitive values.
     * @param field Field to evaluate
     * @return true if primitive or list/array of primitive values
     */
    public static boolean isPrimitive(Field field) {
        Class basicType = getCollectionComponentType(field);
        return basicType != null && (basicType.isPrimitive() || Number.class.isAssignableFrom(basicType) || basicType == String.class);
    }
    
    /**
     * A simple field is either primitive, an enumeration, or a string.
     * @param field Field to evaluate
     * @return true if primitive, an enumeration, or a string
     */
    public static boolean isSimple(Field field) {
        Class basicType = getCollectionComponentType(field);
        return basicType != null && (isPrimitive(field) || basicType.isEnum() || basicType == String.class);
    }
    
    private IntrospectionUtil() {
        // Utility class, not to be instantiated directly
    }
}
