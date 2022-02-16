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

import com.adobe.acs.commons.data.Variant;
import com.adobe.acs.commons.mcp.util.IntrospectionUtil;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.scripting.SlingScriptHelper;

/**
 * Describes a component in a manner which supports auto-generated forms
 */
public abstract class FieldComponent {

    private String name;
    private FormField formField;
    private AccessibleObject accessibleObject;
    private SlingScriptHelper sling;
    private final ResourceMetadata componentMetadata = new ResourceMetadata();
    private String resourceType = "granite/ui/components/coral/foundation/form/textfield";
    private String resourceSuperType = "granite/ui/components/coral/foundation/form/field";
    private Resource resource;
    private String path = "/fake/path";
    private final EnumMap<ClientLibraryType, Set<String>> clientLibraries = new EnumMap<>(ClientLibraryType.class);
    private String category;

    public final void setup(String name, AccessibleObject fieldOrMethod, FormField field, SlingScriptHelper sling) {
        this.name = name;
        this.formField = field;
        this.sling = sling;
        this.accessibleObject = fieldOrMethod;
        this.setCategory(field.category());
        if (!componentMetadata.containsKey("name")) {
            componentMetadata.put("name", name);
        }
        componentMetadata.put("fieldLabel", formField.name());
        if (!StringUtils.isEmpty(formField.description())) {
            componentMetadata.put("fieldDescription", formField.description());
        }
        if (formField.required()) {
            componentMetadata.put("required", formField.required());
        }
        componentMetadata.put("emptyText", formField.hint());
        if (formField.showOnCreate()) {
            componentMetadata.put("cq:showOnCreate", true);
        }

        Optional<String> defaultValue = getOption("default");
        if (!defaultValue.isPresent()) {
            defaultValue = IntrospectionUtil.getDeclaredValue(fieldOrMethod).map(String::valueOf);
        }
        defaultValue.ifPresent(val -> componentMetadata.put("value", val));
        init();
    }

    public abstract void init();

    public final void setHelper(SlingScriptHelper helper) {
        this.sling = helper;
    }

    public final SlingScriptHelper getHelper() {
        return sling;
    }

    public final void setPath(String path) {
        this.path = path;
    }

    public final String getPath() {
        return path;
    }

    /**
     * Get form field if possible
     * @return Form field if a safe cast is possible otherwise null
     * @deprecated Use getAccessibleObject and AccessibleObjectUtils to handle both Method (getter) or Fields
     */
    @Deprecated
    public final Field getField() {
        if (accessibleObject instanceof Field) {
            return (Field) accessibleObject;
        } else {
            return null;
        }
    }

    public final AccessibleObject getAccessibleObject() {
        return accessibleObject;
    }

    public final FormField getFieldDefinition() {
        return formField;
    }

    public final String getHtml() {
        sling.include(getComponentResource());
        return "";
    }

    private Resource getComponentResource() {
        if (resource == null) {
            purgeEmptyMetadata();
            resource = buildComponentResource();
            if (resource instanceof AbstractResourceImpl && sling != null) {
                ((AbstractResourceImpl) resource).setResourceResolver(sling.getRequest().getResourceResolver());
            }
        }
        return resource;
    }

    /**
     * If your component needs child nodes then override this method, call the
     * superclass implementation, and then use addChildren to add additional
     * nodes to it.
     *
     * @return
     */
    public Resource buildComponentResource() {
        purgeEmptyMetadata();
        AbstractResourceImpl res = new AbstractResourceImpl(path, resourceType, resourceSuperType, componentMetadata);
        if (sling != null) {
            res.setResourceResolver(sling.getRequest().getResourceResolver());
        }
        return res;
    }

    /**
     * @return the componentMetadata
     */
    public final ResourceMetadata getComponentMetadata() {
        return componentMetadata;
    }

    public final Map<ClientLibraryType, Set<String>> getClientLibraryCategories() {
        return Collections.unmodifiableMap(clientLibraries);
    }

    public final void addClientLibrary(String category) {
        addClientLibraries(ClientLibraryType.ALL, Arrays.asList(category));
    }

    public final void addClientLibraries(ClientLibraryType type, String... categories) {
        addClientLibraries(type, Arrays.asList(categories));
    }

    public final void addClientLibraries(ClientLibraryType type, Collection<String> categories) {
        Set<String> categoriesSet = clientLibraries.getOrDefault(type, new LinkedHashSet<>());
        categoriesSet.addAll(categories);
        clientLibraries.put(type, categoriesSet);
    }

    public final void addClientLibraries(FieldComponent component) {
        component.getClientLibraryCategories().forEach((type, categories) -> {
            if (categories != null) {
                addClientLibraries(type, categories);
            }
        });
    }

    /**
     * @return the resourceType
     */
    public final String getResourceType() {
        return resourceType;
    }

    /**
     * @param resourceType the resourceType to set
     */
    public final void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    /**
     * @return the resourceSuperType
     */
    public final String getResourceSuperType() {
        return resourceSuperType;
    }

    /**
     * @param resourceSuperType the resourceSuperType to set
     */
    public final void setResourceSuperType(String resourceSuperType) {
        this.resourceSuperType = resourceSuperType;
    }

    public final void purgeEmptyMetadata() {
        Set<String> emptyKeys = new HashSet<>();
        componentMetadata.forEach((key, value) -> {
            if (value == null || "".equals(value)) {
                emptyKeys.add(key);
            }
        });
        componentMetadata.keySet().removeAll(emptyKeys);
    }

    /**
     * @return the name
     */
    public final String getName() {
        return name;
    }

    public final Collection<String> getOptionNames() {
        if (formField == null || formField.options() == null) {
            return Collections.emptySet();
        }
        return Stream.of(formField.options())
                .map(s -> StringUtils.substringBefore(s, "="))
                .collect(Collectors.toList());
    }

    public final boolean hasOption(String optionName) {
        if (formField == null || formField.options() == null) {
            return false;
        } else {
            return Stream.of(formField.options())
                    .filter(s -> s.equalsIgnoreCase(optionName) || s.startsWith(optionName + "="))
                    .findFirst().isPresent();
        }
    }

    public final Optional<String> getOption(String option) {
        if (formField == null || formField.options() == null) {
            return Optional.empty();
        } else {
            return Stream.of(formField.options())
                    .filter(s -> s.startsWith(option + "="))
                    .findFirst().map(o -> o.split("=")[1]);
        }
    }

    public final Optional<Boolean> getBooleanOption(String option) {
        return getOption(option).map(s -> Variant.convert(s, Boolean.class));
    }

    /**
     * @return the category
     */
    public final String getCategory() {
        return category;
    }

    /**
     * @param category the category to set
     */
    public final void setCategory(String category) {
        this.category = category;
    }

    public static enum ClientLibraryType {
        JS, CSS, ALL
    }
}
