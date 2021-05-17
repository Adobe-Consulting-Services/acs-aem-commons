/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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
package com.adobe.acs.commons.redirects;

import java.time.temporal.TemporalAccessor;

import static com.adobe.acs.commons.redirects.models.UpgradeLegacyRedirects.DATE_FORMATTER;
import static org.junit.Assert.assertEquals;

public class Asserts {

    /**
     * assert date truncated to days
     *
     * @param expected  date in the 'dd MMMM yyyy' format, e.g. 16 February 2022
     * @param zdt   the time to assert
     */
    public static void assertDateEquals(String expected, TemporalAccessor zdt) {
        assertEquals(expected, DATE_FORMATTER.format(zdt));

    }
}
