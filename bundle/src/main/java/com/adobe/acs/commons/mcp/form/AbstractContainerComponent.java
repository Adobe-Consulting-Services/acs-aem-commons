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

import com.adobe.acs.commons.mcp.util.AccessibleObjectUtil;
import com.adobe.acs.commons.mcp.util.AnnotatedFieldDeserializer;
import com.adobe.acs.commons.mcp.util.SyntheticResourceBuilder;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.sling.api.resource.ResourceMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represent a generic container component which has one or more children
 */
public class AbstractContainerComponent extends FieldComponent {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractContainerComponent.class);

    Map<String, FieldComponent> fieldComponents = new LinkedHashMap<>();
    private boolean composite;
    private AbstractGroupingContainerComponent groupingContainer;
    private Class<? extends FieldComponent> defaultChildComponent = TextfieldComponent.class;

    private DialogProvider.DialogStyle dialogStyle = DialogProvider.DialogStyle.UNKNOWN;
    private String propertiesTabName = null;
    private boolean forceDotSlashPrefix = true;

    public void applyDialogProviderSettings(DialogProvider providerAnnotation) {
        setDialogStyle(providerAnnotation.style());
        setPropertiesTabName(providerAnnotation.propertiesTab());
        setForceDotSlashPrefix(providerAnnotation.forceDotSlashPrefix());
        if (groupingContainer != null) {
            groupingContainer.applyDialogProviderSettings(providerAnnotation);
        }
    }

    @Override
    public void init() {
        if (getAccessibleObject()!= null) {
            Class<?> fieldType = AccessibleObjectUtil.getType(getAccessibleObject());
            if (fieldType.isArray()) {
                extractFieldComponents(fieldType.getComponentType());
            } else if (Collection.class.isAssignableFrom(fieldType)) {
                ParameterizedType type = (ParameterizedType) AccessibleObjectUtil.getGenericType(getAccessibleObject());
                Class clazz = (Class) type.getActualTypeArguments()[0];
                extractFieldComponents(clazz);
            } else {
                extractFieldComponents(fieldType);
                fieldComponents.values().forEach(comp -> {
                    ResourceMetadata meta = comp.getComponentMetadata();
                    String currentName = String.valueOf(meta.get("name"));
                    meta.put("name", AccessibleObjectUtil.getFieldName(getAccessibleObject()) + "/" + currentName);
                });
            }
        }
        if (getHelper() != null) {
            setPath(getHelper().getRequest().getResource().getPath());
        }
    }

    public AbstractGroupingContainerComponent getGroupingContainer() {
        if (groupingContainer == null) {
            groupingContainer = new AbstractGroupingContainerComponent.AccordionComponent();
        }
        return groupingContainer;
    }

    public void setGroupingContainer(AbstractGroupingContainerComponent comp) {
        groupingContainer = comp;
    }

    public Map<String, FieldComponent> getFieldComponents() {
        return fieldComponents;
    }

    public FieldComponent generateDefaultChildComponent() {
        try {
            return defaultChildComponent.newInstance();
        } catch (InstantiationException | IllegalAccessException ex) {
            LOG.error("got exception", ex);
            return null;
        }
    }

    private void extractFieldComponents(Class clazz) {
        if (clazz == String.class || clazz.isPrimitive()) {
            FieldComponent comp = generateDefaultChildComponent();
            FormField fieldDef = FormField.Factory.create(getName(), "", null, null, false, comp.getClass(), null);
            comp.setup(getName(), null, fieldDef, getHelper());
            comp.getComponentMetadata().put("title", getName());
            // TODO: Provide a proper mechanism for setting path when creating components
            addComponent(getName(), comp);
            composite = false;
        } else {
            AnnotatedFieldDeserializer.getFormFields(clazz, getHelper()).forEach((name, component) -> addComponent(name, component));
            composite = true;
        }
        fieldComponents.values().forEach(this::addClientLibraries);
    }

    public void addComponent(String name, FieldComponent field) {
        fieldComponents.put(name, field);
        addClientLibraries(field);
    }

    protected AbstractResourceImpl generateItemsResource(String path, boolean useFieldSet) {
        SyntheticResourceBuilder rb = new SyntheticResourceBuilder(path + "/items", null);
        if (hasCategories(fieldComponents.values())) {
            AbstractGroupingContainerComponent groups = getGroupingContainer();
            groups.setPath(path + "/tabs");
            fieldComponents.forEach((name, component) -> groups.addComponent(component.getCategory(), name, component));
            rb.withChild(groups.buildComponentResource());
        } else if (useFieldSet) {
            FieldsetComponent fieldset = new FieldsetComponent();
            fieldComponents.forEach((name, comp) -> fieldset.addComponent(name, comp));
            fieldset.setPath(path + "/fields");
            fieldset.setHelper(getHelper());
            rb.withChild(fieldset.buildComponentResource());
        } else {
            for (FieldComponent component : fieldComponents.values()) {
                if (getHelper() != null) {
                    component.setHelper(getHelper());
                }
                component.setPath(path + "/items/" + component.getName());
                rb.withChild(component.buildComponentResource());
            }
        }
        AbstractResourceImpl items = rb.build();
        if (getHelper() != null) {
            items.setResourceResolver(getHelper().getRequest().getResourceResolver());
        }
        return items;
    }

    /**
     * Set the composite flag (generally you don't need to but in case you have to override the behavior for some reason)
     * @param val new value for composite flag
     */
    public void setComposite(boolean val) {
        composite = val;
    }

    /**
     * @return the composite
     */
    public boolean isComposite() {
        return composite;
    }

    public boolean hasCategories(Collection<FieldComponent> values) {
        return values.stream()
                .map(FieldComponent::getCategory)
                .filter(s -> s != null && !s.isEmpty())
                .distinct()
                .count() > 1;
    }

    /**
     * @param defaultChildComponent the defaultChildComponent to set
     */
    public void setDefaultChildComponent(Class<? extends FieldComponent> defaultChildComponent) {
        this.defaultChildComponent = defaultChildComponent;
    }

    /**
     * @return the dialogStyle
     */
    public DialogProvider.DialogStyle getDialogStyle() {
        return dialogStyle;
    }

    /**
     * @param dialogStyle the dialogStyle to set
     */
    public void setDialogStyle(DialogProvider.DialogStyle dialogStyle) {
        this.dialogStyle = dialogStyle;
    }

    /**
     * @return the propertiesTabName
     */
    public String getPropertiesTabName() {
        return propertiesTabName;
    }

    /**
     * @param propertiesTabName the propertiesTabName to set
     */
    public void setPropertiesTabName(String propertiesTabName) {
        this.propertiesTabName = propertiesTabName;
    }

    /**
     * @return the forceDotSlashPrefix
     */
    public boolean isForceDotSlashPrefix() {
        return forceDotSlashPrefix;
    }

    /**
     * @param forceDotSlashPrefix the forceDotSlashPrefix to set
     */
    public void setForceDotSlashPrefix(boolean forceDotSlashPrefix) {
        this.forceDotSlashPrefix = forceDotSlashPrefix;
    }
}
