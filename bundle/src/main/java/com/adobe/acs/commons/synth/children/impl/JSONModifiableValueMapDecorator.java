/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 - Adobe
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
package com.adobe.acs.commons.synth.children.impl;

import org.apache.sling.api.wrappers.ModifiableValueMapDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Special wrapper to handle deserialization of JSON data to value map.
 * <p>
 * Contains special handling for Date and Calendar interoperability.
 */
public final class JSONModifiableValueMapDecorator extends ModifiableValueMapDecorator {
    private static final Logger log = LoggerFactory.getLogger(JSONModifiableValueMapDecorator.class);

    /**
     * Creates an empty JSONModifiableValueMapDecorator.
     */
    public JSONModifiableValueMapDecorator() {
        super(new HashMap<String, Object>());
    }

    /**
     * Creates an pre-populated JSONModifiableValueMapDecorator.
     *
     * @param base the data to prepopulate the ValueMap with
     */
    public JSONModifiableValueMapDecorator(final Map<String, Object> base) {
        super(base);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Special implementation of get to handle Date and Calendar interoperability, BigDecimals and Integers.
     */
    @Override
    public <T> T get(String name, Class<T> type) {
        try {
            if (Calendar.class.equals(type)) {
                return (T) getCalendar(name);
            } else if (Date.class.equals(type)) {
                return (T) getDate(name);
            } else if (BigDecimal.class.equals(type)) {
                return (T) getBigDecimal(name);
            } else if (Integer.class.equals(type)) {
                return (T) getInteger(name);
            } else {
                return super.get(name, type);
            }
        } catch (Exception e) {
            log.warn("Unable to get property [ {} ] as [ {} ]. Returning null.", name, type);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T get(String name, T defaultValue) {
        if (defaultValue == null) {
            return (T) this.get(name);
        }

        Class<T> type = (Class<T>) defaultValue.getClass();

        T value = get(name, type);
        if (value == null) {
            value = defaultValue;
        }

        return value;
    }

    /**
     * Coerces the value at {@param name} to a Calendar object.
     *
     * @param name the property name
     * @return a Calendar obj representing the property value, or null if no value can be coerced.
     */
    private Calendar getCalendar(String name) {
        Object obj = super.get(name);
        if (obj instanceof Calendar) {
            return (Calendar) obj;
        } else if (obj instanceof Date) {
            Calendar cal = Calendar.getInstance();
            cal.setTime((Date) obj);
            return cal;
        } else {
            String tmp = super.get(name, String.class);
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(OffsetDateTime.parse(tmp).toInstant().toEpochMilli());
            return cal;
        }
    }

    /**
     * Coerces the value at {@param name} to a Date object.
     *
     * @param name the property name
     * @return a Date obj representing the property value, or null if no value can be coerced.
     */
    private Date getDate(String name) {
        Object obj = super.get(name);
        if (obj instanceof Date) {
            return (Date) obj;
        } else if (obj instanceof Calendar) {
            Calendar cal = (Calendar) obj;
            return cal.getTime();
        } else {
            String tmp = super.get(name, String.class);
            return new Date(OffsetDateTime.parse(tmp).toInstant().toEpochMilli());
        }
    }

    /**
     * Coerces the value at {@param name} to a Integer object.
     *
     * @param name the property name
     * @return a Integer obj representing the property value, or null if no value can be coerced.
     */
    private Integer getInteger(String name) {
        Object obj = super.get(name);
        if (obj instanceof Integer) {
            return (Integer) obj;
        } else if (obj instanceof Double) {
            Double tmp = (Double) obj;
            return tmp.intValue();
        } else if (obj instanceof Long) {
            Long tmp = (Long) obj;
            return tmp.intValue();
        } else {
            String tmp = super.get(name, String.class);
            return Integer.parseInt(tmp);
        }
    }

    /**
     * Coerces the value at {@param name} to a BigDecimal object.
     *
     * @param name the property name
     * @return a BigDecimal obj representing the property value, or null if no value can be coerced.
     */
    private BigDecimal getBigDecimal(String name) {
        Object obj = super.get(name);
        if (obj instanceof BigDecimal) {
            return (BigDecimal) obj;
        } else if (obj instanceof Double) {
            return BigDecimal.valueOf((Double) obj);
        } else if (obj instanceof Long) {
            return BigDecimal.valueOf((Long) obj);
        } else if (obj instanceof Integer) {
            return BigDecimal.valueOf((Integer) obj);
        } else {
            String tmp = super.get(name, String.class);
            return new BigDecimal(tmp);
        }
    }
}