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
import com.adobe.acs.commons.mcp.form.PathfieldComponent.FolderSelectComponent;
import com.adobe.acs.commons.mcp.form.PathfieldComponent.NodeSelectComponent;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Represent multifield with sub-fields based on referenced class. Depending on
 * where this is used, some javascript should be included by the front-end to
 * process the resulting form correctly.
 */
@ProviderType
public final class MultifieldComponent extends AbstractContainerComponent {

    public static final String FIELD_PATH = "/field";
    public static final String NODE_PATH = "node_path";
    public static final String ASSET_PATH = "asset_path";
    public static final String FOLDER_PATH = "folder_path";
    public static final String USE_CLASS = "use_class";

    public MultifieldComponent() {
        setResourceType("granite/ui/components/coral/foundation/form/multifield");
    }

    public void init() {
        if (hasOption(NODE_PATH)) {
            setDefaultChildComponent(NodeSelectComponent.class);
        } else if (hasOption(ASSET_PATH)) {
            setDefaultChildComponent(AssetSelectComponent.class);
        } else if (hasOption(FOLDER_PATH)) {
            setDefaultChildComponent(FolderSelectComponent.class);
        } else {
            getOption(USE_CLASS).ifPresent(c -> {
                try {
                    setDefaultChildComponent((Class<? extends FieldComponent>) Class.forName(c));
                } catch (ClassNotFoundException ex) {
                    Logger.getLogger(MultifieldComponent.class.getName()).log(Level.SEVERE, "Unable to find class " + c, ex);
                }
            });
        }
        super.init();
    }

    @Override
    public Resource buildComponentResource() {
        getComponentMetadata().put("composite", isComposite());
        AbstractResourceImpl res = new AbstractResourceImpl(getPath(), getResourceType(), getResourceSuperType(), getComponentMetadata());
        if (getHelper() != null) {
            res.setResourceResolver(getHelper().getRequest().getResourceResolver());
        }
        if (isComposite()) {
            AbstractResourceImpl field = new AbstractResourceImpl(getPath() + FIELD_PATH, "granite/ui/components/coral/foundation/container", getResourceSuperType(), new ResourceMetadata());
            // The container component is what sets the name, not the base component
            field.getResourceMetadata().put("name", getName());
            res.addChild(field);
            AbstractResourceImpl items = generateItemsResource(getPath() + FIELD_PATH, true);
            field.addChild(items);
        } else {
            for (FieldComponent component : fieldComponents.values()) {
                component.setPath(getPath() + FIELD_PATH);
                component.getComponentMetadata().putAll(getComponentMetadata());
                Resource comp = component.buildComponentResource();
                comp.getResourceMetadata().put("name", getName());
                res.addChild(comp);
            }
        }

        return res;
    }
}
