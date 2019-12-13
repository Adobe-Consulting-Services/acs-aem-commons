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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.adobe.acs.commons.widgets.MultiFieldPanelFunctions;

@RunWith(MockitoJUnitRunner.class)
public class MultiFieldPanelFunctionsTest {

    @Mock
    private Resource resource;

    @Before
    public void setUp() throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("single", "{\"a\":\"b\"}");
        map.put("multiple", new String[] { "{\"a\":\"b\"}", "{\"c\":\"d\"}" });
        ValueMap vm = new ValueMapDecorator(map);
        when(resource.adaptTo(ValueMap.class)).thenReturn(vm);
    }

    @Test
    public void testSingleObject() {
        List<Map<String, String>> actual = MultiFieldPanelFunctions.getMultiFieldPanelValues(resource, "single");
        assertEquals(1, actual.size());
        assertEquals(true, actual.get(0).containsKey("a"));
        assertEquals("b", actual.get(0).get("a"));
    }

    @Test
    public void testMultipleObject() {
        List<Map<String, String>> actual = MultiFieldPanelFunctions.getMultiFieldPanelValues(resource, "multiple");
        assertEquals(2, actual.size());
        assertEquals(true, actual.get(0).containsKey("a"));
        assertEquals("b", actual.get(0).get("a"));
        assertEquals(true, actual.get(1).containsKey("c"));
        assertEquals("d", actual.get(1).get("c"));
    }

    @Test
    public void testKeyWhichDoesntExist() {
        List<Map<String, String>> actual = MultiFieldPanelFunctions.getMultiFieldPanelValues(resource,
                "non-existing");
        assertEquals(0, actual.size());
    }

    @Test
    public void testResourceAdaptToValueMapNull() {
        when(resource.adaptTo(ValueMap.class)).thenReturn(null);
        List<Map<String, String>> actual = MultiFieldPanelFunctions.getMultiFieldPanelValues(resource, "single");
        assertEquals(0, actual.size());
    }
}
