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

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;

/**
 * Represent multifield with sub-fields based on referenced class. Depending on
 * where this is used, some javascript should be included by the front-end to
 * process the resulting form correctly.
 */
public class MultifieldComponent extends AbstractContainerComponent {

    public MultifieldComponent() {
        setResourceType("granite/ui/components/coral/foundation/form/multifield");
    }

     @Override
    public Resource buildComponentResource() {
        getComponentMetadata().put("composite", isComposite());
        AbstractResourceImpl res = new AbstractResourceImpl(getPath(), getResourceType(), getResourceSuperType(), getComponentMetadata());
        if (sling != null) {
            res.setResourceResolver(sling.getRequest().getResourceResolver());
        }
        if (isComposite()) {
            AbstractResourceImpl field = new AbstractResourceImpl(getPath() + "/field", "granite/ui/components/coral/foundation/container", getResourceSuperType(), new ResourceMetadata());
            // The container component is what sets the name, not the base component
            field.getResourceMetadata().put("name", getName());
            res.addChild(field);
            AbstractResourceImpl items = generateItemsResource(getPath() + "/field", true);
            field.addChild(items);
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
}