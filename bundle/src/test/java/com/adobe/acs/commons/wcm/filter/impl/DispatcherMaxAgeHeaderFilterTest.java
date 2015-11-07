/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2015 Adobe
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
package com.adobe.acs.commons.wcm.filter.impl;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Dictionary;
import java.util.Hashtable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;

@RunWith(MockitoJUnitRunner.class)
public class DispatcherMaxAgeHeaderFilterTest {

    DispatcherMaxAgeHeaderFilter filter;

    Dictionary<String, Object> properties = null;

    private long maxage = 2000;
    
    @Mock
    ComponentContext componentContext;

    @Before
    public void setup() throws Exception {
        properties = new Hashtable<String, Object>();
        properties.put(DispatcherMaxAgeHeaderFilter.PROP_MAX_AGE, maxage);

        filter = new DispatcherMaxAgeHeaderFilter();
    }

    @After
    public void tearDown() throws Exception {
        properties = null;
        reset(componentContext);
    }

    @Test
    public void testGetHeaderName() {
        assertEquals(DispatcherMaxAgeHeaderFilter.CACHE_CONTROL_NAME, filter.getHeaderName());
    }

    @Test
    public void testGetHeaderValue() throws Exception {

        when(componentContext.getProperties()).thenReturn(properties);

        filter.doActivate(componentContext);
        assertEquals("max-age=" + maxage, filter.getHeaderValue());
    }

    @Test(expected=ConfigurationException.class)
    public void testActivateNoMaxAge() throws Exception {
        properties.remove(DispatcherMaxAgeHeaderFilter.PROP_MAX_AGE);
        when(componentContext.getProperties()).thenReturn(properties);
        filter.activate(componentContext);
    }

    @Test
    public void testDoActivateSuccess() throws Exception {

        when(componentContext.getProperties()).thenReturn(properties);

        filter.doActivate(componentContext);
        assertEquals("max-age=" + maxage, filter.getHeaderValue());
        verify(componentContext).getProperties();
        verifyNoMoreInteractions(componentContext);

    }

}
