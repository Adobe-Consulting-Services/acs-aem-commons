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
package com.adobe.acs.commons.mcp.model;

import java.lang.reflect.Field;
import java.util.function.Function;

/**
 * Describes the desired output format of a value within a report
 */
@SuppressWarnings({"checkstyle:constantname", "squid:S00115"})
public enum ValueFormat {
    plain, storageSize("_short", ValueFormat::getHumanSize);
    int columnCount = 1;
    String suffix = "";
    Function<Object, Object> altFunction;

    ValueFormat() {
    }

    ValueFormat(String alternate, Function<Object, Object> altFunc) {
        columnCount = 2;
        suffix = alternate;
        altFunction = altFunc;
    }

    public static ValueFormat forField(Enum anEnum) {
        try {
            Field field = anEnum.getDeclaringClass().getField(anEnum.name());
            if (!field.isAnnotationPresent(FieldFormat.class)) {
                return ValueFormat.plain;
            } else {
                FieldFormat rf = field.getAnnotation(FieldFormat.class);
                return rf.value();
            }
        } catch (IllegalArgumentException | NoSuchFieldException | SecurityException ex) {
            // Ignore errors, assume plain
        }
        return ValueFormat.plain;
    }

    public Object getAlternateValue(Object val) {
        return altFunction.apply(val);
    }

    public static String getHumanSize(Object val) {
        if (val == null) {
            return null;
        }
        Long bytes = (val instanceof Long) ? (Long) val : Long.parseLong(String.valueOf(val));
        if (bytes < 1024) {
            return bytes + " b";
        }
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "kmgtpe".charAt(exp - 1);
        return String.format("%.1f %cb", bytes / Math.pow(1024, exp), pre);
    }
}
