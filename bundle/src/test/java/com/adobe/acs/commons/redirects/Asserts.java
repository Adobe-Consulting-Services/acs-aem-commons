package com.adobe.acs.commons.redirects;

import java.time.temporal.TemporalAccessor;

import static com.adobe.acs.commons.redirects.models.UpgradeLegacyRedirects.DATE_FORMATTER;
import static org.junit.Assert.assertEquals;

public class Asserts {

    // assert date truncated to days
    public static void assertDateEquals(String yyyymmMMMMdd, TemporalAccessor zdt) {
        assertEquals(yyyymmMMMMdd, DATE_FORMATTER.format(zdt));

    }
}
