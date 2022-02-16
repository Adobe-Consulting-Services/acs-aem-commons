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

import com.adobe.acs.commons.mcp.util.IntrospectionUtil;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Radio button selector component
 */
@ProviderType
public final class CheckboxComponent extends FieldComponent {
    @Override
    public void init() {
        setResourceType("granite/ui/components/foundation/form/checkbox");
        getComponentMetadata().put("text", getFieldDefinition().name());
        getComponentMetadata().put("value", "true");
        getComponentMetadata().put("uncheckedValue", "false");
        getComponentMetadata().put("required", false);
        boolean trueByDefault = IntrospectionUtil.getDeclaredValue(getAccessibleObject()).map(Boolean.TRUE::equals).orElse(false);
        if (hasOption("checked") || trueByDefault) {
            getComponentMetadata().put("checked", "true");
        }
    }
}
