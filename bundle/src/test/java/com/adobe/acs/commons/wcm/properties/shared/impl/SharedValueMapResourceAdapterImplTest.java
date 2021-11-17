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
package com.adobe.acs.commons.wcm.properties.shared.impl;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertSame;

public class SharedValueMapResourceAdapterImplTest {

    @Test
    public void testConstructor_nullToEmpty() {
        final ValueMap otherEmpty = new ValueMapDecorator(new HashMap<>());
        assertSame("expect empty for global", ValueMap.EMPTY,
                new SharedValueMapResourceAdapterImpl(null, otherEmpty, otherEmpty).getGlobalProperties());
        assertSame("expect empty for shared", ValueMap.EMPTY,
                new SharedValueMapResourceAdapterImpl(otherEmpty, null, otherEmpty).getSharedProperties());
        assertSame("expect empty for merged", ValueMap.EMPTY,
                new SharedValueMapResourceAdapterImpl(otherEmpty, otherEmpty, null).getMergedProperties());

        assertSame("expect otherEmpty for global", otherEmpty,
                new SharedValueMapResourceAdapterImpl(otherEmpty, null, null).getGlobalProperties());
        assertSame("expect otherEmpty for shared", otherEmpty,
                new SharedValueMapResourceAdapterImpl(null, otherEmpty, null).getSharedProperties());
        assertSame("expect otherEmpty for merged", otherEmpty,
                new SharedValueMapResourceAdapterImpl(null, null, otherEmpty).getMergedProperties());
    }
}