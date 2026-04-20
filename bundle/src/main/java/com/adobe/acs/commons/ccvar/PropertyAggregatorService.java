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
 * Service used to aggregate property keys and values into a {@link Map}, given the context of a
 * {@link SlingHttpServletRequest}, that can be used to replace these tokens in content responses.
 */
public interface PropertyAggregatorService {

    /**
     * Uses a list of {@link ContentVariableProvider} classes to create a map of properties available for content
     * variable replacement to be used in supporting classes.
     *
     * @param request The currently scoped request
     * @return The map of properties
     */
    Map<String, Object> getProperties(SlingHttpServletRequest request);
}
