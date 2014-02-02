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

package com.adobe.acs.commons.multipanelfield;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MultiPanelFieldFunctionsTest {


    @Before
    public void setUp() throws Exception {
      
       
    }

    @Test
    public void testGetMultiPanelFieldValues() throws Exception {
    	Resource resource = mock(Resource.class);
    	ValueMap valueMap = mock(ValueMap.class);
    	 when(resource.adaptTo(ValueMap.class)).thenReturn(valueMap);
    	 when(valueMap.containsKey("columns")).thenReturn(true);
    	 when(valueMap.containsKey("columns1")).thenReturn(false);
    	 when(valueMap.containsKey("columns2")).thenReturn(true);
    	 when(valueMap.get("columns",new String[0])).thenReturn(new String[]{"{\"a\":\"b\"}"});
    	 when(valueMap.get("columns2",new String[0])).thenReturn(new String[]{"a=b"});
    	 List<Map<String, String>> actual = MultiPanelFieldFunctions.getMultiPanelFieldValues(resource, "columns");
    	 assertEquals(1, actual.size());
    	 assertEquals(true, actual.get(0).containsKey("a"));
    	 assertEquals("b", actual.get(0).get("a"));
    	 actual = MultiPanelFieldFunctions.getMultiPanelFieldValues(resource, "columns1");
    	 assertEquals(true, actual==null);
    	 actual = MultiPanelFieldFunctions.getMultiPanelFieldValues(resource, "columns2");
    	 assertEquals(true, actual==null);
    }
}
