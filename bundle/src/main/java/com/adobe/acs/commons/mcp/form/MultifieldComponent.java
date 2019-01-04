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

import com.adobe.acs.commons.mcp.util.AnnotatedFieldDeserializer;
import java.lang.reflect.ParameterizedType;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;

/**
 * Represent multifield with sub-fields based on referenced class.
 * Depending on where this is used, some javascript should be included by the front-end
 * to process the resulting form correctly.
 */
public class MultifieldComponent extends FieldComponent {

    Map<String, FieldComponent> fieldComponents = new LinkedHashMap<>();
    boolean isComposite = true;

    public MultifieldComponent() {
        setResourceType("granite/ui/components/coral/foundation/form/multifield");
    }

    public void init() {
        if (getField() != null) {
            ParameterizedType type = (ParameterizedType) getField().getGenericType();
            Class clazz = (Class) type.getActualTypeArguments()[0];
            extractFieldComponents(clazz);
        }
        if (sling != null && sling.getRequest() != null) {
            setPath(sling.getRequest().getResource().getPath());
        }
    }

    @Override
    public Resource buildComponentResource() {
        getComponentMetadata().put("composite", isComposite);
        AbstractResourceImpl res = new AbstractResourceImpl(getPath(), getResourceType(), getResourceSuperType(), getComponentMetadata());
        if (sling != null) {
            res.setResourceResolver(sling.getRequest().getResourceResolver());
        }
        if (isComposite) {
            AbstractResourceImpl field = new AbstractResourceImpl(getPath() + "/field", "granite/ui/components/coral/foundation/container", getResourceSuperType(), new ResourceMetadata());
            // The container component is what sets the name, not the base component
            field.getResourceMetadata().put("name", getName());
            res.addChild(field);
            AbstractResourceImpl items = new AbstractResourceImpl(getPath() + "/field/items", "", "", new ResourceMetadata());
            field.addChild(items);
            for (FieldComponent component : fieldComponents.values()) {
                component.setPath(getPath() + "/field/items/" + component.getName());
                items.addChild(component.buildComponentResource());
            }
        } else {            
            for (FieldComponent component : fieldComponents.values()) {
                component.setPath(getPath() + "/field");
                Resource comp = component.buildComponentResource();
                comp.getResourceMetadata().put("name", getName());
                res.addChild(comp);
            }
        }

        return res;
    }

    public Map<String, FieldComponent> getFieldComponents() {
        return fieldComponents;
    }

    private void extractFieldComponents(Class clazz) {
        fieldComponents = new LinkedHashMap<>();
        if (clazz == String.class) {
            FieldComponent comp = new TextfieldComponent();
            FormField fieldDef = FormField.Factory.create(getName(), "", null, false, comp.getClass(), null);            
            comp.setup(getName(), null, fieldDef, sling);
            comp.getComponentMetadata().put("title", getName());
            // TODO: Provide a proper mechanism for setting path when creating components
            fieldComponents.put(getName(), comp);
            isComposite = false;
        } else {
            fieldComponents.putAll(AnnotatedFieldDeserializer.getFormFields(clazz, sling));
            isComposite = true;
        }
    }
}
