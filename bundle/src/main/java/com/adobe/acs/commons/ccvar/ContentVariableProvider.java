/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
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
package com.adobe.acs.commons.ccvar;

import org.apache.sling.api.SlingHttpServletRequest;

import java.util.Map;

/**
 * Extensible interface allowing pluggable content variables to be used in the {@link PropertyAggregatorService} to allow
 * for replacement in content responses.
 */
public interface ContentVariableProvider {

    /**
     * Method to add the properties to the passed map. This will be overridden to run custom logic and add properties to
     * the Map.
     *
     * @param map     The current set of properties (may or may not be empty)
     * @param request The request used as the context to add properties.
     */
    void addProperties(Map<String, Object> map, SlingHttpServletRequest request);

    /**
     * Determines whether or not the current ContentVariableProvider will accept the request. This can limit what
     * providers to use and allow for more contextual content variables in a multi-tenant situation.
     *
     * @param request The current request
     * @return Whether the ContentVariableProvider should add variables to the property map
     */
    boolean accepts(SlingHttpServletRequest request);
}
