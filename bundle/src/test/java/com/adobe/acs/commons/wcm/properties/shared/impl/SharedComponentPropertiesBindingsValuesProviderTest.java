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
package com.adobe.acs.commons.wcm.properties.shared.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import javax.script.Bindings;
import javax.script.SimpleBindings;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.adobe.acs.commons.testing.PrivateAccessor;
import com.adobe.acs.commons.wcm.properties.shared.SharedComponentProperties;

@RunWith(MockitoJUnitRunner.class)
public class SharedComponentPropertiesBindingsValuesProviderTest {

    private Resource resource;
    private SlingHttpServletRequest request;
    private Bindings bindings;
    private SharedComponentProperties sharedComponentProperties;

    @Before
    public void setUp() {

        resource = mock(Resource.class);
        bindings = new SimpleBindings();
        request = mock(SlingHttpServletRequest.class);
        sharedComponentProperties = mock(SharedComponentProperties.class);

        bindings.put(SlingBindings.REQUEST, request);
        bindings.put(SlingBindings.RESOURCE, resource);

        when(resource.getValueMap()).thenReturn(ValueMap.EMPTY);
    }

    @Test
    public void testDisabled() throws Exception {

        SharedComponentPropertiesBindingsValuesProvider provider =
                new SharedComponentPropertiesBindingsValuesProvider();

        PrivateAccessor.setField(provider, "sharedComponentProperties", sharedComponentProperties);
        activate(provider, false);

        provider.addBindings(bindings);

        verifyNoInteractions(sharedComponentProperties);
        assertEquals(ValueMap.EMPTY,
                bindings.get(SharedComponentProperties.GLOBAL_PROPERTIES));
    }

    @Test
    public void testEnabled() throws Exception {

        SharedComponentPropertiesBindingsValuesProvider provider =
                new SharedComponentPropertiesBindingsValuesProvider();

        PrivateAccessor.setField(provider, "sharedComponentProperties", sharedComponentProperties);
        activate(provider, true);

        provider.addBindings(bindings);

        verify(sharedComponentProperties).getSharedPropertiesPagePath(resource);
        assertNotNull(bindings.get(SharedComponentProperties.GLOBAL_PROPERTIES));
    }

    private void activate(
            SharedComponentPropertiesBindingsValuesProvider provider,
            boolean enabled) {

        SharedComponentPropertiesBindingsValuesProvider.Config config =
                mock(SharedComponentPropertiesBindingsValuesProvider.Config.class);

        when(config.enabled()).thenReturn(enabled);

        provider.activate(config);
    }

}
