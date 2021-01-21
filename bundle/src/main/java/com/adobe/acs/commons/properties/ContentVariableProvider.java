/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2020 Adobe
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
package com.adobe.acs.commons.properties;

import com.day.cq.wcm.api.Page;

import java.util.Map;

/**
 *
 */
public interface ContentVariableProvider {

    /**
     * Method to add the properties to the passed map. This will be overridden to run custom logic and add properties to
     * the Map.
     *
     * @param map  The current set of properties (may or may not be empty)
     * @param page The page used as the context to add properties. Typically the page currently being requested.
     */
    void addProperties(Map<String, Object> map, Page page);

    /**
     * Determines whether or not the current ContentVariableProvider will accept the request. This can limit what
     * providers to use and allow for more contextual content variables in a multi-tenant situation.
     *
     * @param page The current page from the request
     * @return Whether the ContentVariableProvider should add variables to the property map
     */
    boolean accepts(Page page);
}
