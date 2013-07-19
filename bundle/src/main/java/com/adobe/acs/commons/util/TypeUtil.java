package com.adobe.acs.commons.util;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class TypeUtil {
    private static final Logger log = LoggerFactory.getLogger(TypeUtil.class);

    private static final Pattern JSON_DATE = Pattern.compile("^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]{3}[-+]{1}[0-9]{2}[:]{0,1}[0-9]{2}$");
    private static final String REFERENCE = "jcr:reference:";
    private static final String PATH = "jcr:path:";
    private static final String NAME = "jcr:name:";
    private static final String URI = "jcr:uri:";

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
    public static <T> Map<T, T> ArrayToMap(T[] list) {
        final HashMap<T, T> map = new HashMap<T, T>();
        if (list == null) {
            return map;
        }
        if (list.length > 0 && (list.length % 2) == 1) {
            throw new IllegalArgumentException("Array must be even in length, representing a series of Key, Value pairs.");
        }

        for (int i = 0; i < list.length; i++) {
            map.put(list[i], list[++i]);
        }

        return map;
    }

    /**
     * Converts a JSONObject to a simple Map. This will only work properly for
     * JSONObjects of depth 1.
     *
     * @param json
     * @return
     */
    public static Map<String, Object> toMap(JSONObject json) throws JSONException {
        final HashMap<String, Object> map = new HashMap<String, Object>();
        final List<?> keys = IteratorUtils.toList(json.keys());

        for (final Object key : keys) {
            String str = key.toString();
            map.put(str, json.get(str));
        }

        return map;
    }

    /**
     * Determines the type of the parameter object
     * 
     * TODO - review this method
     *
     * @param object
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
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
        }

        return (Class<T>) String.class;
    }

    /**
     * @param data
     * @param klass
     * @param <T>
     * @return
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
        } else if (StringUtils.equalsIgnoreCase("true", data)) {
            return klass.cast(Boolean.TRUE);
        } else if (StringUtils.equalsIgnoreCase("false", data)) {
            return klass.cast(Boolean.FALSE);
        } else if (JSON_DATE.matcher(data).matches()) {
            return klass.cast(ISODateTimeFormat.dateTimeParser().parseDateTime(data).toDate());
        } else {
            return klass.cast(data);
        }
    }

    /**
     * Gets the default string representation of the parameter object
     *
     * @param obj
     * @param klass
     * @return
     */
    public static String toString(final Object obj, final Class<?> klass) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        return toString(obj, klass, null);
    }

    /**
     * Gets a custom string representation based on the parameter (0 arguement) methodName
     *
     * @param obj
     * @param klass
     * @param methodName
     * @return
     */
    public static String toString(final Object obj, final Class<?> klass, String methodName) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        if (StringUtils.isBlank(methodName)) {
            methodName = "toString";
        }

        Method method = klass.getMethod(methodName);
        return (String) method.invoke(obj);
    }
}
