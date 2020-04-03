/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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

package com.adobe.acs.commons.widgets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import javax.script.Bindings;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingBindings;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public final class MultiFieldPanelWCMUseTest {

    private final String path = RandomStringUtils.randomAlphanumeric(10);

    private final MultiFieldPanelWCMUse multiFieldPanelWCMUse = spy(new MultiFieldPanelWCMUse());

    @Mock
    private Bindings bindings;

    @Mock
    private Resource componentResource;

    @Mock
    private Resource parameterResource;

    @Mock
    private ResourceResolver resourceResolver;

    @Mock
    private List<Map<String, String>> myFirstList;

    @Mock
    private List<Map<String, String>> mySecondList;


    @Before
    public void setUp() {
        when(bindings.get(SlingBindings.RESOURCE)).thenReturn(componentResource);
        when(componentResource.getResourceResolver()).thenReturn(resourceResolver);
    }

    @Test
    public void testNullPropertyName() {
        when(bindings.get("location")).thenReturn(null);
        when(bindings.get("name")).thenReturn(null);

        multiFieldPanelWCMUse.init(bindings);
        final List<Map<String, String>> actual = multiFieldPanelWCMUse.getValues();
        assertEquals(0, actual.size());
    }

    @Test
    public void testEmptyPropertyName() {
        when(bindings.get("location")).thenReturn(null);
        when(bindings.get("name")).thenReturn("");

        multiFieldPanelWCMUse.init(bindings);
        final List<Map<String, String>> actual = multiFieldPanelWCMUse.getValues();
        assertEquals(0, actual.size());
    }

    @Test
    public void testNotFoundPropertyName() {
        when(bindings.get("location")).thenReturn(null);
        when(bindings.get("name")).thenReturn("notFoundPropertyName");

        multiFieldPanelWCMUse.init(bindings);
        final List<Map<String, String>> actual = multiFieldPanelWCMUse.getValues();
        assertEquals(0, actual.size());
    }

    @Test
    public void testValidPropertyName() {
        when(bindings.get("location")).thenReturn(null);
        when(bindings.get("name")).thenReturn("myProperty");
        doReturn(myFirstList).when(multiFieldPanelWCMUse)
            .getMultiFieldPanelValues(componentResource, "myProperty");

        multiFieldPanelWCMUse.init(bindings);

        final List<Map<String, String>> actual = multiFieldPanelWCMUse.getValues();
        assertEquals(myFirstList, actual);
        assertNotEquals(mySecondList, actual);
    }

    @Test
    public void testValidResourceName() {
        when(bindings.get("location")).thenReturn(parameterResource);
        when(bindings.get("name")).thenReturn("myProperty");
        doReturn(myFirstList).when(multiFieldPanelWCMUse)
            .getMultiFieldPanelValues(parameterResource, "myProperty");

        multiFieldPanelWCMUse.init(bindings);

        final List<Map<String, String>> actual = multiFieldPanelWCMUse.getValues();
        assertEquals(myFirstList, actual);
        assertNotEquals(mySecondList, actual);
    }

    @Test
    public void testValidResourcePath() {
        when(bindings.get("location")).thenReturn(path);
        when(bindings.get("name")).thenReturn("myProperty");
        when(resourceResolver.getResource(path)).thenReturn(parameterResource);
        doReturn(myFirstList).when(multiFieldPanelWCMUse)
            .getMultiFieldPanelValues(parameterResource, "myProperty");

        multiFieldPanelWCMUse.init(bindings);

        final List<Map<String, String>> actual = multiFieldPanelWCMUse.getValues();
        assertEquals(myFirstList, actual);
        assertNotEquals(mySecondList, actual);
    }
}
