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

import com.adobe.acs.commons.mcp.util.AnnotatedFieldDeserializer;
import java.util.Map;
import org.apache.sling.api.scripting.SlingScriptHelper;

/**
 * Provides a generated dialog for annotated model classes that do not already extend GeneratedDialog
 */
public class GeneratedDialogWrapper extends GeneratedDialog {
    Class wrappedClass;

    public GeneratedDialogWrapper(Class c) {
        wrappedClass = c;
    }

    public GeneratedDialogWrapper(Class c, SlingScriptHelper slingHelper) {
        wrappedClass = c;
        sling = slingHelper;
    }

    @Override
    public Map<String, FieldComponent> getFieldComponents() {
        if (fieldComponents == null) {
            fieldComponents = AnnotatedFieldDeserializer.getFormFields(wrappedClass, getSlingHelper());
        }
        return fieldComponents;
    }
}
