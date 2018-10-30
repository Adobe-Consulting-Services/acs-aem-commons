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
        assertNotNull(testPojo.getFieldComponents().get("textField"));
        assertEquals("textField", testPojo.getFieldComponents().get("textField").getName());

        assertNotNull(testPojo.getFieldComponents().get("multiField"));
        assertEquals("multiField", testPojo.getFieldComponents().get("multiField").getName());
    }

    @Test
    public void testMultifieldComponentGeneration() {
        MultifieldComponent component = (MultifieldComponent) testPojo.getFieldComponents().get("multiField");
        component.setPath("/test/path");
        assertNotNull(component.getFieldComponents().get("subField1"));
        assertNotNull(component.getFieldComponents().get("subField2"));
        AbstractResourceImpl res = (AbstractResourceImpl) component.buildComponentResource();
        assertNotNull(res);
        assertEquals("/test/path", res.getPath());
        assertNotNull("Multifield structure check 1", res.getChild("field"));
        assertNotNull("Multifield structure check 2", res.getChild("field/items"));
        assertNotNull("Should include subfield1 component", res.getChild("field/items/subField1"));
        assertNotNull("Should include subfield2 component", res.getChild("field/items/subField2"));
    }

    public class TestPojo extends GeneratedDialog {

        @FormField(component = TextfieldComponent.class, name = "Text Field")
        String textField;

        @FormField(component = MultifieldComponent.class, name = "Multifield")
        List<TestSubtype> multiField;
    }

    public class TestSubtype {

        @FormField(component = TextfieldComponent.class, name = "Text Field")
        String subField1;

        @FormField(component = TextfieldComponent.class, name = "Text Field")
        String subField2;
    }
}
