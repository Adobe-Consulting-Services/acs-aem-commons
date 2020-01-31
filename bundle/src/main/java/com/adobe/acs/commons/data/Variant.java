/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2018 Adobe
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
package com.adobe.acs.commons.data;

import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Used to represent values that might be provided as one baseType but used as
 * another. Avoids glue code and switch statements in other parts of the code
 * especially dealing with data from spreadsheets.
 */
@ProviderType
public final class Variant {

    private Class baseType = null;
    private static final FastDateFormat STANDARD_DATE_FORMAT = FastDateFormat.getDateTimeInstance(FastDateFormat.SHORT, FastDateFormat.SHORT);
    private Optional<Long> longVal = Optional.empty();
    private Optional<Double> doubleVal = Optional.empty();
    private Optional<String> stringVal = Optional.empty();
    private Optional<Boolean> booleanVal = Optional.empty();
    private Optional<Date> dateVal = Optional.empty();

    private static final FastDateFormat[] DATE_FORMATS = {
            FastDateFormat.getDateInstance(FastDateFormat.SHORT),
            FastDateFormat.getDateInstance(FastDateFormat.LONG),
            FastDateFormat.getTimeInstance(FastDateFormat.SHORT),
            FastDateFormat.getTimeInstance(FastDateFormat.LONG),
            STANDARD_DATE_FORMAT,
            FastDateFormat.getDateTimeInstance(FastDateFormat.LONG, FastDateFormat.SHORT),
            FastDateFormat.getDateTimeInstance(FastDateFormat.SHORT, FastDateFormat.LONG),
            FastDateFormat.getDateTimeInstance(FastDateFormat.LONG, FastDateFormat.LONG),
            FastDateFormat.getDateTimeInstance(FastDateFormat.FULL, FastDateFormat.FULL),
            FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    };

    public Variant() {
    }

    public <T> Variant(T src) {
        setValue(src);
    }

    public Variant(Cell src) {
        this(src, Locale.getDefault());
    }

    public Variant(Cell src, Locale locale) {
        setValue(src, locale);
    }

    public void clear() {
        longVal = Optional.empty();
        doubleVal = Optional.empty();
        stringVal = Optional.empty();
        booleanVal = Optional.empty();
        dateVal = Optional.empty();
    }

    public boolean isEmpty() {
        return !stringVal.isPresent()
                && !longVal.isPresent()
                && !doubleVal.isPresent()
                && !dateVal.isPresent()
                && !booleanVal.isPresent();
    }

    @SuppressWarnings("squid:S00115")
    public static enum CellType {
        // POI switches from int-based to enum-based constants, and unfortunately they also removed things along the way.
        // This bridges the gap between the constants and the enum types.
        // _NONE is used to match POI 4, see https://poi.apache.org/apidocs/4.0/org/apache/poi/ss/usermodel/CellType.html
        // therefore ignoring the code climate issue for this
        _NONE(3), NUMERIC(0), STRING(1), FORMULA(2), BLANK(3), BOOLEAN(4), ERROR(5);
        int ord;
        CellType(int ord) {
            this.ord=ord;
        }

        public static CellType fromObject(Object o) {
            if (o.getClass() == String.class || o.getClass().isEnum()) {
                return CellType.valueOf(o.toString());
            } else {
                int num = (Integer) o;
                for (CellType ct:values()) {
                    if (ct.ord == num) {
                        return ct;
                    }
                }
                return null;
            }
        }
    }

