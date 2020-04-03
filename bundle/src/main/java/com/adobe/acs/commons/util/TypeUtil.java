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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.osgi.annotation.versioning.ProviderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

@ProviderType
public final class TypeUtil {
    private static final Logger log = LoggerFactory.getLogger(TypeUtil.class);

    private static final Pattern JSON_DATE =
            Pattern.compile("^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]{3}[-+]{1}[0-9]{2}[:]{0,1}[0-9]{2}$");

    private TypeUtil() {
    }

    /**
     * Turn a even length Array into a Map. The Array is expected to be in the
     * format: { key1, value1, key2, value2, ... , keyN, valueN }
     *
     * @param <T>
     * @param list
     * @return
     */
    public static <T> Map<T, T> arrayToMap(T[] list) {
        final HashMap<T, T> map = new HashMap<T, T>();
        if (list == null) {
            return map;
        }
        if (list.length > 0 && (list.length % 2) == 1) {
            throw new IllegalArgumentException(
                    "Array must be even in length, representing a series of Key, Value pairs.");
        }

        for (int i = 0; i < list.length; i = i + 2) {
            map.put(list[i], list[i + 1]);
        }

        return map;
    }

    /**
     * Convenience wrapper for toMap(jsonObj, Object.class).
     *
     * @param json
     * @return
     */
    public static Map<String, Object> toMap(JsonObject json) {
        Gson gson = new Gson();
        return gson.fromJson(json, Map.class);
    }

    /**
     * Determines the type of the parameter object.
     * <p/>
     * TODO - review this method
     *
     * @param object
     * @param <T>
     * @return
     */
    @SuppressWarnings({ "unchecked", "PMD.CollapsibleIfStatements" })
    public static <T> Class<T> getType(final Object object) {
        if (object instanceof Double || object instanceof Float) {
            return (Class<T>) Double.class;
        } else if (object instanceof Number) {
            return (Class<T>) Long.class;
        } else if (object instanceof Boolean) {
            return (Class<T>) Boolean.class;
        } else if (object instanceof String) {
            if (JSON_DATE.matcher((String) object).matches()) {
                return (Class<T>) Date.class;
            }
        } else if(object instanceof Calendar) {
            return (Class<T>) Calendar.class;
        } else if(object instanceof Date) {
            return (Class<T>) Date.class;
        }

        return (Class<T>) String.class;
    }

    /**
     * Converts a limited set of String representations to their corresponding Objects
     * <p/>
     * Supports
     * * Double
     * * Long
     * * Integer
     * * Boolean (true/false)
     * * Dates in string format of ISODateTimeFormat
     * <p/>
     * Else, null is returned.
     *
     * @param data  the String representation of the data
     * @param klass the target class type of the provided data
     * @param <T>   the target class type of the provided data
     * @return the derived object representing the data as specified by the klass
     */
    public static <T> T toObjectType(String data, Class<T> klass) {
        if (Double.class.equals(klass)) {
            try {
                return klass.cast(Double.parseDouble(data));
            } catch (NumberFormatException ex) {
                return null;
            }
        } else if (Long.class.equals(klass)) {
            try {
                return klass.cast(Long.parseLong(data));
            } catch (NumberFormatException ex) {
                return null;
            }
        } else if (Integer.class.equals(klass)) {
            try {
                return klass.cast(Long.parseLong(data));
            } catch (NumberFormatException ex) {
                return null;
            }
        } else if (StringUtils.equalsIgnoreCase("true", data)) {
            return klass.cast(Boolean.TRUE);
        } else if (StringUtils.equalsIgnoreCase("false", data)) {
            return klass.cast(Boolean.FALSE);
        } else if (JSON_DATE.matcher(data).matches()) {
            long epochSeconds = OffsetDateTime.parse(data).toInstant().toEpochMilli();
            return klass.cast(new Date(epochSeconds));
        } else {
            return klass.cast(data);
        }
    }

    /**
     * Gets the default string representation of the parameter object.
     *
     * @param obj
     * @param klass
     * @return
     */
    public static String toString(final Object obj, final Class<?> klass)
            throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        return toString(obj, klass, null);
    }

    /**
     * Gets a custom string representation based on the parameter (0 argument) methodName.
     *
     * @param obj
     * @param klass
     * @param methodName
     * @return
     */
    public static String toString(final Object obj, final Class<?> klass, String methodName)
            throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        if (StringUtils.isBlank(methodName)) {
            methodName = "toString";
        }

        boolean isPrimitiveOrWrapped =
                obj.getClass().isPrimitive() || ClassUtils.wrapperToPrimitive(obj.getClass()) != null;

        if (isPrimitiveOrWrapped) {
            return String.valueOf(obj);
        } else if (Date.class.equals(klass)) {
            return ((Date) obj).toString();
        } else if (Calendar.class.equals(klass)) {
            return ((Calendar) obj).getTime().toString();
        } else if(isArray(obj)) {
            return toStringFromArray(obj);
        } else {
            Method method = klass.getMethod(methodName);
            return (String) method.invoke(obj);
        }
    }



    /**
     * Attempt to create a string representation of an object.
     *
     * @param obj the object to represent as a string
     * @return the string representation of the object
     */
    public static String toString(final Object obj)
            throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {

        if (obj == null) {
            return "null";
        }

        boolean isPrimitiveOrWrapped =
                obj.getClass().isPrimitive() || ClassUtils.wrapperToPrimitive(obj.getClass()) != null;

        if (isPrimitiveOrWrapped) {
            return String.valueOf(obj);
        } else if (obj instanceof Date) {
            return ((Date) obj).toString();
        } else if (obj instanceof Calendar) {
            return ((Calendar) obj).getTime().toString();
        } else if(isArray(obj)) {
            return toStringFromArray(obj);
        } else {
            Method method = obj.getClass().getMethod("toString");
            return (String) method.invoke(obj);
        }
    }

    /**
     * Transforms a Map of <String, ?> into a ValueMap.
     *
     * @param map
     * @return a ValueMap of the parameter map
     */
    public static ValueMap toValueMap(final Map<String, ?> map) {
        final Map<String, Object> objectMap = new LinkedHashMap<String, Object>(map.size());

        for (final Map.Entry<String, ?> entry : map.entrySet()) {
            objectMap.put(entry.getKey(), entry.getValue());
        }

        return new ValueMapDecorator(objectMap);
    }


    private static boolean isArray(final Object obj) {
        return obj instanceof Object[]
                || obj instanceof boolean[]
                || obj instanceof byte[]
                || obj instanceof short[]
                || obj instanceof char[]
                || obj instanceof int[]
                || obj instanceof long[]
                || obj instanceof float[]
                || obj instanceof double[];
    }

    private static String toStringFromArray(final Object obj) {
        if (obj instanceof Object[]) {
            return Arrays.deepToString((Object[]) obj);
        } else if (obj instanceof boolean[]) {
            return Arrays.toString((boolean[]) obj);
        } else if (obj instanceof byte[]) {
            return Arrays.toString((byte[]) obj);
        } else if (obj instanceof short[]) {
            return Arrays.toString((short[]) obj);
        } else if (obj instanceof char[]) {
            return Arrays.toString((char[]) obj);
        } else if (obj instanceof int[]) {
            return Arrays.toString((int[]) obj);
        } else if (obj instanceof long[]) {
            return Arrays.toString((long[]) obj);
        } else if (obj instanceof float[]) {
            return Arrays.toString((float[]) obj);
        } else if (obj instanceof double[]) {
            return Arrays.toString((double[]) obj);
        }

        log.warn("Object is not an Array");

        return null;
    }
}