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

import com.adobe.acs.commons.mcp.form.FieldComponent;
import com.adobe.acs.commons.mcp.form.FormField;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.adobe.acs.commons.mcp.util.IntrospectionUtil.getCollectionComponentType;
import static com.adobe.acs.commons.mcp.util.IntrospectionUtil.hasMultipleValues;
import static com.adobe.acs.commons.mcp.util.ValueMapSerializer.serializeToStringArray;

/**
 * Processing routines for handing ProcessInput within a FormProcessor
 */
public class AnnotatedFieldDeserializer {

    private static final Logger LOG = LoggerFactory.getLogger(AnnotatedFieldDeserializer.class);

    public static void deserializeFormFields(Object target, ValueMap input) throws DeserializeException {
        List<Field> fields = FieldUtils.getFieldsListWithAnnotation(target.getClass(), FormField.class);
        deserializeFields(target, fields, input);
    }

    public static void deserializeFields(Object target, List<Field> fields, ValueMap input) throws DeserializeException {
        for (Field field : fields) {
            try {
                parseInput(target, input, field);
            } catch (ParseException | ReflectiveOperationException | NullPointerException ex) {
                throw new DeserializeException("Error when processing field " + field.getName(), ex);
            }
        }
    }

    @SuppressWarnings("squid:S3776")
    private static void parseInput(Object target, ValueMap input, Field field) throws ReflectiveOperationException, ParseException {
        FormField inputAnnotation = field.getAnnotation(FormField.class);
        Object value;
        if (input.get(field.getName()) == null) {
            if (field.getType() == Boolean.class || field.getType() == Boolean.TYPE) {
                value = false;
            } else if (inputAnnotation != null && inputAnnotation.required()) {
                throw new NullPointerException("Required field missing: " + field.getName());
            } else {
                return;
            }
        } else {
            value = input.get(field.getName());
        }

        if (hasMultipleValues(field.getType())) {
            parseInputList(target, serializeToStringArray(value), field);
        } else {
            Object val = value;
            if (value.getClass().isArray()) {
                val = ((Object[]) value)[0];
            }

            if (val instanceof RequestParameter) {
                /**
                 * Special case handling uploaded files; Method call ~ copied
                 * from parseInputValue(..)
                 */
                if (field.getType() == RequestParameter.class) {
                    FieldUtils.writeField(field, target, val, true);
                } else {
                    try {
                        FieldUtils.writeField(field, target, ((RequestParameter) val).getInputStream(), true);
                    } catch (IOException ex) {
                        LOG.error("Unable to get InputStream for uploaded file [ {} ]", ((RequestParameter) val).getName(), ex);
                    }
                }
            } else {
                parseInputValue(target, String.valueOf(val), field);
            }
        }
    }

    private static void parseInputList(Object target, String[] values, Field field) throws ReflectiveOperationException, ParseException {
        List convertedValues = new ArrayList();
        Class type = getCollectionComponentType(field);
        for (String value : values) {
            Object val = convertValue(value, type);
            convertedValues.add(val);
        }
        if (field.getType().isArray()) {
            Object array = Array.newInstance(field.getType().getComponentType(), convertedValues.size());
            for (int i = 0; i < convertedValues.size(); i++) {
                Array.set(array, i, convertedValues.get(i));
            }
            FieldUtils.writeField(field, target, array, true);
        } else {
            Collection c = (Collection) getInstantiatableListType(field.getType()).newInstance();
            c.addAll(convertedValues);
            FieldUtils.writeField(field, target, c, true);
        }
    }

    private static void parseInputValue(Object target, String value, Field field) throws ReflectiveOperationException, ParseException {
        FieldUtils.writeField(field, target, convertValue(value, field.getType()), true);
    }

    private static Object convertValue(String value, Class<?> type) throws ParseException {
        Class clazz = type.isArray() ? type.getComponentType() : type;
        if (clazz.isPrimitive() || Number.class.isAssignableFrom(clazz) || clazz == Boolean.class) {
            return convertPrimitiveValue(value, clazz);
        } else if (clazz == String.class) {
            return value;
        } else if (clazz.isEnum()) {
            return Enum.valueOf((Class<Enum>) clazz, value);
        }

        return null;
    }

    @SuppressWarnings("squid:S3776")
    private static Object convertPrimitiveValue(String value, Class<?> type) throws ParseException {
        if (type.equals(Boolean.class) || type.equals(Boolean.TYPE)) {
            return value.toLowerCase().trim().equals("true");
        } else {
            NumberFormat numberFormat = NumberFormat.getNumberInstance();
            Number num = numberFormat.parse(value);
            if (type.equals(Byte.class) || type.equals(Byte.TYPE)) {
                return num.byteValue();
            } else if (type.equals(Double.class) || type.equals(Double.TYPE)) {
                return num.doubleValue();
            } else if (type.equals(Float.class) || type.equals(Float.TYPE)) {
                return num.floatValue();
            } else if (type.equals(Integer.class) || type.equals(Integer.TYPE)) {
                return num.intValue();
            } else if (type.equals(Long.class) || type.equals(Long.TYPE)) {
                return num.longValue();
            } else if (type.equals(Short.class) || type.equals(Short.TYPE)) {
                return num.shortValue();
            } else {
                return null;
            }
        }
    }

    private static Class getInstantiatableListType(Class<?> type) {
        if (type == List.class || type == Collection.class || type == Iterable.class) {
            return ArrayList.class;
        } else if (type == Set.class) {
            return LinkedHashSet.class;
        } else {
            return type;
        }
    }

    public static Stream<AccessibleObject> getAllAnnotatedObjectMembers(Class source, Class<? extends Annotation> annotation) {
        return Stream.concat(
                FieldUtils.getFieldsListWithAnnotation(source, annotation).stream()
                        .sorted(AnnotatedFieldDeserializer::superclassFieldsFirst),
                MethodUtils.getMethodsListWithAnnotation(source, annotation).stream()
        );
    }

    public static Map<String, FieldComponent> getFormFields(Class source, SlingScriptHelper sling) {
        return getAllAnnotatedObjectMembers(source, FormField.class)
                .collect(Collectors.toMap(AccessibleObjectUtil::getFieldName, f -> {
                    FormField fieldDefinition = f.getAnnotation(FormField.class);
                    FieldComponent component;
                    try {
                        component = fieldDefinition.component().newInstance();
                        component.setup(AccessibleObjectUtil.getFieldName(f), f, fieldDefinition, sling);
                        return component;
                    } catch (InstantiationException | IllegalAccessException ex) {
                        LOG.error("Unable to instantiate field component for " + f.toString(), ex);
                    }
                    return null;
                }, (a, b) -> a, LinkedHashMap::new));
    }

    private static int superclassFieldsFirst(Field a, Field b) {
        if (a.getDeclaringClass() == b.getDeclaringClass()) {
            return 0;
        } else if (a.getDeclaringClass().isAssignableFrom(b.getDeclaringClass())) {
            return -1;
        } else {
            return 1;
        }
    }

    private AnnotatedFieldDeserializer() {
        // Utility class has no constructor
    }
}
