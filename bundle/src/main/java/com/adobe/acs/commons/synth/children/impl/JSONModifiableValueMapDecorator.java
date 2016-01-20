package com.adobe.acs.commons.synth.children.impl;

import org.apache.sling.api.wrappers.ModifiableValueMapDecorator;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Special wrapper to handle deserialization of JSON data to value map.
 *
 * Contains special handling for Date and Calendar interoperability.
 */
public final class JSONModifiableValueMapDecorator extends ModifiableValueMapDecorator {
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
     *
     * Special implementation of get to handle Date and Calendar interoperability.
     */
    @Override
    public <T> T get(String name, Class<T> type) {
        if (Calendar.class.equals(type)) {
            Object obj = super.get(name);

            if (obj instanceof Calendar) {
                return (T) obj;
            } else if (obj instanceof Date) {
                Calendar cal = Calendar.getInstance();
                cal.setTime((Date) obj);
                return (T) cal;
            } else {
                String tmp = super.get(name, String.class);
                final DateTime dateTime = ISODateTimeFormat.dateTime().parseDateTime(tmp);
                if (dateTime != null) {
                    final Calendar cal = Calendar.getInstance();
                    cal.setTime(dateTime.toDate());
                    return (T) cal;
                } else {
                    return null;
                }
            }
        } else if (Date.class.equals(type)) {
            Object obj = super.get(name);
            if (obj instanceof Date) {
                return (T) obj;
            } else if (obj instanceof Calendar) {
                Calendar cal = (Calendar) obj;
                return (T) cal.getTime();
            } else {
                String tmp = super.get(name, String.class);
                final DateTime dateTime = ISODateTimeFormat.dateTime().parseDateTime(tmp);
                if (dateTime != null) {
                    return (T) dateTime.toDate();
                } else {
                    return null;
                }
            }
        } else {
            return super.get(name, type);
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
}