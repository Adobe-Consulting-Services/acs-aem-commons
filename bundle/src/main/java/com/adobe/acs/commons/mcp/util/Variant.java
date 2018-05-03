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
package com.adobe.acs.commons.mcp.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;
import org.apache.poi.ss.usermodel.Cell;

/**
 * Used to represent values that might be provided as one type but used as another. Avoids glue code and switch
 * statements in other parts of the code expecially dealing with data from spreadsheets.
 */
public class Variant {

    private static final DateFormat DATE_FORMAT = SimpleDateFormat.getDateTimeInstance();
    private Optional<Long> longVal = Optional.empty();
    private Optional<Double> doubleVal = Optional.empty();
    private Optional<String> stringVal = Optional.empty();
    private Optional<Boolean> booleanVal = Optional.empty();
    private Optional<Date> dateVal = Optional.empty();

    public Variant() {
    }

    public <T> Variant(T src) {
        setValue(src);
    }

    public Variant(Cell src) {
        setValue(src);
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

    private void setValue(Cell cell) {
        int cellType = cell.getCellType();
        if (cellType == Cell.CELL_TYPE_FORMULA) {
            cellType = cell.getCachedFormulaResultType();
        }
        switch (cellType) {
            case Cell.CELL_TYPE_BOOLEAN:
                setValue(cell.getBooleanCellValue());
                break;
            case Cell.CELL_TYPE_NUMERIC:
                double number = cell.getNumericCellValue();
                if (Math.floor(number) == number) {
                    setValue((long) number);
                } else {
                    setValue(number);
                }
                setValue(cell.getDateCellValue());
                break;
            case Cell.CELL_TYPE_STRING:
                setValue(cell.getStringCellValue().trim());
                break;
            case Cell.CELL_TYPE_BLANK:
            default:
                clear();
                break;
        }
    }

    @SuppressWarnings("squid:S3776")
    public final <T> void setValue(T val) {
        if (val == null) {
            return;
        }
        Class type = val.getClass();
        if (type == Variant.class) {
            Variant v = (Variant) val;
            longVal = v.longVal;
            doubleVal = v.doubleVal;
            stringVal = v.stringVal;
            booleanVal = v.booleanVal;
            dateVal = v.dateVal;
        } else if (type == Byte.TYPE || type == Byte.class) {
            setLongVal(((Byte) val).longValue());
        } else if (type == Integer.TYPE || type == Integer.class) {
            setLongVal(((Integer) val).longValue());
        } else if (type == Long.TYPE || type == Long.class) {
            setLongVal((Long) val);
        } else if (type == Short.TYPE || type == Short.class) {
            setLongVal(((Short) val).longValue());
        } else if (type == Float.TYPE || type == Float.class) {
            setDoubleVal((Double) val);
        } else if (type == Double.TYPE || type == Double.class) {
            setDoubleVal((Double) val);
        } else if (type == Boolean.TYPE || type == Boolean.class) {
            setBooleanVal((Boolean) val);
        } else if (type == String.class) {
            setStringVal((String) val);
        } else if (type == Date.class) {
            setDateVal((Date) val);
        } else if (type == Instant.class) {
            setDateVal(new Date(((Instant) val).toEpochMilli()));
        } else if (type == Calendar.class) {
            setDateVal(((Calendar) val).getTime());
        } else {
            setStringVal(String.valueOf(val));
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
                                .orElse(stringVal.map(Long::parseLong)
                                        .orElse(null)))));
    }

    public Double toDouble() {
        return doubleVal.orElse(longVal.map(Long::doubleValue)
                .orElse(booleanVal.map(b -> (Double) (b ? 1.0 : 0.0))
                        .orElse(stringVal.map(Double::parseDouble)
                                .orElse(null))));
    }

    public String toString() {
        return stringVal.orElse(dateVal.map(DATE_FORMAT::format)
                .orElse(doubleVal.map(String::valueOf)
                        .orElse(longVal.map(String::valueOf)
                                .orElse(booleanVal.map(String::valueOf)
                                        .orElse(null)))));
    }

    public Date toDate() {
        return dateVal.orElse(longVal.map(Date::new)
                .orElse(stringVal.map(s -> {
                    try {
                        return DATE_FORMAT.parse(s);
                    } catch (ParseException ex) {
                        return null;
                    }
                }).orElse(null)));
    }

    public Boolean toBoolean() {
        return booleanVal.orElse(longVal.map(l -> l != 0)
                .orElse(doubleVal.map(d -> d != 0)
                        .orElse(stringVal.map(Boolean::parseBoolean)
                                .orElse(null))));
    }

    @SuppressWarnings("squid:S3776")
    public <T> T asType(Class<T> type) {
        if (type == Byte.TYPE || type == Byte.class) {
            return (T) (Byte) toLong().byteValue();
        } else if (type == Integer.TYPE || type == Integer.class) {
            return (T) (Integer) toLong().intValue();
        } else if (type == Long.TYPE || type == Long.class) {
            return (T) toLong();
        } else if (type == Short.TYPE || type == Short.class) {
            return (T) (Short) toLong().shortValue();
        } else if (type == Float.TYPE || type == Float.class) {
            return (T) (Float) toDouble().floatValue();
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
}
