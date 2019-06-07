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

import java.util.Calendar;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.adobe.acs.commons.http.headers.impl.DailyExpiresHeaderFilter;

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
