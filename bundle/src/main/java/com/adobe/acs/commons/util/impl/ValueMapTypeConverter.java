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

import com.day.cq.commons.inherit.InheritanceValueMap;
import org.apache.commons.lang3.ClassUtils;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.adobe.acs.commons.util.impl.ReflectionUtil.getGenericParameter;
import static com.adobe.acs.commons.util.impl.ReflectionUtil.isSetType;

/**
 * Converts value map values to the value with the desired type.
 */
public class ValueMapTypeConverter {

    private final String name;
    private final Type declaredType;
    private final Object convertedValue;

    private ValueMap valueMap;
    private InheritanceValueMap inheritanceValueMap;

    private static final Logger LOG = LoggerFactory.getLogger(ValueMapTypeConverter.class);

    public ValueMapTypeConverter(InheritanceValueMap inheritanceValueMap, String name, Type declaredType) {
        this.inheritanceValueMap = inheritanceValueMap;
        this.name = name;
        this.declaredType = declaredType;
        this.convertedValue = convertValue();
    }

    public ValueMapTypeConverter(ValueMap valueMap, String name, Type declaredType) {
        this.valueMap = valueMap;
        this.name = name;
        this.declaredType = declaredType;
        this.convertedValue = convertValue();
    }

    private Object convertValue() {
        if (declaredType instanceof Class<?>) {
            Class<?> clazz;
            try {
                clazz = (Class<?>) declaredType;
                if(clazz.isArray()){
                    return handleArrayProperty(clazz);
                }else{
                    return getValueFromMap(clazz);
                }
            } catch (ClassCastException e) {
                return null;
            }
        } else if (ParameterizedType.class.isInstance(declaredType)) {
            return handleCollectionTypes((ParameterizedType) declaredType);
        } else {
            LOG.debug("ValueMapTypeConverter doesn't support non-class types {}", declaredType);
            return null;
        }
    }

    private Object handleCollectionTypes(ParameterizedType pType) {
        // list support
        if (pType.getActualTypeArguments().length != 1) {
            return null;
        }
        Class<?> collectionType = (Class<?>) pType.getRawType();
        if (!isCollectionTypeSupported(collectionType)) {
            return null;
        }
        if (isSetType(collectionType)) {
            return handleSetType(pType);
        } else {
            return handleListType(pType);
        }

    }

    private Object handleSetType(ParameterizedType pType) {
        return new HashSet<>((List) handleListType(pType));
    }

    private Object handleListType(ParameterizedType pType) {
        Class<?> genericParameter = getGenericParameter(pType);
        Object array = getValueFromMap(Array.newInstance(genericParameter, 0).getClass());
        if (array == null) {
            return null;
        }
        return Arrays.asList((Object[]) array);
    }

    private boolean isCollectionTypeSupported(Class<?> collectionType) {
        return collectionType.equals(Collection.class) || collectionType.equals(List.class) || collectionType.equals(Set.class);
    }

    private Object handleArrayProperty(Class<?> clazz) {
        // handle case of primitive/wrapper arrays
        Class<?> componentType = clazz.getComponentType();
        if (componentType.isPrimitive()) {
            Class<?> wrapper = ClassUtils.primitiveToWrapper(componentType);
            if (wrapper != componentType) {
                Object wrapperArray = getValueFromMap(Array.newInstance(wrapper, 0).getClass());
                if (wrapperArray != null) {
                    return unwrapArray(wrapperArray, componentType);
                }
            }
        }else{
            Object wrapperArray = getValueFromMap(Array.newInstance(componentType, 0).getClass());
            if (wrapperArray != null) {
                return unwrapArray(wrapperArray, componentType);
            }
        }
        return null;
    }

    private Object unwrapArray(Object wrapperArray, Class<?> primitiveType) {
        int length = Array.getLength(wrapperArray);
        Object primitiveArray = Array.newInstance(primitiveType, length);
        for (int i = 0; i < length; i++) {
            Array.set(primitiveArray, i, Array.get(wrapperArray, i));
        }
        return primitiveArray;
    }

    private Object getValueFromMap(Class<?> type) {
        if (inheritanceValueMap != null) {
            return inheritanceValueMap.getInherited(name, type);
        } else {
            return valueMap.get(name, type);
        }
    }

    public Object getConvertedValue() {
        return convertedValue;
    }


}
