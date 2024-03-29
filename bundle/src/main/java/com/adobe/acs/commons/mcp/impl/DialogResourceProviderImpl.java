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
package com.adobe.acs.commons.mcp.impl;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.reflect.FieldUtils;
import org.apache.commons.lang.reflect.MethodUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.mcp.form.AbstractResourceImpl;
import com.adobe.acs.commons.mcp.form.DialogProvider;
import com.adobe.acs.commons.mcp.form.GeneratedDialog;
import com.adobe.acs.commons.mcp.form.GeneratedDialogWrapper;
import com.adobe.acs.commons.mcp.util.SyntheticResourceBuilder;

/**
 * Create a resource provider for a dialog, the resource type is identified in
 * three ways: First it checks if the Model annotation is present and declares a
 * resource type. Failing that it looks for a getter named getResourceType.
 * Finally it looks for a public property named resourceType.
 * If resource type cannot be determined then this class will throw an exception.
 */
public class DialogResourceProviderImpl extends ResourceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DialogResourceProviderImpl.class);
    private String resourceType = "/unknown/type";
    private String root = "";
    private final GeneratedDialog dialog;
    private final AbstractResourceImpl resource;
    private final Class originalClass;
    private final boolean isComponent;

    public DialogResourceProviderImpl(Class c, DialogProvider annotation) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException  {
        originalClass = c;
        isComponent = annotation != null && annotation.style() == DialogProvider.DialogStyle.COMPONENT;
        if (GeneratedDialog.class.isAssignableFrom(c)) {
            dialog = (GeneratedDialog) c.getDeclaredConstructor().newInstance();
        } else {
            dialog = new GeneratedDialogWrapper(c);
        }
        dialog.initAnnotationValues(annotation);
        setResourceTypeFromClass();
        root = "/apps/" + resourceType + "/cq:dialog";
        AbstractResourceImpl formResource = (AbstractResourceImpl) dialog.getFormResource();
        AbstractResourceImpl formItems = ((AbstractResourceImpl) formResource.getChild("items")).cloneResource();
        SyntheticResourceBuilder rb = new SyntheticResourceBuilder(root, isComponent ? "cq/gui/components/authoring/dialog" : formResource.getResourceType());
        if (StringUtils.isNotBlank(dialog.getFormTitle())) {
            rb.withAttributes("jcr:title", dialog.getFormTitle());
        }
        rb.createChild("content", "granite/ui/components/coral/foundation/container");
        rb.withChild(formItems);
        resource = rb.build();
        resource.disableMergeResourceProvider();
    }

    private void setResourceTypeFromClass() throws IllegalAccessException, InstantiationException {
        Model modelAnnotation = (Model) originalClass.getAnnotation(Model.class);
        // Use the model annotation for resource type if possible
        if (modelAnnotation != null
                && modelAnnotation.resourceType() != null
                && modelAnnotation.resourceType().length == 1) {
            resourceType = modelAnnotation.resourceType()[0];
        } else {
            // Last-ditch effort is hope that there's a java bean property for it
            try {
                Method getter = MethodUtils.getMatchingAccessibleMethod(originalClass, "getResourceType", new Class[]{});
                if (getter != null) {
                    resourceType = String.valueOf(getter.invoke(originalClass.getDeclaredConstructor().newInstance()));
                } else {
                    Field field = FieldUtils.getField(originalClass, "resourceType", true);
                    if (field != null) {
                        resourceType = String.valueOf(field.get(originalClass.getDeclaredConstructor().newInstance()));
                    } else {
                        throw new InstantiationException(String.format("No resource type present for %s", originalClass.getCanonicalName()));
                    }
                }
            } catch (RuntimeException | ReflectiveOperationException ex) {
                LOGGER.debug("Unable to determine sling resource type for model bean: {} ", originalClass);
            }
        }
    }

    @Nullable
    @Override
    public Resource getResource(@NotNull ResolveContext resolveContext, @NotNull String path, @NotNull ResourceContext resourceContext, @Nullable Resource parent) {
        LOGGER.debug("Get resource at path: {}", path);
        AbstractResourceImpl clone = null;
        if (root.equals(path)) {
            clone = ((AbstractResourceImpl) this.resource).cloneResource();
        } else if (parent != null && path.equals(parent.getPath())) {
            return parent;
        } else {
            String relPath = path.substring(root.length() + 1);
            LOGGER.debug("Relative path: {}", relPath);
            AbstractResourceImpl child = (AbstractResourceImpl) this.resource.getChild(relPath);
            if (child != null) {
                clone = child.cloneResource();
            }

        }
        if (clone != null) {
            clone.setResourceResolver(resolveContext.getResourceResolver());
            clone.setPath(path);
        } else {
            LOGGER.debug("Unable to find node for {}", path);
        }
        return clone;
    }

    @Nullable
    @Override
    public Iterator<Resource> listChildren(@NotNull ResolveContext resolveContext, @NotNull Resource parent) {
        LOGGER.debug("List children of {}", parent.getPath());
        if (parent instanceof AbstractResourceImpl) {
            AbstractResourceImpl res = (AbstractResourceImpl) parent;
            return res.listChildren();
        } else {
            return Collections.emptyIterator();
        }
    }

    public String getRoot() {
        return root;
    }
}