    private void setValue(Cell cell, Locale locale) {
        try {
            // Use reflection to access the method as it changes return type from int to CellType in 4.x
            CellType cellType = CellType.fromObject(MethodUtils.invokeMethod(cell, "getCellType"));
            if (cellType == CellType.FORMULA) {
                // Use reflection to access the method as it changes return type from int to CellType in 4.x
                cellType = CellType.fromObject(MethodUtils.invokeMethod(cell,"getCachedFormulaResultType"));
            }
            switch (cellType) {
                case BOOLEAN:
                    setValue(cell.getBooleanCellValue());
                    break;
                case NUMERIC:
                    double number = cell.getNumericCellValue();
                    if (Math.floor(number) == number) {
                        setValue((long) number);
                    } else {
                        setValue(number);
                    }
                    if (DateUtil.isCellDateFormatted(cell)) {
                        setValue(cell.getDateCellValue());
                        baseType = Calendar.class;
                    }
                    DataFormatter dataFormatter = new DataFormatter(locale);
                    if (cellType == CellType.FORMULA) {
                        setValue(dataFormatter.formatCellValue(cell));
                    } else {
                        CellStyle cellStyle = cell.getCellStyle();
                        setValue(dataFormatter.formatRawCellContents(
                                cell.getNumericCellValue(),
                                cellStyle.getDataFormat(),
                                cellStyle.getDataFormatString()
                        ));
                    }
                    break;
                case STRING:
                    setValue(cell.getStringCellValue().trim());
                    break;
                case BLANK:
                default:
                    clear();
                    break;
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            Logger.getLogger(Variant.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @SuppressWarnings("squid:S3776")
    public final <T> void setValue(T val) {
        if (val == null) {
            return;
        }
        Class valueType = val.getClass();
        if (valueType == Variant.class) {
            Variant v = (Variant) val;
            longVal = v.longVal;
            doubleVal = v.doubleVal;
            stringVal = v.stringVal;
            booleanVal = v.booleanVal;
            dateVal = v.dateVal;
            this.baseType = v.baseType;
        } else if (valueType == Byte.TYPE || valueType == Byte.class) {
            setLongVal(((Byte) val).longValue());
            if (baseType == null || baseType == String.class) {
                baseType = Long.TYPE;
            }
        } else if (valueType == Integer.TYPE || valueType == Integer.class) {
            setLongVal(((Integer) val).longValue());
            if (baseType == null || baseType == String.class) {
                baseType = Long.TYPE;
            }
        } else if (valueType == Long.TYPE || valueType == Long.class) {
            setLongVal((Long) val);
            if (baseType == null || baseType == String.class) {
                baseType = Long.TYPE;
            }
        } else if (valueType == Short.TYPE || valueType == Short.class) {
            setLongVal(((Short) val).longValue());
            if (baseType == null || baseType == String.class) {
                baseType = Long.TYPE;
            }
        } else if (valueType == Float.TYPE || valueType == Float.class
                || valueType == Double.TYPE || valueType == Double.class) {
            setDoubleVal((Double) val);
            if (baseType == null || baseType == String.class) {
                baseType = Double.TYPE;
            }
        } else if (valueType == Boolean.TYPE || valueType == Boolean.class) {
            setBooleanVal((Boolean) val);
            if (baseType == null || baseType == String.class) {
                baseType = Boolean.TYPE;
            }
        } else if (valueType == String.class) {
            setStringVal((String) val);
            if (baseType == null) {
                baseType = String.class;
            }
        } else if (valueType == Date.class) {
            setDateVal((Date) val);
            baseType = Calendar.class;
        } else if (valueType == Instant.class) {
            setDateVal(new Date(((Instant) val).toEpochMilli()));
            baseType = Calendar.class;
        } else if (valueType == Calendar.class) {
            setDateVal(((Calendar) val).getTime());
            baseType = Calendar.class;
        } else {
            setStringVal(String.valueOf(val));
            if (baseType == null) {
                baseType = String.class;
            }
        }
    }

    private void setLongVal(Long l) {
        longVal = l == null ? Optional.empty() : Optional.of(l);
    }

    private void setDoubleVal(Double d) {
        doubleVal = d == null ? Optional.empty() : Optional.of(d);
    }

    private void setStringVal(String s) {
        if (s != null && !s.isEmpty()) {
            stringVal = Optional.of(s);
        } else {
            stringVal = Optional.empty();
        }
    }

    private void setBooleanVal(Boolean b) {
        booleanVal = b == null ? Optional.empty() : Optional.of(b);
    }

    private void setDateVal(Date d) {
        dateVal = d == null ? Optional.empty() : Optional.of(d);
    }

    public Long toLong() {
        return longVal.orElse(dateVal.map(Date::getTime)
                .orElse(doubleVal.map(Double::longValue)
                        .orElse(booleanVal.map(b -> (Long) (b ? 1L : 0L))
                                .orElseGet(() -> {
                                    try {
                                        return stringVal.map(s -> (long) Double.parseDouble(s)).orElse(null);
                                    } catch (NumberFormatException ex) {
                                        return null;
                                    }
                                }))));
    }

    public Double toDouble() {
        return doubleVal.orElse(longVal.map(Long::doubleValue)
                .orElse(booleanVal.map(b -> (Double) (b ? 1.0 : 0.0))
                        .orElseGet(() -> {
                            try {
                                return stringVal.map(Double::parseDouble).orElse(null);
                            } catch (NumberFormatException ex) {
                                return null;
                            }
                        })));
    }

    @Override
    public String toString() {
        return stringVal.orElse(dateVal.map(STANDARD_DATE_FORMAT::format)
                .orElse(doubleVal.map(String::valueOf)
                        .orElse(longVal.map(String::valueOf)
                                .orElse(booleanVal.map(String::valueOf)
                                        .orElse(null)))));
    }

    public Date toDate() {
        return dateVal.orElse(longVal.map(Date::new)
                .orElse(stringVal.map(s -> {
                    for (FastDateFormat format : DATE_FORMATS) {
                        try {
                            return format.parse(s);
                        } catch (ParseException ex) {
                            // No good, go to the next pattern
                        }
                    }
                    return null;
                }).orElse(null)));
    }

    public Boolean toBoolean() {
        return booleanVal.orElse(longVal.map(l -> l != 0)
                .orElse(doubleVal.map(d -> d != 0)
                        .orElse(stringVal.map(this::isStringTruthy)
                                .orElse(null))));
    }

    /**
     * Truthiness is any non-empty string that looks like a non-zero number or
     * looks like it is True, Yes, or X
     *
     * @param s String to evaluate
     * @return True if it is truthy, otherwise false
     */
    public boolean isStringTruthy(String s) {
        if (s == null || s.trim().isEmpty()) {
            return false;
        } else if (StringUtils.isNumeric(s)) {
            return Long.parseLong(s) != 0;
        } else {
            char c = s.trim().toLowerCase().charAt(0);
            return (c == 't' || c == 'y' || c == 'x');
        }
    }

    private <U, T> T apply(U value, Function<U, T> func) {
        return value == null ? null : func.apply(value);
    }

    @SuppressWarnings("squid:S3776")
    public <T> T asType(Class<T> type) {
        if (type == Byte.TYPE || type == Byte.class) {
            return (T) apply(toLong(), Long::byteValue);
        } else if (type == Integer.TYPE || type == Integer.class) {
            return (T) apply(toLong(), Long::intValue);
        } else if (type == Long.TYPE || type == Long.class) {
            return (T) toLong();
        } else if (type == Short.TYPE || type == Short.class) {
            return (T) apply(toLong(), Long::shortValue);
        } else if (type == Float.TYPE || type == Float.class) {
            return (T) apply(toDouble(), Double::floatValue);
        } else if (type == Double.TYPE || type == Double.class) {
            return (T) toDouble();
        } else if (type == Boolean.TYPE || type == Boolean.class) {
            return (T) toBoolean();
        } else if (type == String.class) {
            return (T) toString();
        } else if (type == Date.class) {
            return (T) toDate();
        } else if (type == Instant.class) {
            return (T) toDate().toInstant();
        } else if (type == Calendar.class) {
            Calendar c = Calendar.getInstance();
            c.setTime(toDate());
            return (T) c;
        } else {
            return null;
        }
    }

    public static <S, D> D convert(S val, Class<D> destType) {
        Variant v = new Variant(val);
        return v.asType(destType);
    }

    Class getBaseType() {
        return baseType;
    }
}
