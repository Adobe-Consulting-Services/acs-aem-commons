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

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ MultiFieldPanelFunctions.class, MultiFieldPanelWCMUse.class })
public class MultiFieldPanelWCMUseTest {

    @Mock
    private Resource componentResource;
    @Mock
    private Resource parameterResource;

    @Mock
    private List<Map<String, String>> myFirstList;
    @Mock
    private List<Map<String, String>> mySecondList;

    @Mock
    private MultiFieldPanelFunctions multiFieldPanelFunctions;

    @Spy
    private MultiFieldPanelWCMUse multiFieldPanelWCMUse = new MultiFieldPanelWCMUse();


    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(MultiFieldPanelFunctions.class);
        doReturn(componentResource).when(multiFieldPanelWCMUse).getResource();
    }

    @Test
    public void testNullPropertyName() throws Exception {
        doReturn(null).when(multiFieldPanelWCMUse).get("location", Resource.class);
        doReturn(null).when(multiFieldPanelWCMUse).get("name", String.class);

        multiFieldPanelWCMUse.activate();
        List<Map<String, String>> actual = multiFieldPanelWCMUse.getValues();
        assertEquals(0, actual.size());
    }

    @Test
    public void testEmptyPropertyName() throws Exception {
        doReturn(null).when(multiFieldPanelWCMUse).get("location", Resource.class);
        doReturn("").when(multiFieldPanelWCMUse).get("name", String.class);

        multiFieldPanelWCMUse.activate();
        List<Map<String, String>> actual = multiFieldPanelWCMUse.getValues();
        assertEquals(0, actual.size());
    }

    @Test
    public void testNotFoundPropertyName() throws Exception {
        doReturn(null).when(multiFieldPanelWCMUse).get("location", Resource.class);
        doReturn("notFoundPropertyName").when(multiFieldPanelWCMUse).get("name", String.class);

        multiFieldPanelWCMUse.activate();
        List<Map<String, String>> actual = multiFieldPanelWCMUse.getValues();
        assertEquals(0, actual.size());
    }

    @Test
    public void testValidPropertyName() throws Exception {
        doReturn(null).when(multiFieldPanelWCMUse).get("location", Resource.class);
        doReturn("myProperty").when(multiFieldPanelWCMUse).get("name", String.class);
        BDDMockito
                .given(MultiFieldPanelFunctions.getMultiFieldPanelValues(componentResource, "myProperty"))
                .willReturn(myFirstList);

        multiFieldPanelWCMUse.activate();

        List<Map<String, String>> actual = multiFieldPanelWCMUse.getValues();
        assertEquals(myFirstList, actual);
        assertNotEquals(mySecondList, actual);
    }

    @Test
    public void testValidResourceName() throws Exception {
        doReturn(parameterResource).when(multiFieldPanelWCMUse).get("location", Resource.class);
        doReturn("myProperty").when(multiFieldPanelWCMUse).get("name", String.class);
        BDDMockito
                .given(MultiFieldPanelFunctions.getMultiFieldPanelValues(parameterResource, "myProperty"))
                .willReturn(myFirstList);

        multiFieldPanelWCMUse.activate();

        List<Map<String, String>> actual = multiFieldPanelWCMUse.getValues();
        assertEquals(myFirstList, actual);
        assertNotEquals(mySecondList, actual);
    }
}
