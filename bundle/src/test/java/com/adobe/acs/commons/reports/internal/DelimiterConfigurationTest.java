package com.adobe.acs.commons.reports.internal;

import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;

@RunWith(MockitoJUnitRunner.class)
public class DelimiterConfigurationTest {

    private static final String CUSTOM_FIELD_DELIM = "%FD%";
    private static final String CUSTOM_MULTI_VALUE_DELIM = "%MVD%";

    @Rule
    public SlingContext context = new SlingContext();

    @Mock
    private DelimiterConfiguration.Config config;

    private DelimiterConfiguration delimiterConfiguration;

    @Before
    public void setUp() {
        delimiterConfiguration = context.registerInjectActivateService(new DelimiterConfiguration());

        doReturn(CUSTOM_FIELD_DELIM).when(config).field_delimiter();
        doReturn(CUSTOM_MULTI_VALUE_DELIM).when(config).multi_value_delimiter();
    }

    @Test
    public void testDelimiterConfiguration() {
        assertEquals(DelimiterConfiguration.DEFAULT_FIELD_DELIMITER, delimiterConfiguration.getFieldDelimiter());
        assertEquals(DelimiterConfiguration.DEFAULT_MULTI_VALUE_DELIMITER, delimiterConfiguration.getMultiValueDelimiter());

        delimiterConfiguration.configurationModified(config);
        assertEquals(CUSTOM_FIELD_DELIM, delimiterConfiguration.getFieldDelimiter());
        assertEquals(CUSTOM_MULTI_VALUE_DELIM, delimiterConfiguration.getMultiValueDelimiter());
    }
}
