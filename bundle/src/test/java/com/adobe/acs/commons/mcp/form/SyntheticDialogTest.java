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

import java.util.Iterator;
import java.util.List;
import org.apache.sling.api.resource.Resource;
import org.junit.Before;
import org.junit.Test;

import static com.adobe.acs.commons.mcp.form.MultifieldComponent.NODE_PATH;
import static org.junit.Assert.*;

/**
 * Assert the generation behavior of synthetic dialogs from a java bean -- which
 * could be a sling model also. Also try various components and initialization
 * behaviors.
 */
public class SyntheticDialogTest {

    TestPojo testPojo;

    @Before
    public void init() {
        testPojo = new TestPojo();
        testPojo.init();
        testPojo.getFieldComponents();
    }

    @Test
    public void testDialogInit() {
        assertFieldExists("textField");
        assertFieldExists("multiField");
        assertFieldExists("simpleMultiField");
        assertFieldExists("readOnly");
        assertFieldExists("tags");
        assertFieldExists("textArea");
        assertFieldExists("beanProperty");
        assertFieldExists("booleanProperty");
    }

    public void assertFieldExists(String name) {
        assertNotNull("Checking for field " + name, testPojo.getFieldComponents().get(name));
        assertEquals("Validating field name " + name, name, testPojo.getFieldComponents().get(name).getName());
    }

    @Test
    public void testCompositeMultifieldComponentGeneration() {
        MultifieldComponent component = (MultifieldComponent) testPojo.getFieldComponents().get("multiField");
        component.setPath("/test/path");
        assertNotNull(component.getFieldComponents().get("subField1"));
        assertNotNull(component.getFieldComponents().get("subField2"));
        assertNotNull(component.getFieldComponents().get("subField3"));
        assertNotNull(component.getFieldComponents().get("subField4"));
        assertNotNull(component.getFieldComponents().get("subField5"));
        assertNotNull(component.getFieldComponents().get("subField6"));
        assertTrue(component.isComposite());
        AbstractResourceImpl res = (AbstractResourceImpl) component.buildComponentResource();
        assertNotNull(res);
        assertEquals("/test/path", res.getPath());
        assertNotNull("Multifield structure check 1", res.getChild("field"));
        assertNotNull("Multifield structure check 2", res.getChild("field/items"));
        // Fields should be grouped in a fieldset, but not divided into categories
        assertNotNull("Should include subfield1 component", res.getChild("field/items/fields/items/subField1"));
        assertNotNull("Should include subfield2 component", res.getChild("field/items/fields/items/subField2"));
        assertNotNull("Should include subfield3 component", res.getChild("field/items/fields/items/subField3"));
        assertNotNull("Should include subfield4 component", res.getChild("field/items/fields/items/subField4"));
        assertNotNull("Should include subfield5 component", res.getChild("field/items/fields/items/subField5"));
        assertNotNull("Should include subfield5 component", res.getChild("field/items/fields/items/subField6"));
    }

    @Test
    public void testGroupingBehavior() {
        AbstractResourceImpl res = (AbstractResourceImpl) testPojo.getFormResource();
        assertNotNull("Form structure -- Confirm form container", res.getChild("items"));
        assertNotNull("Form structure -- Confirm presence of tab container", res.getChild("items/tabs"));
        assertNotNull("Form structure -- Confirm tab items list", res.getChild("items/tabs/items"));
        assertEquals("Should have 4 tabs", 4, res.getChild("items/tabs/items").getChildren().spliterator().getExactSizeIfKnown());
        for (Resource tab : res.getChild("items/tabs/items").getChildren()) {
            assertNotEquals("Should not have a 'Misc' tab", AbstractGroupingContainerComponent.GENERIC_GROUP, tab.getResourceMetadata().get("jcr:title"));
        }
    }

    @Test
    public void testSimpleMultifieldComponentGeneration() {
        MultifieldComponent component = (MultifieldComponent) testPojo.getFieldComponents().get("simpleMultiField");
        component.setPath("/test/path");
        assertFalse(component.isComposite());
        AbstractResourceImpl res = (AbstractResourceImpl) component.buildComponentResource();
        assertNotNull(res);
        assertEquals("/test/path", res.getPath());
        assertNotNull("Multifield node check", res.getChild("field"));
    }

