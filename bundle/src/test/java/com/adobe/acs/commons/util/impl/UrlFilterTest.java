/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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
package com.adobe.acs.commons.util.impl;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;

import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UrlFilterTest {
    private ValueMap properties;

    @Before
    public void setup() {
        properties = new ValueMapDecorator(new HashMap<String, Object>());
    }

    @Test
    public void null_selector() {
        UrlFilter filter = new UrlFilter();

        RequestPathInfo testInfo = mock(RequestPathInfo.class);
        when(testInfo.getSelectorString()).thenReturn(null);

        assertTrue(filter.checkSelector(testInfo, null));
    }

    @Test
    public void non_null_selector() {
        UrlFilter filter = new UrlFilter();

        RequestPathInfo testInfo = mock(RequestPathInfo.class);
        when(testInfo.getSelectorString()).thenReturn("sample");

        // null allowedSelectors = ok
        assertTrue(filter.checkSelector(testInfo, properties));

        // empty array allowedSelectors = fail
        properties.put(UrlFilter.PN_ALLOWED_SELECTORS, (Object) new String[0]);
        assertFalse(filter.checkSelector(testInfo, properties));

        // selector string in array = ok
        properties.put(UrlFilter.PN_ALLOWED_SELECTORS, (Object) new String[] { "sample", "sample2" });
        assertTrue(filter.checkSelector(testInfo, properties));

        // selector string not in array = fail
        properties.put(UrlFilter.PN_ALLOWED_SELECTORS, (Object) new String[] { "other" });
        assertFalse(filter.checkSelector(testInfo, properties));

        properties.clear();

        // matches regex
        properties.put(UrlFilter.PN_ALLOWED_SELECTOR_PATTERN, "^s[a-z]m.*$");
        assertTrue(filter.checkSelector(testInfo, properties));

        // doesn't match regex
        properties.put(UrlFilter.PN_ALLOWED_SELECTOR_PATTERN, "^s[1-2]m$");
        assertFalse(filter.checkSelector(testInfo, properties));

        properties.clear();

        // matches array or regex = ok
        properties.put(UrlFilter.PN_ALLOWED_SELECTORS, (Object) new String[] { "other" });
        properties.put(UrlFilter.PN_ALLOWED_SELECTOR_PATTERN, "^s[a-z]m.*$");
        assertTrue(filter.checkSelector(testInfo, properties));

        properties.put(UrlFilter.PN_ALLOWED_SELECTORS, (Object) new String[] { "sample" });
        properties.put(UrlFilter.PN_ALLOWED_SELECTOR_PATTERN, "^s[a-z]m$");
        assertTrue(filter.checkSelector(testInfo, properties));

    }
}
