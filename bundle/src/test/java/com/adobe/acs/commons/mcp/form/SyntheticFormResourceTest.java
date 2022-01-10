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

import com.adobe.acs.commons.mcp.form.PathfieldComponent.AssetSelectComponent;
import com.adobe.acs.commons.mcp.form.RadioComponent.EnumerationSelector;
import com.adobe.acs.commons.mcp.util.AnnotatedFieldDeserializer;
import com.adobe.acs.commons.mcp.util.DeserializeException;
import java.util.Map;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.junit.Test;

import javax.inject.Named;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 */
public class SyntheticFormResourceTest {
    private enum TestEnum {value1,value2,value3}

    @FormField(
            component = TextfieldComponent.class,
            name = "Text component",
            options = {"default=defaultValue"},
            required = true
    )
    private String textComponentTest;

    @FormField(
            component = EnumerationSelector.class,
            name = "Radio component",
            options = {"default=value3"}
    )
    private TestEnum enumComponentTest;

    @FormField(
            component = AssetSelectComponent.class,
            name = "Path component",
            options = {"default=/dam/content"}
    )
    private String pathComponentTest;

    @FormField(
            component = CheckboxComponent.class,
            name = "Checkbox component",
            options = {"default=true"}
    )
    private boolean checkboxComponentTest;

    @FormField(
            component = PasswordComponent.class,
            name = "password component"
    )
    private String passwordComponentTest;

    @FormField(
            component = ButtonComponent.class,
            name = "generic button"
    )
    private String button;

    @Named("test:differentName")
    @FormField(
            name = "Text component",
            options = {"default=defaultValue"},
            required = true
    )
    private String renamedComponentTest;


    @Test
    public void defaultValuesTest() throws DeserializeException {
        Map<String, FieldComponent> form = AnnotatedFieldDeserializer.getFormFields(getClass(), null);
        assertNotNull(form.get("textComponentTest"));
        assertNotNull(form.get("enumComponentTest"));
        assertNotNull(form.get("pathComponentTest"));
        assertNotNull(form.get("checkboxComponentTest"));
        assertNotNull(form.get("passwordComponentTest"));

        assertEquals(TextfieldComponent.class, form.get("textComponentTest").getClass());
        assertEquals(EnumerationSelector.class, form.get("enumComponentTest").getClass());
        assertEquals(AssetSelectComponent.class, form.get("pathComponentTest").getClass());
        assertEquals(CheckboxComponent.class, form.get("checkboxComponentTest").getClass());
        assertEquals(PasswordComponent.class, form.get("passwordComponentTest").getClass());

        assertEquals("defaultValue", form.get("textComponentTest").getOption("default").orElse(null));
        assertEquals(TestEnum.value3.name(), form.get("enumComponentTest").getOption("default").orElse(null));
        assertEquals("/dam/content", form.get("pathComponentTest").getOption("default").orElse(null));
        assertEquals("true", form.get("checkboxComponentTest").getOption("default").orElse(null));
        assertNull(form.get("passwordComponentTest").getOption("default").orElse(null));
    }

    @Test
    public void syntheticResourceTest() throws DeserializeException {
        SlingHttpServletRequest mockRequest = mock(SlingHttpServletRequest.class);
        SlingScriptHelper mockScriptHelper = mock(SlingScriptHelper.class);
        ResourceResolver mockResourceResolver = mock(ResourceResolver.class);
        when(mockScriptHelper.getRequest()).thenReturn(mockRequest);
        when(mockRequest.getResourceResolver()).thenReturn(mockResourceResolver);
        when(mockResourceResolver.getResource(anyString())).thenReturn(null);

        Map<String, FieldComponent> form = AnnotatedFieldDeserializer.getFormFields(getClass(), mockScriptHelper);
        assertNotNull(form.get("textComponentTest"));
        Resource fieldResource = form.get("textComponentTest").buildComponentResource();
        assertEquals("granite/ui/components/coral/foundation/form/textfield", fieldResource.getResourceType());
        assertEquals("granite/ui/components/coral/foundation/form/field", fieldResource.getResourceSuperType());
        assertEquals("textComponentTest", fieldResource.getResourceMetadata().get("name"));
        assertEquals("Text component", fieldResource.getResourceMetadata().get("fieldLabel"));
        assertEquals(true, fieldResource.getResourceMetadata().get("required"));
    }

    @Test
    public void namedPropertiesTest() {
        Map<String, FieldComponent> form = AnnotatedFieldDeserializer.getFormFields(getClass(), null);
        assertNull("Should not map to variable name for named properties", form.get("renamedComponentTest"));
        assertNotNull("Should not map to variable name for named properties", form.get("test:differentName"));
    }
}