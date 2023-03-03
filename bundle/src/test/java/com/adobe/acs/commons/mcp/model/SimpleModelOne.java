/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.mcp.model;

import com.adobe.acs.commons.mcp.form.AutocompleteComponent;
import com.adobe.acs.commons.mcp.form.FormField;
import com.adobe.acs.commons.mcp.form.GeneratedDialog;
import com.adobe.acs.commons.mcp.form.RichTextEditorComponent;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;

/**
 * Simple sling model with a resource type declared in the model annotation directly.
 * This extends GeneratedDialog, so should be detected as a dialog provider.
 */
@Model(adaptables = {Resource.class, SlingHttpServletRequest.class},
        resourceType = "test/model1"
)
public class SimpleModelOne extends GeneratedDialog {
    @FormField(name = "Field 1")
    private String field1;

    @FormField(name = "Rich Text", component = RichTextEditorComponent.class)
    private String field2;

    @FormField(name = "Tags", component = AutocompleteComponent.class)
    private String[] field3;
}
