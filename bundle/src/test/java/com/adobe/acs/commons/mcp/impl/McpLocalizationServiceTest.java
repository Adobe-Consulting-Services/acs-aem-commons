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
package com.adobe.acs.commons.mcp.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

public class McpLocalizationServiceTest {


    private McpLocalizationConfiguration config;
    private McpLocalizationServiceImpl impl;

    @Before
    public void init() throws Exception {
        config = mock(McpLocalizationConfiguration.class);
        impl = new McpLocalizationServiceImpl();
    }

    @Test
    public void givenLocalizationEnabledIsTrue_WhenGetterCalled_ThenTrueIsReturned() throws Exception {

        when(config.localizationEnabled()).thenReturn(true);
        impl.activate(config);

        boolean localizationEnabled = impl.isLocalizationEnabled();
        assertTrue(localizationEnabled);
    }

    @Test
    public void givenLocalizationEnabledIsFalse_WhenGetterCalled_ThenFalseIsReturned() throws Exception {

        when(config.localizationEnabled()).thenReturn(false);
        impl.activate(config);

        boolean localizationEnabled = impl.isLocalizationEnabled();
        assertFalse(localizationEnabled);
    }


    @Test
    public void givenOverlayedLangResPathIsValid_WhenGetterCalled_ThenSameValidPathIsReturned() throws Exception {

        String validOverlayedLangPath = "/apps/wcm/core/resources/languages";

        when(config.overlayedLanguagesResourcePath()).thenReturn(validOverlayedLangPath);
        impl.activate(config);

        String actualPath = impl.getOverlayedLanguagesResourcePath();
        assertEquals(validOverlayedLangPath, actualPath);
    }

    @Test
    public void givenOverlayedLangResPathIsInvalid_WhenGetterCalled_ThenNullIsReturned() throws Exception {

        String invalidOverlayedLangPath = "/libs/wcm/core/resources/languages";

        when(config.overlayedLanguagesResourcePath()).thenReturn(invalidOverlayedLangPath);
        impl.activate(config);

        String actualPath = impl.getOverlayedLanguagesResourcePath();
        assertNull(actualPath);
    }

 
 
    
}
