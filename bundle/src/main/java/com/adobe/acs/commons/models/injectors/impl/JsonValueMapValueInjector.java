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
package com.adobe.acs.commons.models.injectors.impl;


import com.adobe.acs.commons.models.injectors.annotation.JsonValueMapValue;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.apache.sling.models.spi.Injector;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.adobe.acs.commons.models.injectors.impl.InjectorUtils.getResource;
import static com.adobe.acs.commons.util.impl.ReflectionUtil.getClassOrGenericParam;
import static com.adobe.acs.commons.util.impl.ReflectionUtil.getGenericParameter;
import static com.adobe.acs.commons.util.impl.ReflectionUtil.isArray;
import static com.adobe.acs.commons.util.impl.ReflectionUtil.isCollectionType;
import static com.adobe.acs.commons.util.impl.ReflectionUtil.isListType;
import static com.adobe.acs.commons.util.impl.ReflectionUtil.isSetType;
import static com.adobe.acs.commons.util.impl.ReflectionUtil.toArray;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

/**
 * JsonValueMapValueInjector
 * Injects a POJO into a field using GSON.
 * Supports a list or a single value.
 */
@Component(
        property = {
                Constants.SERVICE_RANKING + ":Integer=5501"
        },
        service = Injector.class
)
public class JsonValueMapValueInjector implements Injector {

    @Override
    public String getName() {
        return JsonValueMapValue.SOURCE;
    }

    private static final Gson GSON = new Gson();

    @Override
    public Object getValue(Object adaptable, String name, Type declaredType, AnnotatedElement element, DisposalCallbackRegistry callbackRegistry) {

        if (element.isAnnotationPresent(JsonValueMapValue.class)) {
            Resource resource = getResource(adaptable);
            JsonValueMapValue annotation = element.getAnnotation(JsonValueMapValue.class);
            String key = defaultIfEmpty(annotation.name(), name);
            String[] jsonStringArray = resource.getValueMap().get(key, String[].class);
            return parseValue(declaredType, jsonStringArray, key, resource);
        }

        return null;
    }

    private Object parseValue(Type declaredType,String[] jsonStringArray, String key, Resource resource){
         if ((isSetType(declaredType)) && isNotEmpty(jsonStringArray)) {
             return createSet(jsonStringArray, getGenericParameter(declaredType));
         } else if ((isListType(declaredType) || isCollectionType(declaredType)) && isNotEmpty(jsonStringArray)) {
             return createList(jsonStringArray, getGenericParameter(declaredType));
         } else if (isArray(declaredType) && isNotEmpty(jsonStringArray)) {
             return createArray(jsonStringArray, getGenericParameter(declaredType));
         } else if (resource.getValueMap().containsKey(key)) {
             String jsonString = resource.getValueMap().get(key, String.class);
             return GSON.fromJson(jsonString, getClassOrGenericParam(declaredType));
         } else {
             return null;
         }
    }


    private <T> T[] createArray(String[] jsonStringList, Class<T> targetClass) {
        return toArray(createList(jsonStringList, targetClass));
    }

    private <T> Set<T> createSet(String[] jsonStringArray, Class<T> targetClass) {
        return new HashSet<>(createList(jsonStringArray, targetClass));
    }

    private <T> List<T> createList(String[] jsonStringArray, Class<T> targetClass) {

        if (isEmpty(jsonStringArray)) {
            return Collections.emptyList();
        }

        List<String> jsonStringList = Arrays.asList(jsonStringArray);
        return jsonStringList.stream()
                .map(json -> GSON.fromJson(json, targetClass))
                .collect(Collectors.toList());
    }


}
