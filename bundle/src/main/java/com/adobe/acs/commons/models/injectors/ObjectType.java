/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2014 Adobe
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
package com.adobe.acs.commons.models.injectors;

/**
 * Enumeration which encapsulated the available objects.
 */
enum ObjectType {

    RESOURCE("resource"),
    RESOURCE_RESOLVER("resourceResolver"),
    COMPONENT_CONTEXT("componentContext"),
    PAGE_MANAGER("pageManager"),
    CURRENT_PAGE("currentPage"),
    RESOURCE_PAGE("resourcePage"),
    DESIGNER("designer"),
    CURRENT_DESIGN("currentDesign"),
    RESOURCE_DESIGN("resourceDesign"),
    CURRENT_STYLE("currentStyle"),
    SESSION("session"),
    XSS_API("xssApi");

    private String text;

    ObjectType(String text) {
        this.text = text;
    }

    public String getText() {
        return this.text;
    }

    public static ObjectType fromString(String text) {
        if (text != null) {
            for (ObjectType b : ObjectType.values()) {
                if (text.equalsIgnoreCase(b.text)) {
                    return b;
                }
            }
        }
        return null;
    }
}
