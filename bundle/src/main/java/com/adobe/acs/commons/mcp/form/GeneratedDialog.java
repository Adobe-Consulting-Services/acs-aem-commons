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
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;

/**
 * Generates a dialog out of @FormField annotations Ideally your sling model
 * should extend this class to inherit its features but you can also just use
 * the @DialogProvider annotation
 */
@Model(
        adaptables = {SlingHttpServletRequest.class},
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
@DialogProvider
public class GeneratedDialog {

    @Inject
    @JsonIgnore
    private Resource resource;

    @Inject
    @JsonIgnore
    private SlingHttpServletRequest request;

    @Inject
    @JsonIgnore
    SlingScriptHelper sling;

    @JsonIgnore
    private FormComponent form;

    @JsonIgnore
    protected Map<String, FieldComponent> fieldComponents;

    @JsonIgnore
    private String formTitle = null;

    DialogProvider providerAnnotation = null;

    @PostConstruct
    public void init() {
        if (getResource() == null && getRequest() != null) {
            resource = getRequest().getResource();
        }
        getFieldComponents();
    }

    public void initAnnotationValues(DialogProvider annotation) {
        if (annotation == null) {
            return;
        }
        if (StringUtils.isNotBlank(annotation.title())) {
            setFormTitle(annotation.title());
        }
        providerAnnotation = annotation;
    }

    @JsonIgnore
    public Map<String, FieldComponent> getFieldComponents() {
        if (fieldComponents == null) {
            fieldComponents = AnnotatedFieldDeserializer.getFormFields(getClass(), getSlingHelper());
        }
        return fieldComponents;
    }

    @JsonIgnore
    public Collection<String> getAllClientLibraries() {
        return getClientLibraries(FieldComponent.ClientLibraryType.ALL);
    }

    @JsonIgnore
    public Collection<String> getCssClientLibraries() {
        return getClientLibraries(FieldComponent.ClientLibraryType.CSS);
    }

    @JsonIgnore
    public Collection<String> getJsClientLibraries() {
        return getClientLibraries(FieldComponent.ClientLibraryType.JS);
    }

    private Collection<String> getClientLibraries(FieldComponent.ClientLibraryType type) {
        LinkedHashSet<String> allLibraries = new LinkedHashSet<>();
        fieldComponents.values().stream()
                .map(c -> c.getClientLibraryCategories().get(type))
                .filter(v -> v != null)
                .forEach(v -> allLibraries.addAll(v));
        return allLibraries;
    }

    /**
     * @return the resource
     */
    @JsonIgnore
    public Resource getResource() {
        return resource;
    }

    /**
     * @return the request
     */
    @JsonIgnore
    public SlingHttpServletRequest getRequest() {
        return request;
    }

    /**
     * @return the sling helper
     */
    @JsonIgnore
    public SlingScriptHelper getSlingHelper() {
        return sling;
    }

    @JsonIgnore
    public Resource getFormResource() {
        return getForm().buildComponentResource();
    }

    @JsonIgnore
    public FormComponent getForm() {
        if (form == null) {
            form = new FormComponent();
            if (providerAnnotation != null) {
                form.applyDialogProviderSettings(providerAnnotation);
            }
            if (formTitle != null) {
                form.getComponentMetadata().put("jcr:title", formTitle);
            }
            if (sling != null) {
                form.setHelper(sling);
                form.setPath(sling.getRequest().getResource().getPath());
                form.setAsync(true);
                form.getComponentMetadata().put("granite:id", "mcp-generated-form");
            } else {
                form.setPath("/form");
            }
            getFieldComponents().forEach((name, component) -> form.addComponent(name, component));
        }
        return form;
    }

    /**
     * @return the formTitle
     */
    public String getFormTitle() {
        return formTitle;
    }

    /**
     * @param formTitle the formTitle to set
     */
    public void setFormTitle(String formTitle) {
        this.formTitle = formTitle;
    }
}
