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
package com.adobe.acs.commons.util.impl;

import com.adobe.acs.commons.util.impl.ValueMapTypeConverter;
import com.day.cq.commons.inherit.InheritanceValueMap;
import org.apache.sling.api.resource.ValueMap;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Contains reflection utility methods
 */
public class ReflectionUtil {

    private static final Pattern CASTED_VALUE = Pattern.compile("\\{(Long|Integer|String|Boolean|Float|Double)}(.+)");

    private ReflectionUtil() {
        // static methods only
    }

    public static <T> T convertValueMapValue(ValueMap valueMap, String name, Type declaredType) {
        return (T) new ValueMapTypeConverter(valueMap, name, declaredType).getConvertedValue();
    }

    public static <T> T convertValueMapValue(InheritanceValueMap valueMap, String name, Type declaredType) {
        return (T) new ValueMapTypeConverter(valueMap, name, declaredType).getConvertedValue();
    }

    public static <T> T[] toArray(Collection<T> c, T[] a) {
        return c.size() > a.length
                ? c.toArray((T[]) Array.newInstance(a.getClass().getComponentType(), c.size()))
                : c.toArray(a);
    }

    /**
     * The collection CAN be empty
     */
    public static <T> T[] toArray(Collection<T> c, Class klass) {
        return toArray(c, (T[]) Array.newInstance(klass, c.size()));
    }

    /**
     * The collection CANNOT be empty!
     */
    public static <T> T[] toArray(Collection<T> c) {
        return toArray(c, c.iterator().next().getClass());
    }

    public static boolean isArray(Type declaredType) {
        if (declaredType instanceof Class<?>) {
            Class<?> clazz = (Class<?>) declaredType;
            return isArray(clazz);
        }
        return false;
    }

    public static boolean isArray(Class<?> clazz) {
        return clazz.isArray();
    }

    public static boolean isCollectionType(Type declaredType) {
        if (declaredType instanceof Class<?>) {
            Class<?> clazz = (Class<?>) declaredType;
            return isCollectionType(clazz);
        } else {
            ParameterizedType parameterizedType = (ParameterizedType) declaredType;
            return isCollectionType(parameterizedType.getRawType());
        }
    }

    public static boolean isCollectionType(Class<?> collectionType) {
        return collectionType.equals(Collection.class);
    }

    public static boolean isSetType(Type declaredType) {
        if (declaredType instanceof Class<?>) {
            Class<?> clazz = (Class<?>) declaredType;
            return isSetType(clazz);
        } else {
            ParameterizedType parameterizedType = (ParameterizedType) declaredType;
            return isSetType(parameterizedType.getRawType());
        }
    }

    public static boolean isSetType(Class<?> collectionType) {
        return collectionType.equals(Set.class);
    }

    public static boolean isListType(Type declaredType) {
        if (declaredType instanceof Class<?>) {
            Class<?> clazz = (Class<?>) declaredType;
            return isListType(clazz);
        } else {
            ParameterizedType parameterizedType = (ParameterizedType) declaredType;
            return isListType(parameterizedType.getRawType());
        }
    }

    public static boolean isListType(Class<?> collectionType) {
        return collectionType.equals(List.class);
    }

    public static Class<?> getClassOrGenericParam(Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            return getGenericParameter(parameterizedType, 0);
        } else {
            Class<?> clazz = (Class<?>) type;

            if(clazz.isArray()){
                return clazz.getComponentType();
            }else{
                return clazz;
            }
        }
    }

    public static boolean isAssignableFrom(Type assignableFromType, Class<?> clazz) {

        if (assignableFromType == null || clazz == null) {
            return false;
        }
        if (assignableFromType instanceof Class<?>) {
            Class<?> assignAbleFromClazz = (Class<?>) assignableFromType;
            return assignAbleFromClazz.isAssignableFrom(clazz);
        } else {
            ParameterizedType parameterizedType = (ParameterizedType) assignableFromType;
            return ((Class<?>) parameterizedType.getRawType()).isAssignableFrom(clazz);
        }

    }

    public static boolean hasGenericParameter(Type type) {
        return ParameterizedType.class.isInstance(type);
    }

    public static Class<?> getGenericParameter(Type type) {

        return getGenericParameter(type, 0);
    }

    public static Class<?> getGenericParameter(Type type, int index) {

        if (isArray(type)) {
            return ((Class<?>) type).getComponentType();
        }
        if (hasGenericParameter(type)) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            return (Class<?>) parameterizedType.getActualTypeArguments()[index];
        }
        return null;
    }





    public static Object castStringValue(String allowedValue) {
        final Matcher matcher = CASTED_VALUE.matcher(allowedValue);
        boolean match = matcher.matches();

        if(match){

            String type = matcher.group(1);
            String value = matcher.group(2);

            switch (type){
                case "Long":
                    return Long.valueOf(value);
                case "Integer":
                    return Integer.valueOf(value);
                case "Float":
                    return Float.valueOf(value);
                case "Boolean":
                    if(value.equalsIgnoreCase("true")){
                        return Boolean.TRUE;
                    }else{
                        return Boolean.FALSE;
                    }
                case "Double":
                    return Double.valueOf(value);
                case "String":
                default:
                    return allowedValue;
            }

        }else{
            return allowedValue;
        }
    }
}
