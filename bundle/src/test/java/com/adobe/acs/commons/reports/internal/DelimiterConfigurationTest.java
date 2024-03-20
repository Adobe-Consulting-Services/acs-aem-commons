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
