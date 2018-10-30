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

import aQute.bnd.annotation.ProviderType;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.scripting.SlingScriptHelper;

/**
 * Describes a component in a manner which supports auto-generated forms
 */
@ProviderType
public abstract class FieldComponent {
    private String name;
    protected FormField formField;
    protected Field javaField;
    protected SlingScriptHelper sling;
    private final ResourceMetadata componentMetadata = new ResourceMetadata();
    private String resourceType = "granite/ui/components/coral/foundation/form/textfield";
    private String resourceSuperType = "granite/ui/components/coral/foundation/form/field";
    private Resource resource;
    private String path = "/fake/path";

    public final void setup(String name, Field javaField, FormField field, SlingScriptHelper sling) {
        this.name = name;
        this.formField = field;
        this.sling = sling;
        this.javaField = javaField;
        componentMetadata.put("name", name);
        componentMetadata.put("fieldLabel", formField.name());
        componentMetadata.put("fieldDescription", formField.description());
        componentMetadata.put("required", formField.required());
        componentMetadata.put("emptyText", formField.hint());
        getOption("default").ifPresent(val->componentMetadata.put("value", val));
        init();
    }
    
    public abstract void init();
    
    public SlingScriptHelper getHelper() {
        return sling;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public String getPath() {
        return path;
    }
    
    public Field getField() {
        return javaField;
    }
    
    public FormField getFieldDefinition() {
        return formField;
    }
    
    public String getHtml() {
        sling.include(getComponentResource());
        return "";
    }

    private Resource getComponentResource() {
        if (resource == null) {
            resource = buildComponentResource();
        }
        return resource;
    }

    /**
     * If your component needs child nodes then override this method, call the superclass implementation, and then use addChildren
     * to add additional nodes to it.
     * @return 
     */
    public Resource buildComponentResource() {
        AbstractResourceImpl res = new AbstractResourceImpl(path, resourceType, resourceSuperType, componentMetadata);
        if (sling != null && sling.getRequest() != null) {
            res.setResourceResolver(sling.getRequest().getResourceResolver());
        }
        return res;
    }

    /**
     * @return the componentMetadata
     */
    public ResourceMetadata getComponentMetadata() {
        return componentMetadata;
    }

    /**
     * @return the resourceType
     */
    public String getResourceType() {
        return resourceType;
    }

    /**
     * @param resourceType the resourceType to set
     */
    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    /**
     * @return the resourceSuperType
     */
    public String getResourceSuperType() {
        return resourceSuperType;
    }

    /**
     * @param resourceSuperType the resourceSuperType to set
     */
    public void setResourceSuperType(String resourceSuperType) {
        this.resourceSuperType = resourceSuperType;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    public boolean hasOption(String optionName) {
        return Stream.of(formField.options())
                .filter(s -> s.equalsIgnoreCase(optionName) || s.startsWith(optionName + "="))
                .findFirst().isPresent();
    }
    
    public Optional<String> getOption(String option) {
        return Stream.of(formField.options())
                .filter(s -> s.startsWith(option+"="))
                .findFirst().map(o -> o.split("=")[1]);
    }
}
