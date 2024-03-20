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
package com.adobe.acs.commons.redirects;

import org.apache.sling.api.SlingHttpServletRequest;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * This service interface provides a hook into the RedirectFilter, and allow for further adjustment
 * of the Location header
 */
@ConsumerType
public interface LocationHeaderAdjuster {

    /**
     * Allows for custom adjustment of the Location header after its been parsed and resourceResolver.map(..)'d,
     * but before it's been dispatched.
     *
     * @param request the request
     * @param location the Location header passed through resourceResolver.map(..)
     * @return the location to try to resolve
     */
    String adjust(SlingHttpServletRequest request, String location);
}
