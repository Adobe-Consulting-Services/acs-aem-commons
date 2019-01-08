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

import org.apache.sling.api.resource.Resource;
import org.junit.Test;
import org.mockito.Answers;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PropertiesTest {

    @Test
    public void lastModified_null_returnNewDate() throws Exception {
        Date result = Properties.lastModified(null);
        assertNotNull(result);
    }

    @Test
    public void lastModified_hasProperty_Date() throws Exception {
        // given
        Resource resource = mock(Resource.class, Answers.RETURNS_DEEP_STUBS.get());
        Date d = new Date();
        when(resource.getValueMap().get(anyString(), any(Date.class))).thenReturn(d);

        // when
        Date result = Properties.lastModified(resource);

        // then
        assertNotNull(result);
        assertEquals(result, d);
    }
}