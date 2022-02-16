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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.wcm.testing.mock.aem.junit.AemContext;

@RunWith(MockitoJUnitRunner.class)
public class DatePickerComponentTest {

    @Rule
    public AemContext ctx = new AemContext(ResourceResolverType.JCR_MOCK);

    private Field field;

    @Mock
    private FormField formField;

    @Mock
    private SlingScriptHelper slingScriptHelper;

    @Before
    public void setup() {
        when(slingScriptHelper.getRequest()).thenReturn(ctx.request());
    }

    @Test
    public void normalDateField() {
        final DatePickerComponent dateComponent = new DatePickerComponent();
        when(formField.name()).thenReturn("Start Date");
        when(formField.description()).thenReturn("Select the start date.");
        dateComponent.setup("startDate", field, formField, slingScriptHelper);

        Resource resource = dateComponent.buildComponentResource();
        ValueMap map = resource.getValueMap();

        assertEquals("wrong component resource type.", "granite/ui/components/coral/foundation/form/datepicker",
                resource.getResourceType());

        assertEquals("wrong field label", "Start Date", map.get("fieldLabel"));
        assertEquals("wrong field description", "Select the start date.", map.get("fieldDescription"));
    }

    @Test
    public void dateTimeField() {
        final DatePickerComponent dateComponent = new DatePickerComponent();

        String[] opts = new String[1];
        opts[0] = DatePickerComponent.TYPE_OPT_DATETIME;
        when(formField.options()).thenReturn(opts);

        when(formField.name()).thenReturn("Start Date");
        when(formField.description()).thenReturn("Select the start date.");
        dateComponent.setup("startDate", field, formField, slingScriptHelper);

        Resource resource = dateComponent.buildComponentResource();
        ValueMap map = resource.getValueMap();

        assertEquals("wrong component type.", DatePickerComponent.TYPE_OPT_DATETIME,
                map.get(DatePickerComponent.TYPE));
    }

    @Test
    public void timeField() {
        final DatePickerComponent dateComponent = new DatePickerComponent();

        String[] opts = new String[1];
        opts[0] = DatePickerComponent.TYPE_OPT_TIME;
        when(formField.options()).thenReturn(opts);

        when(formField.name()).thenReturn("Start Date");
        when(formField.description()).thenReturn("Select the start date.");
        dateComponent.setup("startDate", field, formField, slingScriptHelper);

        Resource resource = dateComponent.buildComponentResource();
        ValueMap map = resource.getValueMap();

        assertEquals("wrong component type.", DatePickerComponent.TYPE_OPT_TIME,
                map.get(DatePickerComponent.TYPE));
    }
}
