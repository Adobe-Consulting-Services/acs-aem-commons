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
package com.adobe.acs.commons.http.headers.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.util.converter.Converters;

@RunWith(MockitoJUnitRunner.class)
public class MonthlyExpiresHeaderFilterTest {

    @Mock
    BundleContext bundleContext;

    MonthlyExpiresHeaderFilter createFilterWithDayOfMonth(int dayOfMonth) {
        return createFilter(Integer.toString(dayOfMonth));
    }

    MonthlyExpiresHeaderFilter createFilter(String dayOfMonth) {
        Map<String, Object> props = new HashMap<>();
        props.put("filter.pattern", new String[] { "/content/dam/.*" });
        props.put("expires.time", "02:30");
        props.put("expires.day-of-month", dayOfMonth);
        MonthlyExpiresHeaderFilter.Config config = Converters.standardConverter().convert(props).to(MonthlyExpiresHeaderFilter.Config.class);
        return new MonthlyExpiresHeaderFilter(config, bundleContext);
    }

    @Test
    public void testAdjustExpiresPastDay() throws Exception {
        Calendar actual = Calendar.getInstance();
        actual.set(Calendar.SECOND, 0);
        actual.set(Calendar.MILLISECOND, 0);

        Calendar expected = Calendar.getInstance();
        expected.setTime(actual.getTime());
        expected.add(Calendar.DAY_OF_MONTH, -3);
        expected.add(Calendar.MONTH, 1);

        actual.set(Calendar.DAY_OF_MONTH, 15);

        final int month = expected.get(Calendar.MONTH);

        MonthlyExpiresHeaderFilter filter = createFilterWithDayOfMonth(expected.get(Calendar.DAY_OF_MONTH));
        filter.adjustExpires(actual);

        assertTrue(DateUtils.isSameInstant(expected, actual));
        assertEquals(month, actual.get(Calendar.MONTH));
    }

    @Test
    public void testAdjustExpiresSameDayPast() throws Exception {

        Calendar actual = Calendar.getInstance();
        actual.set(Calendar.SECOND, 0);
        actual.set(Calendar.MILLISECOND, 0);

        Calendar expected = Calendar.getInstance();
        expected.setTime(actual.getTime());
        expected.add(Calendar.MONTH, 1);

        final int month = expected.get(Calendar.MONTH);

        MonthlyExpiresHeaderFilter filter = createFilterWithDayOfMonth(expected.get(Calendar.DAY_OF_MONTH));
        filter.adjustExpires(actual);

        assertTrue(DateUtils.isSameInstant(expected, actual));
        assertEquals(month, actual.get(Calendar.MONTH));
    }

    @Test
    public void testAdjustExpiresSameDayFuture() throws Exception {

        Calendar actual = Calendar.getInstance();
        actual.add(Calendar.MINUTE, 1);
        actual.set(Calendar.SECOND, 0);
        actual.set(Calendar.MILLISECOND, 0);

        Calendar expected = Calendar.getInstance();
        expected.setTime(actual.getTime());

        final int month = expected.get(Calendar.MONTH);

        MonthlyExpiresHeaderFilter filter = createFilterWithDayOfMonth(expected.get(Calendar.DAY_OF_MONTH));
        filter.adjustExpires(actual);

        assertTrue(DateUtils.isSameInstant(expected, actual));
        assertEquals(month, actual.get(Calendar.MONTH));
    }

    @Test
    public void testAdjustExpiresFutureDay() throws Exception {

        Calendar actual = Calendar.getInstance();
        actual.set(Calendar.SECOND, 0);
        actual.set(Calendar.MILLISECOND, 0);

        Calendar expected = Calendar.getInstance();
        expected.setTime(actual.getTime());
        expected.add(Calendar.DAY_OF_MONTH, 5);

        actual.set(Calendar.DAY_OF_MONTH, 15);

        final int month = expected.get(Calendar.MONTH);

        MonthlyExpiresHeaderFilter filter = createFilterWithDayOfMonth(expected.get(Calendar.DAY_OF_MONTH));
        filter.adjustExpires(actual);

        assertTrue(DateUtils.isSameInstant(expected, actual));
        assertEquals(month, actual.get(Calendar.MONTH));
    }

    @Test
    public void testAdjustExpiresLastDayPast() throws Exception {

        Calendar actual = Calendar.getInstance();
        actual.add(Calendar.MONTH, -1);
        actual.set(Calendar.SECOND, 0);
        actual.set(Calendar.MILLISECOND, 0);
        actual.set(Calendar.DAY_OF_MONTH, actual.getActualMaximum(Calendar.DAY_OF_MONTH));

        Calendar expected = Calendar.getInstance();
        expected.setTime(actual.getTime());
        expected.add(Calendar.MONTH, 1);

        final int month = expected.get(Calendar.MONTH);

        MonthlyExpiresHeaderFilter filter = createFilter("LAST");
        filter.adjustExpires(actual);

        assertTrue(DateUtils.isSameInstant(expected, actual));
        assertEquals(month, actual.get(Calendar.MONTH));
    }

    @Test
    public void testAdjustExpiresLastDayFuture() throws Exception {

        Calendar actual = Calendar.getInstance();
        actual.add(Calendar.MINUTE, 1);
        actual.set(Calendar.SECOND, 0);
        actual.set(Calendar.MILLISECOND, 0);
        actual.set(Calendar.DAY_OF_MONTH, actual.getActualMaximum(Calendar.DAY_OF_MONTH));

        Calendar expected = Calendar.getInstance();
        expected.setTime(actual.getTime());

        final int month = expected.get(Calendar.MONTH);

        MonthlyExpiresHeaderFilter filter = createFilter("LAST");
        filter.adjustExpires(actual);

        assertTrue(DateUtils.isSameInstant(expected, actual));
        assertEquals(month, actual.get(Calendar.MONTH));
    }

    @Test
    public void testDoActivateSuccess() throws Exception {

        Calendar actual = Calendar.getInstance();
        actual.set(Calendar.SECOND, 0);
        actual.set(Calendar.MILLISECOND, 0);
        actual.set(Calendar.HOUR_OF_DAY, 1);
        actual.set(Calendar.MINUTE, 29);

        MonthlyExpiresHeaderFilter filter = createFilterWithDayOfMonth(15);
        filter.adjustExpires(actual);

        assertEquals(15, actual.get(Calendar.DAY_OF_MONTH));
        // Abstract class will pass expires, this checks we aren't messing with
        // the time.
        assertEquals(1, actual.get(Calendar.HOUR_OF_DAY));
        assertEquals(29, actual.get(Calendar.MINUTE));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDoActivateInvalidLowDayOfMonth() throws Exception {
        createFilterWithDayOfMonth(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDoActivateInvalidHighDayOfMonth() throws Exception {
        Calendar test = Calendar.getInstance();
        int val = test.getActualMaximum(Calendar.DAY_OF_MONTH) + 1;
        createFilterWithDayOfMonth(val);
    }
}
