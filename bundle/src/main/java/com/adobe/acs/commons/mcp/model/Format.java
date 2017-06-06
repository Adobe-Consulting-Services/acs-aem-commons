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
package com.adobe.acs.commons.mcp.model;

import java.lang.reflect.Field;
import java.util.function.Function;

/**
 * Describes the desired output format of a value within a report
 */
public enum Format {
    plain, storageSize("_short", Format::getHumanSize);
    int columnCount = 1;
    String suffix = "";
    Function<Object, Object> altFunction;

    Format() {
    }

    Format(String alternate, Function<Object, Object> altFunc) {
        columnCount = 2;
        suffix = alternate;
        altFunction = altFunc;
    }

    public static Format forField(Enum e) {
        try {
            Field f = e.getDeclaringClass().getField(e.name());
            if (!f.isAnnotationPresent(FieldFormat.class)) {
                return Format.plain;
            } else {
                FieldFormat rf = f.getAnnotation(FieldFormat.class);
                return rf.value();
            }
        } catch (IllegalArgumentException | NoSuchFieldException | SecurityException ex) {
            // Ignore errors, assume plain
        }
        return Format.plain;
    }

    public Object getAlternateValue(Object val) {
        return altFunction.apply(val);
    }

    public static String getHumanSize(Object val) {
        Long v = (val instanceof Long) ? (Long) val : Long.parseLong(String.valueOf(val));
        int magnitude = ( Long.numberOfTrailingZeros(Long.highestOneBit(v)));
        String scale = "B";
        if (magnitude >= 50) {
            scale = "PB";
        } else if (magnitude >= 40) {
            scale = "TB";
        } else if (magnitude >= 30) {
            scale = "GB";
        } else if (magnitude >= 20) {
            scale = "MB";
        } else if (magnitude >= 10) {
            scale = "KB";
        }
        Long shortVal = (v >> ((magnitude / 10)*10));
        return shortVal + " " + scale;
    }    
}
