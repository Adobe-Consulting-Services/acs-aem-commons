/*
 * Copyright 2017 Adobe.
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
package com.adobe.acs.commons.mcp;

import org.apache.sling.api.resource.AbstractResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.api.wrappers.ValueMapDecorator;

/**
 * Describes a component in a manner which supports auto-generated forms
 */
public abstract class FieldComponent {
    private String name;
    protected FormField formField;
    protected SlingScriptHelper sling;
    private final ResourceMetadata componentMetadata = new ResourceMetadata();
    private String resourceType = "/libs/granite/ui/components/coral/foundation/form/textfield";
    private String resourceSuperType = "/libs/granite/ui/components/coral/foundation/form/field";


    public void init(String name, FormField field, SlingScriptHelper sling) {
        this.name = name;
        this.formField = field;
        this.sling = sling;
        componentMetadata.put("name", name);
        componentMetadata.put("required", field.required());
        componentMetadata.put("emptyText", field.description());
    }

    public SlingScriptHelper getHelper() {
        return sling;
    }
    
    public FormField getFieldDefinition() {
        return formField;
    }
    
    public String getHtml() {
        sling.include(buildComponentResource());
        return "";
    }

    public Resource buildComponentResource() {
        Resource res = new AbstractResource() {
            @Override
            public String getPath() {
                return "/not/a/real/path";
            }

            @Override
            public String getResourceType() {
                return resourceType;
            }

            @Override
            public String getResourceSuperType() {
                return resourceSuperType;
            }

            @Override
            public ResourceMetadata getResourceMetadata() {
                return getComponentMetadata();
            }
            
            @Override
            public ValueMap getValueMap() {
                return new ValueMapDecorator(getComponentMetadata());
            }

            @Override
            public ResourceResolver getResourceResolver() {
                return sling.getRequest().getResourceResolver();
            }
        };
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
}
