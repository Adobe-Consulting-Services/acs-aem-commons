/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2019 Adobe
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
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@RunWith(MockitoJUnitRunner.class)
public class SelectComponentTest {

    @Mock
    private SlingScriptHelper sling;

    @Mock
    private SlingHttpServletRequest request;

    @Captor
    private ArgumentCaptor<Resource> resourceCaptor;

    private Field javaField;

    @Mock
    private FormField formField;

    @Before
    public void setup() throws NoSuchFieldException {
        when(sling.getRequest()).thenReturn(request);
        javaField = TestFieldContainer.class.getField("field");

        when(formField.category()).thenReturn("some-category");
    }

    @Test
    public void testWithoutDefault() {
        SelectComponent.EnumerationSelector selectComponent = new SelectComponent.EnumerationSelector();
        selectComponent.setup("testSelect", javaField, formField, sling);

        selectComponent.getHtml();

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

    @Test
    public void testWithDefault() {
        when(formField.options()).thenReturn(new String[] { "default=second" });
        SelectComponent.EnumerationSelector selectComponent = new SelectComponent.EnumerationSelector();
        selectComponent.setup("testSelect", javaField, formField, sling);

        selectComponent.getHtml();

        verify(sling, times(1)).include(resourceCaptor.capture());
        Resource resource = resourceCaptor.getValue();

        List<String> options = StreamSupport.stream(resource.getChild("items").getChildren().spliterator(), false).map(r -> {
            ValueMap props = r.getValueMap();
            return String.format("%s:%s:%s", props.get("text"), props.get("value"), props.get("selected", false));
        }).collect(Collectors.toList());

        assertEquals(Arrays.asList(
            "First:first:false",
            "Second:second:true",
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
