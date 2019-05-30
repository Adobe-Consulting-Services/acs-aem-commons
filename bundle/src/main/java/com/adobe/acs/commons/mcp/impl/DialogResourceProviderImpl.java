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
package com.adobe.acs.commons.mcp.impl;

import com.adobe.acs.commons.mcp.form.AbstractResourceImpl;
import com.adobe.acs.commons.mcp.form.GeneratedDialog;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.apache.commons.lang.reflect.FieldUtils;
import org.apache.commons.lang.reflect.MethodUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DialogResourceProviderImpl extends ResourceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DialogResourceProviderImpl.class);
    private String resourceType = "/unknown/type";
    private String root = "";
    private GeneratedDialog dialog;
    private AbstractResourceImpl resource;

    public DialogResourceProviderImpl(Class<? extends GeneratedDialog> c) throws InstantiationException, IllegalAccessException {
        dialog = c.newInstance();
        setResourceTypeFromClass();
        root = "/apps/" + resourceType + "/cq:dialog";
        resource = (AbstractResourceImpl) dialog.getFormResource();
        resource.disableMergeResourceProvider();
        resource.setPath(root);
        AbstractResourceImpl items = ((AbstractResourceImpl) resource.getChild("items")).cloneResource();
        items.setPath("items");
        AbstractResourceImpl content = new AbstractResourceImpl("content", "granite/ui/components/coral/foundation/fixedcolumns", null, null);
        content.addChild(items);
        resource.addChild(content);
    }

    private void setResourceTypeFromClass() throws IllegalAccessException, InstantiationException {
        Model modelAnnotation = dialog.getClass().getAnnotation(Model.class);
        // Use the model annotation for resource type if possible
        if (modelAnnotation != null
                && modelAnnotation.resourceType() != null
                && modelAnnotation.resourceType().length == 1) {
            resourceType = modelAnnotation.resourceType()[0];
        } else {
            // Last-ditch effort is hope that there's a java bean property for it
            try {
                Method getter = MethodUtils.getMatchingAccessibleMethod(dialog.getClass(), "getResourceType", new Class[]{});
                if (getter != null) {
                    resourceType = String.valueOf(getter.invoke(dialog));
                } else {
                    Field field = FieldUtils.getField(dialog.getClass(), "resourceType", true);
                    if (field != null) {
                        resourceType = String.valueOf(field.get(dialog));
                    }
                }
            } catch (InvocationTargetException | IllegalAccessException ex) {
                LOGGER.debug("Unable to determine sling resource type for model bean: " + dialog.getClass());
            }
        }
    }

    @CheckForNull
    @Override
    public Resource getResource(@Nonnull ResolveContext resolveContext, @Nonnull String path, @Nonnull ResourceContext resourceContext, @CheckForNull Resource parent) {
        LOGGER.debug("Get resource at path: " + path);
        AbstractResourceImpl clone = null;
        if (root.equals(path)) {
            clone = ((AbstractResourceImpl) this.resource).cloneResource();
        } else if (parent != null && path.equals(parent.getPath())) {
            return parent;
        } else {
            String relPath = path.substring(root.length() + 1);
            LOGGER.debug("Relative path: " + relPath);
            AbstractResourceImpl child = (AbstractResourceImpl) this.resource.getChild(relPath);
            if (child != null) {
                clone = child.cloneResource();
            }

        }
        if (clone != null) {
            clone.setResourceResolver(resolveContext.getResourceResolver());
            clone.setPath(path);
        } else {
            LOGGER.debug("Unable to find node for " + path);
        }
        return clone;
    }

    @CheckForNull
    @Override
    public Iterator<Resource> listChildren(@Nonnull ResolveContext resolveContext, @Nonnull Resource parent) {
        LOGGER.debug("List children of " + parent.getPath());
        return parent.listChildren();
    }

    public String getRoot() {
        return root;
    }
}
