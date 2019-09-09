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

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.function.Function;
import javax.inject.Named;
import org.apache.commons.lang3.StringUtils;

/**
 * Methods to handle AccessibleObjects (field and methods) in a similar manner
 */
public class AccessibleObjectUtil {

    public static <T> T processDualFunction(AccessibleObject o,
            Function<Method, T> methodFunction,
            Function<Field, T> fieldFunction) {
        if (o == null) {
            return null;
        } else if (o instanceof Method) {
            return methodFunction.apply((Method) o);
        } else if (o instanceof Field) {
            return fieldFunction.apply((Field) o);
        } else {
            return null;
        }
    }

    /**
     * Get the property type (assuming it's either a field or a getter method)
     * @param accessibleObject
     * @return
     */
    public static Class<?> getType(AccessibleObject accessibleObject) {
        return processDualFunction(accessibleObject, Method::getReturnType, Field::getType);
    }

    /**
     * Get the genericproperty type (assuming it's either a field or a getter method)
     * @param accessibleObject
     * @return
     */
    public static Type getGenericType(AccessibleObject accessibleObject) {
        return processDualFunction(accessibleObject, Method::getGenericReturnType, Field::getGenericType);
    }

    /**
     * Return field name or extract property name from getter method name if needed
     * @param o Field or Method
     * @return String of extracted name, or null if not able to convert
     */
    public static String getFieldName(AccessibleObject o) {
        if (o == null) {
            return null;
        }
        Named named = o.getAnnotation(Named.class);
        if (named != null) {
            return named.value();
        } else if (o instanceof Field) {
            Field f = (Field) o;
            return f.getName();
        } else if (o instanceof Method) {
            Method m = (Method) o;
            String name = m.getName();
            if (name.startsWith("get")) {
                return StringUtils.uncapitalize(name.substring(3));
            } else if (name.startsWith("is")) {
                return StringUtils.uncapitalize(name.substring(2));
            } else {
                return name;
            }
        } else {
            return null;
        }
    }

    private AccessibleObjectUtil() {
        // Static util class has no constructor
    }
}
