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
package com.adobe.acs.commons.wcm.comparisons.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public final class PropertiesTest {

    @Test
    public void lastModified_null_returnNewDate() {
        final Date result = Properties.lastModified(null);
        assertNotNull(result);
    }

    @Test
    public void lastModified_hasProperty_Date() {
        // given
        final Resource resource = mock(Resource.class);
        final ValueMap properties = mock(ValueMap.class);
        when(resource.getValueMap()).thenReturn(properties);
        final Date d = new Date();
        when(properties.get(anyString(), any(Date.class))).thenReturn(d);

        // when
        final Date result = Properties.lastModified(resource);

        // then
        assertNotNull(result);
        assertEquals(result, d);
    }
}