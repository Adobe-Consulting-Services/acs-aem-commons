/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2015 Adobe
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
package com.adobe.acs.commons.http.headers.impl;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Calendar;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;

import com.adobe.acs.commons.http.headers.impl.WeeklyExpiresHeaderFilter;

@RunWith(MockitoJUnitRunner.class)
public class WeeklyExpiresHeaderFilterTest {

    WeeklyExpiresHeaderFilter filter = new WeeklyExpiresHeaderFilter();

    Dictionary<String, Object> properties = null;

    @Mock
    ComponentContext componentContext;

    @Mock
    BundleContext bundleContext;

    @Before
    public void setup() throws Exception {
        properties = new Hashtable<String, Object>();
        properties.put(WeeklyExpiresHeaderFilter.PROP_EXPIRES_TIME, "02:30");

        when(componentContext.getProperties()).thenReturn(properties);
        when(componentContext.getBundleContext()).thenReturn(bundleContext);

    }

    @After
    public void tearDown() throws Exception {
        properties = null;
        reset(componentContext, bundleContext);
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
        properties.put(WeeklyExpiresHeaderFilter.PROP_EXPIRES_DAY_OF_WEEK, dayOfWeek);

        filter.doActivate(componentContext);
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
        properties.put(WeeklyExpiresHeaderFilter.PROP_EXPIRES_DAY_OF_WEEK, dayOfWeek);

        filter.doActivate(componentContext);
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
        properties.put(WeeklyExpiresHeaderFilter.PROP_EXPIRES_DAY_OF_WEEK, dayOfWeek);

        filter.doActivate(componentContext);
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

        int dayOfweek = expected.get(Calendar.DAY_OF_WEEK);
        properties.put(WeeklyExpiresHeaderFilter.PROP_EXPIRES_DAY_OF_WEEK, dayOfweek);


        filter.doActivate(componentContext);
        filter.adjustExpires(actual);

        assertTrue(DateUtils.isSameInstant(expected, actual));
        assertEquals(dayOfweek, actual.get(Calendar.DAY_OF_WEEK));
    }


    @Test
    public void testDoActivateSuccess() throws Exception {

        properties.put(WeeklyExpiresHeaderFilter.PROP_EXPIRES_DAY_OF_WEEK, Calendar.SUNDAY);

        Calendar actual = Calendar.getInstance();
        actual.set(Calendar.SECOND, 0);
        actual.set(Calendar.MILLISECOND, 0);
        actual.set(Calendar.HOUR_OF_DAY, 1);
        actual.set(Calendar.MINUTE, 29);

        filter.doActivate(componentContext);
        filter.adjustExpires(actual);

        assertEquals(Calendar.SUNDAY, actual.get(Calendar.DAY_OF_WEEK));
        // Abstract class will pass expires, this checks we aren't messing with the time.
        assertEquals(1, actual.get(Calendar.HOUR_OF_DAY));
        assertEquals(29, actual.get(Calendar.MINUTE));
    }

    @Test(expected = ConfigurationException.class)
    public void testDoActivateCallsParent() throws Exception {

        properties.remove(WeeklyExpiresHeaderFilter.PROP_EXPIRES_TIME);
        filter.doActivate(componentContext);
    }

    @Test(expected = ConfigurationException.class)
    public void testDoActivateNoDayOfWeek() throws Exception {

        filter.doActivate(componentContext);
    }

    @Test(expected = ConfigurationException.class)
    public void testDoActivateInvalidLowDayOfWeek() throws Exception {
        properties.put(WeeklyExpiresHeaderFilter.PROP_EXPIRES_DAY_OF_WEEK, Calendar.SUNDAY - 1);
        filter.doActivate(componentContext);
    }

    @Test(expected = ConfigurationException.class)
    public void testDoActivateInvalidHighDayOfWeek() throws Exception {
        properties.put(WeeklyExpiresHeaderFilter.PROP_EXPIRES_DAY_OF_WEEK, Calendar.SATURDAY + 1);
        filter.doActivate(componentContext);
    }
}
