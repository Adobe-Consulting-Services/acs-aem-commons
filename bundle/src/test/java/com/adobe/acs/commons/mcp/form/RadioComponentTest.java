/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2018 Adobe
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
package com.adobe.acs.commons.mcp.form;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class RadioComponentTest {

    @Mock
    private FormField formField;

    @Mock
    private SlingScriptHelper sling;

    @Mock
    private SlingHttpServletRequest request;

    @Captor
    private ArgumentCaptor<Resource> resourceCaptor;

    private Field javaField;

    @Before
    public void setup() throws NoSuchFieldException {
        when(sling.getRequest()).thenReturn(request);
        javaField = TestFieldContainer.class.getField("field");
    }

    @Test
    public void testRadioComponentDoesntSetFieldLabelOrDescription() {
        when(formField.options()).thenReturn(new String[0]);
        when(formField.name()).thenReturn("NAME");
        when(formField.description()).thenReturn("DESCRIPTION");

        RadioComponent cmp = new RadioComponent() {
            @Override
            public Map<String, String> getOptions() {
                return new HashMap<String, String>() { {
                    put("v1", "t1");
                    put("v2", "t2");
                    put("v3", "t3");
                }
            };
            }
        };
        cmp.setup("test", null, formField, sling);

        Resource r = cmp.buildComponentResource();
        ValueMap map = r.getValueMap();
        assertEquals("NAME", map.get("text"));
        assertFalse(map.containsKey("fieldLabel"));
        assertFalse(map.containsKey("fieldDescription"));

    }

    @Test
    public void testEnumWithoutDefault() {
        RadioComponent.EnumerationSelector radioComponent = new RadioComponent.EnumerationSelector();
        radioComponent.setup("testRadio", javaField, formField, sling);

        radioComponent.getHtml();

        verify(sling, times(1)).include(resourceCaptor.capture());
        Resource resource = resourceCaptor.getValue();

        List<String> options = StreamSupport.stream(resource.getChild("items").getChildren().spliterator(), false).map(r -> {
            ValueMap props = r.getValueMap();
            return String.format("%s:%s:%s", props.get("text"), props.get("value"), props.get("selected", false));
        }).collect(Collectors.toList());

        assertEquals(Arrays.asList(
            "First:first:false",
            "Second:second:false",
            "Third:third:false"
        ), options);
    }

    private class TestFieldContainer {
        public TestEnum field;
    }

    private enum TestEnum {
        first,
        second,
        third
    }

}
