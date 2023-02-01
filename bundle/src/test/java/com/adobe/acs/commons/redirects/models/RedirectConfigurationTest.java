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
package com.adobe.acs.commons.redirects.models;

import org.apache.sling.api.SlingHttpServletRequest;
import org.junit.Test;

import static com.adobe.acs.commons.redirects.models.RedirectConfiguration.determinePathToEvaluate;
import static com.adobe.acs.commons.redirects.models.RedirectConfiguration.normalizePath;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class RedirectConfigurationTest {

    @Test
    public void testNormalizePath(){
        assertEquals("/content/we-retail/en", normalizePath("/content/we-retail/en"));
        assertEquals("/content/we-retail/en", normalizePath("/content/we-retail/en.html"));
        assertEquals("/content/dam/we-retail/en.html", normalizePath("/content/dam/we-retail/en.html"));
        assertEquals("/content/dam/we-retail/en.pdf", normalizePath("/content/dam/we-retail/en.pdf"));
    }

    @Test
    public void testPathToEvaluate(){
        final String resourcePath = "/content/we-retail/en";
        final String expectedURI = "/content/we-retail/en.html/suffix.html";
        SlingHttpServletRequest mockRequest = mock(SlingHttpServletRequest.class);
        doReturn(expectedURI).when(mockRequest).getRequestURI();

        assertEquals(resourcePath, determinePathToEvaluate(resourcePath, false, null));
        assertEquals(resourcePath, determinePathToEvaluate(resourcePath, false, mockRequest));
        assertEquals(expectedURI, determinePathToEvaluate(resourcePath, true, mockRequest));
    }
}
