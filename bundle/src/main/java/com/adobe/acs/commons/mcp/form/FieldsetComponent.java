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
import org.osgi.annotation.versioning.ProviderType;

/**
 * Wrap a field set (section) component which is similar to container but also has a class attribute
 */
@ProviderType
public final class FieldsetComponent extends ContainerComponent {
    private static final String CLASS = "class";

    private String cssClass;

    public FieldsetComponent() {
        setResourceType("granite/ui/components/coral/foundation/form/fieldset");
    }

    @Override
    public void init() {
        super.init();
        getOption(CLASS).ifPresent(this::setCssClass);
    }

    @Override
    public Resource buildComponentResource() {
        getComponentMetadata().put(CLASS, getCssClass());
        return super.buildComponentResource();
    }

    /**
     * @return the cssClass
     */
    public String getCssClass() {
        return cssClass;
    }

    /**
     * @param cssClass the cssClass to set
     */
    public void setCssClass(String cssClass) {
        this.cssClass = cssClass;
    }
}
