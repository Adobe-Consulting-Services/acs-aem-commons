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

import java.util.List;
import org.junit.Before;
import org.junit.Test;

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
    }

    @Test
    public void testDialogInit() {
        assertFieldExists("textField");
        assertFieldExists("multiField");
        assertFieldExists("simpleMultiField");
        assertFieldExists("readOnly");
        assertFieldExists("tags");
        assertFieldExists("textArea");
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
        assertTrue(component.isComposite);
        AbstractResourceImpl res = (AbstractResourceImpl) component.buildComponentResource();
        assertNotNull(res);
        assertEquals("/test/path", res.getPath());
        assertNotNull("Multifield structure check 1", res.getChild("field"));
        assertNotNull("Multifield structure check 2", res.getChild("field/items"));
        assertNotNull("Should include subfield1 component", res.getChild("field/items/subField1"));
        assertNotNull("Should include subfield2 component", res.getChild("field/items/subField2"));
        assertNotNull("Should include subfield3 component", res.getChild("field/items/subField3"));
        assertNotNull("Should include subfield4 component", res.getChild("field/items/subField4"));
        assertNotNull("Should include subfield5 component", res.getChild("field/items/subField5"));
    }

    @Test
    public void testSimpleMultifieldComponentGeneration() {
        MultifieldComponent component = (MultifieldComponent) testPojo.getFieldComponents().get("simpleMultiField");
        component.setPath("/test/path");
        assertFalse(component.isComposite);
        AbstractResourceImpl res = (AbstractResourceImpl) component.buildComponentResource();
        assertNotNull(res);
        assertEquals("/test/path", res.getPath());
        assertNotNull("Multifield node check", res.getChild("field"));
    }

    public class TestPojo extends GeneratedDialog {

        @FormField(component = TextfieldComponent.class, name = "Text Field")
        String textField;

        @FormField(component = MultifieldComponent.class, name = "Multifield (composite)")
        List<TestSubtype> multiField;

        @FormField(component = MultifieldComponent.class, name = "Multifield (simple)")
        List<String> simpleMultiField;

        @FormField(component = ReadonlyTextfieldComponent.class, name = "Read-only")
        String readOnly;

        @FormField(component = TagPickerComponent.class, name = "tags")
        List<String> tags;

        @FormField(component = TextareaComponent.class, name = "Text Area")
        String textArea;
    }

    public class TestSubtype {

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
    }
}
