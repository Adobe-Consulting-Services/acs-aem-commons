/*
 * Copyright 2019 Adobe.
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
package com.adobe.acs.commons.mcp.form;

import org.apache.sling.api.resource.Resource;

/**
 * Simplest possible container with optional title property
 */
public class ContainerComponent extends AbstractContainerComponent {
    public static final String JCR_TITLE = "jcr:title";

    private String title;

    public ContainerComponent() {
        setResourceType("granite/ui/components/foundation/container");
    }

    @Override
    public void init() {
        super.init();
        if (getTitle() == null) {
            getOption(JCR_TITLE).ifPresent(this::setTitle);
        }
    }

    @Override
    public Resource buildComponentResource() {
        if (getTitle() != null) {
            getComponentMetadata().put(JCR_TITLE, getTitle());
        }
        AbstractResourceImpl res = new AbstractResourceImpl(getPath(), getResourceType(), getResourceSuperType(), getComponentMetadata());
        if (sling != null) {
            res.setResourceResolver(sling.getRequest().getResourceResolver());
        }
        res.addChild(generateItemsResource(getPath() + "/items", false));
        return res;
    }

    /**
     * @return the jcrTitle
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title the jcrTitle to set
     */
    public void setTitle(String title) {
        this.title = title;
    }
}
