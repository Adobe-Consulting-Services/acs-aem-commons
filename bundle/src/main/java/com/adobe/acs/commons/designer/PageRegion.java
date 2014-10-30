/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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
package com.adobe.acs.commons.designer;

import aQute.bnd.annotation.ProviderType;

/**
 * Enum representing the possible target regions for a client library within an HTML page.
 *
 */
@ProviderType
public enum PageRegion {
    /**
     * The head section.
     */
    HEAD("head"),

    /**
     * The body section.
     */
    BODY("body");

    private String name;
    private PageRegion(final String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}