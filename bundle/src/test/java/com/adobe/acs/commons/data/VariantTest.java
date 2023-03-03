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
package com.adobe.acs.commons.data;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import org.junit.Test;

import static org.junit.Assert.*;

public class VariantTest {

    @Test
    public void numericConversion() {
        assert (Variant.convert("12345", Integer.class) instanceof Integer);
        assertEquals(12345L, (long) Variant.convert("12345", Integer.class));
        assert (Variant.convert("12345", Short.class) instanceof Short);
        assertEquals(12345L, (long) Variant.convert("12345", Short.class));
        assert (Variant.convert("12345.123", Long.class) instanceof Long);
        assertEquals(12345L, (long) Variant.convert("12345", Long.class));
        assert (Variant.convert("12345.123", Double.class) instanceof Double);
        assertEquals(12345.123, Variant.convert("12345.123", Double.class), 0.0000001);
        assert (Variant.convert("12345.123", Float.class) instanceof Float);
        assertEquals(12345.123, Variant.convert("12345.123", Float.class), 0.001);
        assert (Variant.convert(1, Boolean.class));
        assertFalse(Variant.convert("0", Boolean.class));
        assertEquals(1L, (long) Variant.convert(true, Long.class));
    }

    @Test
    public void booleanConversion() {
        assert (Variant.convert("1", Boolean.class));
        assertFalse(Variant.convert("0", Boolean.class));
        assert (Variant.convert(1.1234, Boolean.class));
        assertFalse(Variant.convert(0.0, Boolean.class));
        assert (Variant.convert("true", Boolean.class));
        assertFalse(Variant.convert("FaLsE", Boolean.class));
        assert (Variant.convert("tRuE", Boolean.class));
        assert (Variant.convert("y", Boolean.class));
        assertFalse(Variant.convert("n", Boolean.class));
    }

    @Test
    public void calendarConversion() {
        long now = System.currentTimeMillis();
        Date nowDate = new Date(now);
        Instant nowInstant = nowDate.toInstant();

        assertEquals(nowDate, Variant.convert(now, Date.class));
        assertEquals(nowInstant, Variant.convert(now, Instant.class));
        assertEquals(nowInstant, Variant.convert(nowDate, Instant.class));

        // Locale.getDefault() and Locale.getDefault(Locale.Category.FORMAT) may return different values in certain OS settings.
        // Variant uses the former, SimpleDateFormat uses the latter by default.
        // To make things consistent, pass Locale.getDefault() to SimpleDateFormat explicitly.
        String nowStringShort = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.LONG, Locale.getDefault()).format(nowDate);
        String nowStringLong = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.LONG, SimpleDateFormat.LONG, Locale.getDefault()).format(nowDate);
        assertNotNull(Variant.convert(nowStringLong, Date.class).getTime());
        assertNotNull(Variant.convert(nowStringShort, Date.class).getTime());
        assertNotNull(Variant.convert("12:00 AM", Date.class).getTime());
    }

    @Test
    public void databaseDateFormatConversion() {
        Calendar cal = Variant.convert("2016-02-12T14:47:41.922-05:00", Calendar.class);
        assertNotNull(cal);
        assertEquals(2016L, cal.get(Calendar.YEAR));
    }

    @Test
    public void emptyBehavior() {
        assertTrue((new Variant()).isEmpty());
        assertFalse(new Variant("some value").isEmpty());
        assertTrue((new CompositeVariant(String.class)).isEmpty());
        assertFalse(new CompositeVariant("some value").isEmpty());
        assertNull(Variant.convert("", String.class));
        assertNull(Variant.convert(null, String.class));
    }

    @Test
    public void unsupportedTypes() {
        assertNull(Variant.convert("Known type", Exception.class));
        assertNull(Variant.convert("Known type", VariantTest.class));
    }
}
