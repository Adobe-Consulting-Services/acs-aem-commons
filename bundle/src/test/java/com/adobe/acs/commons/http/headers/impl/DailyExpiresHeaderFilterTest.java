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
public class DailyExpiresHeaderFilterTest {

    @Mock
    BundleContext bundleContext;

    DailyExpiresHeaderFilter createFilter() {
        Map<String, Object> props = new HashMap<>();
        props.put("filter.pattern", new String[] { "/content/dam/.*" });
        props.put("expires.time", "02:30");
        DailyExpiresHeaderFilter.Config config = Converters.standardConverter().convert(props).to(DailyExpiresHeaderFilter.Config.class);
        return new DailyExpiresHeaderFilter(config, bundleContext);
    }

    @Test
    public void testAdjustExpiresPast() throws Exception {
        Calendar actual = Calendar.getInstance();
        actual.set(Calendar.SECOND, 0);
        actual.set(Calendar.MILLISECOND, 0);

        Calendar expected = Calendar.getInstance();
        expected.setTime(actual.getTime());
        expected.add(Calendar.DAY_OF_MONTH, 1);

        DailyExpiresHeaderFilter filter = createFilter();
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

        DailyExpiresHeaderFilter filter = createFilter();
        filter.adjustExpires(actual);

        assertTrue(DateUtils.isSameInstant(expected, actual));
    }

}
