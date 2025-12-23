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
public class WeeklyExpiresHeaderFilterTest {

    @Mock
    BundleContext bundleContext;

    WeeklyExpiresHeaderFilter createFilter(int dayOfWeek) {
        Map<String, Object> props = new HashMap<>();
        props.put("filter.pattern", new String[] { "/content/dam/.*" });
        props.put("expires.time", "02:30");
        props.put("expires.day-of-week", dayOfWeek);
        WeeklyExpiresHeaderFilter.Config config = Converters.standardConverter().convert(props).to(WeeklyExpiresHeaderFilter.Config.class);
        return new WeeklyExpiresHeaderFilter(config, bundleContext);
    }

    @Test
    public void testAdjustExpiresPastWeekday() throws Exception {
        Calendar actual = Calendar.getInstance();
        actual.set(Calendar.SECOND, 0);
        actual.set(Calendar.MILLISECOND, 0);

        Calendar expected = Calendar.getInstance();
        expected.setTime(actual.getTime());
        expected.add(Calendar.DAY_OF_WEEK, -3);
        expected.add(Calendar.DAY_OF_WEEK, 7);

        int dayOfWeek = expected.get(Calendar.DAY_OF_WEEK);
        WeeklyExpiresHeaderFilter filter = createFilter(dayOfWeek);
        filter.adjustExpires(actual);

        assertTrue(DateUtils.isSameInstant(expected, actual));
        assertEquals(dayOfWeek, actual.get(Calendar.DAY_OF_WEEK));
    }

    @Test
    public void testAdjustExpiresSameDayPast() throws Exception {
        Calendar actual = Calendar.getInstance();
        actual.set(Calendar.SECOND, 0);
        actual.set(Calendar.MILLISECOND, 0);

        Calendar expected = Calendar.getInstance();
        expected.setTime(actual.getTime());
        expected.add(Calendar.DAY_OF_WEEK, 7);

        int dayOfWeek = expected.get(Calendar.DAY_OF_WEEK);
        WeeklyExpiresHeaderFilter filter = createFilter(dayOfWeek);
        filter.adjustExpires(actual);

        assertTrue(DateUtils.isSameInstant(expected, actual));
        assertEquals(dayOfWeek, actual.get(Calendar.DAY_OF_WEEK));
    }

    @Test
    public void testAdjustExpiresSameDayFuture() throws Exception {
        Calendar actual = Calendar.getInstance();
        actual.add(Calendar.MINUTE, 1);
        actual.set(Calendar.SECOND, 0);
        actual.set(Calendar.MILLISECOND, 0);

        Calendar expected = Calendar.getInstance();
        expected.setTime(actual.getTime());
        int dayOfWeek = expected.get(Calendar.DAY_OF_WEEK);
        WeeklyExpiresHeaderFilter filter = createFilter(dayOfWeek);
        filter.adjustExpires(actual);

        assertTrue(DateUtils.isSameInstant(expected, actual));
        assertEquals(dayOfWeek, actual.get(Calendar.DAY_OF_WEEK));
    }

    @Test
    public void testAdjustExpiresFutureWeekday() throws Exception {
        Calendar actual = Calendar.getInstance();
        actual.set(Calendar.SECOND, 0);
        actual.set(Calendar.MILLISECOND, 0);

        Calendar expected = Calendar.getInstance();
        expected.setTime(actual.getTime());
        expected.add(Calendar.DAY_OF_WEEK, 2);

        int dayOfWeek = expected.get(Calendar.DAY_OF_WEEK);
        WeeklyExpiresHeaderFilter filter = createFilter(dayOfWeek);
        filter.adjustExpires(actual);

        assertTrue(DateUtils.isSameInstant(expected, actual));
        assertEquals(dayOfWeek, actual.get(Calendar.DAY_OF_WEEK));
    }

    @Test
    public void testDoActivateSuccess() throws Exception {
        Calendar actual = Calendar.getInstance();
        actual.set(Calendar.SECOND, 0);
        actual.set(Calendar.MILLISECOND, 0);
        actual.set(Calendar.HOUR_OF_DAY, 1);
        actual.set(Calendar.MINUTE, 29);

        WeeklyExpiresHeaderFilter filter = createFilter(Calendar.SUNDAY);
        filter.adjustExpires(actual);

        assertEquals(Calendar.SUNDAY, actual.get(Calendar.DAY_OF_WEEK));
        // Abstract class will pass expires, this checks we aren't messing with the time.
        assertEquals(1, actual.get(Calendar.HOUR_OF_DAY));
        assertEquals(29, actual.get(Calendar.MINUTE));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDoActivateInvalidLowDayOfWeek() throws Exception {
        createFilter(Calendar.SUNDAY - 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDoActivateInvalidHighDayOfWeek() throws Exception {
        createFilter(Calendar.SATURDAY + 1);
    }
}
