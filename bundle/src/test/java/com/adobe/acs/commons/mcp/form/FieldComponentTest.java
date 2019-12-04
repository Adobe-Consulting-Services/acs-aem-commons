/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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

import com.adobe.acs.commons.mcp.form.FieldComponent.ClientLibraryType;
import com.adobe.acs.commons.mcp.util.AnnotatedFieldDeserializer;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class FieldComponentTest {
    @Test
    public void hasOption() {
        FieldComponent testComponent = new TestFieldComponent(new String[]{"a=b", "c", "d="});
        assertTrue(testComponent.hasOption("a"));
        assertTrue(testComponent.hasOption("c"));
        assertTrue(testComponent.hasOption("d"));
        assertFalse(testComponent.hasOption("z"));
    }

    @Test
    public void getOption() {
        FieldComponent testComponent = new TestFieldComponent(new String[]{"a=b", "c", "d="});
        assertEquals("b", testComponent.getOption("a").get());
        assertEquals(Optional.empty(), testComponent.getOption("c"));
        assertEquals(Optional.empty(), testComponent.getOption("z"));
    }

    @Test
    public void hasOptionNullOptions() {
        FieldComponent testComponent = new TestFieldComponent(null);
        assertFalse(testComponent.hasOption("z"));
    }

    @Test
    public void getOptionNullOptions() {
        FieldComponent testComponent = new TestFieldComponent(null);
        assertEquals(Optional.empty(), testComponent.getOption("z"));
    }

    @Test
    public void testClientLibraryTracking() {
        TestFieldComponent componentA = new TestFieldComponent(null);
        TestFieldComponent componentB = new TestFieldComponent(null);

        assertNotNull(componentA.getClientLibraryCategories());
        assertEquals(0, componentA.getClientLibraryCategories().size());

        componentB.addClientLibrary("All-Test1");
        componentB.addClientLibraries(FieldComponent.ClientLibraryType.JS, "JS-Test1", "JS-Test2");
        componentB.addClientLibraries(FieldComponent.ClientLibraryType.CSS, Arrays.asList("CSS-Test1", "CSS-Test2"));

        assertArrayEquals(new String[]{"JS-Test1", "JS-Test2"}, componentB.getClientLibraryCategories().get(ClientLibraryType.JS).toArray());
        assertArrayEquals(new String[]{"CSS-Test1", "CSS-Test2"}, componentB.getClientLibraryCategories().get(ClientLibraryType.CSS).toArray());

        componentA.addClientLibrary("All-Test2");
        componentA.addClientLibraries(componentB);
        assertArrayEquals(new String[]{"All-Test2", "All-Test1"}, componentA.getClientLibraryCategories().get(ClientLibraryType.ALL).toArray());
    }

    @Test
    public void testGetHtmlCallsSlingInclude() {
        SlingScriptHelper sling = mock(SlingScriptHelper.class);
        SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        when(sling.getRequest()).thenReturn(request);

        TestFieldComponent componentA = new TestFieldComponent(null);
        componentA.setHelper(sling);
        componentA.setPath("/apps/some/path");

        assertEquals("", componentA.getHtml());

        ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        verify(sling, times(1)).include(resourceCaptor.capture());
        assertEquals("/apps/some/path", resourceCaptor.getValue().getPath());
    }

    public static final String TEST_VALUE_1 = "Madness?  This is sparta!";

    @Test
    public void testDefaultFormValuesMatchCodeDefaults() {
        Map<String, FieldComponent> form = AnnotatedFieldDeserializer.getFormFields(AnnotationTestClass.class, null);
        assertEquals("Should have default string value", TEST_VALUE_1, form.get("test1").getComponentMetadata().get("value"));
        assertEquals("1st Checkbox should be checked", "true", form.get("isChecked").getComponentMetadata().get("checked"));
        assertEquals("2nd Checkbox not should be checked", null, form.get("isNotChecked").getComponentMetadata().get("checked"));
    }

    public static class AnnotationTestClass {
        @FormField(name="field 1")
        String test1=TEST_VALUE_1;

        @FormField(name="Checkbox", component = CheckboxComponent.class)
        boolean isChecked = true;

        @FormField(name="Checkbox", component = CheckboxComponent.class)
        boolean isNotChecked;
    }

    public static class TestFieldComponent extends FieldComponent {
        public TestFieldComponent(String[] options) {
            FormField field = new FormField() {
                @Override
                public boolean equals(Object obj) {
                    return false;
                }

                @Override
                public int hashCode() {
                    return 0;
                }

                @Override
                public String toString() {
                    return null;
                }

                @Override
                public Class<? extends Annotation> annotationType() {
                    return null;
                }

                @Override
                public String name() {
                    return null;
                }

                @Override
                public String hint() {
                    return null;
                }

                @Override
                public String description() {
                    return null;
                }

                @Override
                public String category() {
                    return null;
                }

                @Override
                public boolean required() {
                    return false;
                }

                @Override
                public Class<? extends FieldComponent> component() {
                    return null;
                }

                @Override
                public String[] options() {
                    return options;
                }
            };
            setup("test", null, field, null);
        }

        @Override
        public void init() {

        }
    }

}