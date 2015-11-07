package com.adobe.acs.commons.wcm.filter.impl;

import static org.junit.Assert.*;

import java.util.Calendar;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DailyExpiresHeaderFilterTest {

    DailyExpiresHeaderFilter filter = new DailyExpiresHeaderFilter();

    @Test
    public void testAdjustExpiresPast() throws Exception {


        Calendar actual = Calendar.getInstance();
        actual.set(Calendar.SECOND, 0);
        actual.set(Calendar.MILLISECOND, 0);

        Calendar expected = Calendar.getInstance();
        expected.setTime(actual.getTime());
        expected.add(Calendar.DAY_OF_MONTH, 1);

        filter.adjustExpires(actual);

        assertTrue(DateUtils.isSameInstant(expected, actual));
    }

    @Test
    public void testAdjustExpiresFuture() throws Exception {


        Calendar actual = Calendar.getInstance();
        actual.add(Calendar.MINUTE, 1);
        actual.set(Calendar.SECOND, 0);
        actual.set(Calendar.MILLISECOND, 0);

        Calendar expected = Calendar.getInstance();
        expected.setTime(actual.getTime());

        filter.adjustExpires(actual);

        assertTrue(DateUtils.isSameInstant(expected, actual));
    }

}
