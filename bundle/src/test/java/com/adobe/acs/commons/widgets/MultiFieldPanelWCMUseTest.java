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

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingBindings;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.script.Bindings;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(MultiFieldPanelFunctions.class)
public class MultiFieldPanelWCMUseTest {

    private String path = RandomStringUtils.randomAlphanumeric(10);

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

    @Mock
    private MultiFieldPanelFunctions multiFieldPanelFunctions;

    private MultiFieldPanelWCMUse multiFieldPanelWCMUse = new MultiFieldPanelWCMUse();


    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(MultiFieldPanelFunctions.class);
        when(bindings.get(SlingBindings.RESOURCE)).thenReturn(componentResource);
        when(componentResource.getResourceResolver()).thenReturn(resourceResolver);
    }

    @Test
    public void testNullPropertyName() throws Exception {
        when(bindings.get("location")).thenReturn(null);
        when(bindings.get("name")).thenReturn(null);

        multiFieldPanelWCMUse.init(bindings);
        List<Map<String, String>> actual = multiFieldPanelWCMUse.getValues();
        assertEquals(0, actual.size());
    }

    @Test
    public void testEmptyPropertyName() throws Exception {
        when(bindings.get("location")).thenReturn(null);
        when(bindings.get("name")).thenReturn("");

        multiFieldPanelWCMUse.init(bindings);
        List<Map<String, String>> actual = multiFieldPanelWCMUse.getValues();
        assertEquals(0, actual.size());
    }

    @Test
    public void testNotFoundPropertyName() throws Exception {
        when(bindings.get("location")).thenReturn(null);
        when(bindings.get("name")).thenReturn("notFoundPropertyName");

        multiFieldPanelWCMUse.init(bindings);
        List<Map<String, String>> actual = multiFieldPanelWCMUse.getValues();
        assertEquals(0, actual.size());
    }

    @Test
    public void testValidPropertyName() throws Exception {
        when(bindings.get("location")).thenReturn(null);
        when(bindings.get("name")).thenReturn("myProperty");
        BDDMockito
                .given(MultiFieldPanelFunctions.getMultiFieldPanelValues(componentResource, "myProperty"))
                .willReturn(myFirstList);

        multiFieldPanelWCMUse.init(bindings);

        List<Map<String, String>> actual = multiFieldPanelWCMUse.getValues();
        assertEquals(myFirstList, actual);
        assertNotEquals(mySecondList, actual);
    }

    @Test
    public void testValidResourceName() throws Exception {
        when(bindings.get("location")).thenReturn(parameterResource);
        when(bindings.get("name")).thenReturn("myProperty");
        BDDMockito
                .given(MultiFieldPanelFunctions.getMultiFieldPanelValues(parameterResource, "myProperty"))
                .willReturn(myFirstList);

        multiFieldPanelWCMUse.init(bindings);

        List<Map<String, String>> actual = multiFieldPanelWCMUse.getValues();
        assertEquals(myFirstList, actual);
        assertNotEquals(mySecondList, actual);
    }

    @Test
    public void testValidResourcePath() throws Exception {
        when(bindings.get("location")).thenReturn(path);
        when(bindings.get("name")).thenReturn("myProperty");
        when(resourceResolver.getResource(path)).thenReturn(parameterResource);
        BDDMockito
                .given(MultiFieldPanelFunctions.getMultiFieldPanelValues(parameterResource, "myProperty"))
                .willReturn(myFirstList);

        multiFieldPanelWCMUse.init(bindings);

        List<Map<String, String>> actual = multiFieldPanelWCMUse.getValues();
        assertEquals(myFirstList, actual);
        assertNotEquals(mySecondList, actual);
    }
}
