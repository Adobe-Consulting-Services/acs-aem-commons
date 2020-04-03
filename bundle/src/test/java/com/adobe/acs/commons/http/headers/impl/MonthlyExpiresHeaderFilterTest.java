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

import com.adobe.acs.commons.http.headers.impl.MonthlyExpiresHeaderFilter;

@RunWith(MockitoJUnitRunner.class)
public class MonthlyExpiresHeaderFilterTest {

    MonthlyExpiresHeaderFilter filter = new MonthlyExpiresHeaderFilter();

    Dictionary<String, Object> properties = null;

    @Mock
    ComponentContext componentContext;

    @Mock
    BundleContext bundleContext;

    @Before
    public void setup() throws Exception {
        properties = new Hashtable<String, Object>();
        properties.put(MonthlyExpiresHeaderFilter.PROP_EXPIRES_TIME, "02:30");

        when(componentContext.getProperties()).thenReturn(properties);
        when(componentContext.getBundleContext()).thenReturn(bundleContext);

    }

    @After
    public void tearDown() throws Exception {
        properties = null;
        reset(componentContext, bundleContext);
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
        properties.put(MonthlyExpiresHeaderFilter.PROP_EXPIRES_DAY_OF_MONTH, expected.get(Calendar.DAY_OF_MONTH));

        filter.doActivate(componentContext);
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
        properties.put(MonthlyExpiresHeaderFilter.PROP_EXPIRES_DAY_OF_MONTH, expected.get(Calendar.DAY_OF_MONTH));

        filter.doActivate(componentContext);
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
        properties.put(MonthlyExpiresHeaderFilter.PROP_EXPIRES_DAY_OF_MONTH, expected.get(Calendar.DAY_OF_MONTH));

        filter.doActivate(componentContext);
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
        properties.put(MonthlyExpiresHeaderFilter.PROP_EXPIRES_DAY_OF_MONTH, expected.get(Calendar.DAY_OF_MONTH));

        filter.doActivate(componentContext);
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
        properties.put(MonthlyExpiresHeaderFilter.PROP_EXPIRES_DAY_OF_MONTH, "LAST");

        filter.doActivate(componentContext);
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
        properties.put(MonthlyExpiresHeaderFilter.PROP_EXPIRES_DAY_OF_MONTH, "LAST");

        filter.doActivate(componentContext);
        filter.adjustExpires(actual);

        assertTrue(DateUtils.isSameInstant(expected, actual));
        assertEquals(month, actual.get(Calendar.MONTH));
    }

    @Test
    public void testDoActivateSuccess() throws Exception {

        properties.put(MonthlyExpiresHeaderFilter.PROP_EXPIRES_DAY_OF_MONTH, 15);

        Calendar actual = Calendar.getInstance();
        actual.set(Calendar.SECOND, 0);
        actual.set(Calendar.MILLISECOND, 0);
        actual.set(Calendar.HOUR_OF_DAY, 1);
        actual.set(Calendar.MINUTE, 29);

        filter.doActivate(componentContext);
        filter.adjustExpires(actual);

        assertEquals(15, actual.get(Calendar.DAY_OF_MONTH));
        // Abstract class will pass expires, this checks we aren't messing with
        // the time.
        assertEquals(1, actual.get(Calendar.HOUR_OF_DAY));
        assertEquals(29, actual.get(Calendar.MINUTE));
    }

    @Test(expected = ConfigurationException.class)
    public void testDoActivateCallsParent() throws Exception {

        properties.remove(MonthlyExpiresHeaderFilter.PROP_EXPIRES_TIME);
        filter.doActivate(componentContext);
    }

    @Test(expected = ConfigurationException.class)
    public void testDoActivateNoDayOfMonth() throws Exception {

        filter.doActivate(componentContext);
    }

    @Test(expected = ConfigurationException.class)
    public void testDoActivateInvalidLowDayOfMonth() throws Exception {
        properties.put(MonthlyExpiresHeaderFilter.PROP_EXPIRES_DAY_OF_MONTH, 0);
        filter.doActivate(componentContext);
    }

    @Test(expected = ConfigurationException.class)
    public void testDoActivateInvalidHighDayOfMonth() throws Exception {
        Calendar test = Calendar.getInstance();
        int val = test.getActualMaximum(Calendar.DAY_OF_MONTH) + 1;
        properties.put(MonthlyExpiresHeaderFilter.PROP_EXPIRES_DAY_OF_MONTH, val);
        filter.doActivate(componentContext);
    }
}