    @Test
    public void testClientLibraryHandling() {
        assertEquals(1, testPojo.getAllClientLibraries().size());
        assertEquals(1, testPojo.getJsClientLibraries().size());
        assertEquals(1, testPojo.getCssClientLibraries().size());
        assert(testPojo.getAllClientLibraries().contains("component-all"));
        assert(testPojo.getJsClientLibraries().contains("component-js"));
        assert(testPojo.getCssClientLibraries().contains("component-css"));
    }

    @Test
    public void testSubclassBehaviors() {
        TestInherited subclass = new TestInherited();
        subclass.init();
        subclass.getFieldComponents();
        Resource form = subclass.getFormResource();
        // Check for correct number of categories
        AbstractResourceImpl tabs = (AbstractResourceImpl) form.getChild("items/tabs/items");
        assertEquals("Should have 4 tabs", 4, tabs.children.size());
        // Assert correct number and order of tabs
        Iterator<Resource> children = tabs.listChildren();
        assertEquals("Tab 1 should be first", "1", children.next().getValueMap().get("jcr:title"));
        assertEquals("Tab 2 should be second", "2", children.next().getValueMap().get("jcr:title"));
        assertEquals("Tab 3 should be third", "3", children.next().getValueMap().get("jcr:title"));
        assertEquals("Tab 4 should be fourth", "4", children.next().getValueMap().get("jcr:title"));
        // Check if somethingElse is last
        AbstractResourceImpl tab3 = (AbstractResourceImpl) tabs.children.get(2).getChild("items");
        assertEquals("readOnly should be first", "readOnly", tab3.children.get(0).getName());
        assertEquals("tags should be second", "tags", tab3.children.get(1).getName());
        assertEquals("somethingElse should be last", "somethingElse", tab3.children.get(2).getName());
        // Check if additionalTestArea is last
        AbstractResourceImpl tab4 = (AbstractResourceImpl) tabs.children.get(3).getChild("items");
        assertEquals("additionalTextArea should be last", "additionalTextArea", tab4.children.get(1).getName());
    }

    public static class ComponentWithClientLibraries extends FieldComponent {
        @Override
        public void init() {
            addClientLibraries(FieldComponent.ClientLibraryType.JS, "component-js");
            addClientLibraries(FieldComponent.ClientLibraryType.CSS, "component-css");
            addClientLibrary("component-all");
        }
    }

    public static class TestPojo extends GeneratedDialog {
        @FormField(component = TextfieldComponent.class, name = "Text Field", category="1")
        String textField;

        @FormField(component = MultifieldComponent.class, name = "Multifield (composite)", category="1")
        List<TestSubtype> multiField;

        @FormField(component = MultifieldComponent.class, name = "Multifield (simple)", category="2", options = NODE_PATH)
        List<String> simpleMultiField;

        @FormField(component = MultifieldComponent.class, name = "Multifield (simple)", category="2")
        String[] simpleArrayMultiField;

        @FormField(component = ReadonlyTextfieldComponent.class, name = "Read-only", category="3")
        String readOnly;

        @FormField(component = TagPickerComponent.class, name = "tags", category="3")
        List<String> tags;

        @FormField(component = TextareaComponent.class, name = "Text Area", category="4")
        String textArea;

        String beanProperty;

        @FormField(name = "Bean property", category="4")
        public String getBeanProperty() {
            return beanProperty;
        }

        boolean booleanProperty;

        @FormField(component = CheckboxComponent.class, name = "Boolean property", category="4")
        public boolean isBooleanProperty() {
            return booleanProperty;
        }
    }

    public static class TestSubtype {

        @FormField(component = TextfieldComponent.class, name = "Text Field")
        String subField1;

        @FormField(component = TextfieldComponent.class, name = "Text Field")
        String subField2;

        @FormField(component = ReadonlyTextfieldComponent.class, name = "Read-only")
        String subField3;

        @FormField(component = TagPickerComponent.class, name = "tags")
        List<String> subField4;

        @FormField(component = TextareaComponent.class, name = "Text Area")
        String subField5;

        @FormField(component = ComponentWithClientLibraries.class, name = "Component with client libs")
        String subField6;
    }

    public static class TestInherited extends TestPojo {
        @FormField(component = TextareaComponent.class, name = "Something Else", category="3")
        String somethingElse;

        @FormField(component = TextareaComponent.class, name = "Text Area", category="4")
        String additionalTextArea;
    }
}
