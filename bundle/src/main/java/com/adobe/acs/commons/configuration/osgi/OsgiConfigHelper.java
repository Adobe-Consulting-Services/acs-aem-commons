/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
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

package com.adobe.acs.commons.configuration.osgi;

import org.apache.sling.api.resource.Resource;

public interface OsgiConfigHelper {
    /**
     * Creates the OSGi Config pid based on the acs.pid and acs.configurationType properties on the resource.
     *
     * @param resource The resource representing the configuration
     * @return The OSGi config pid name
     */
    String getPID(Resource resource);

    /**
     * Checks of the resource has the required properties set based on the acs.requiredProperties field.
     * All required properties must evaluate to a non-blank string (non-null, non-all spaces).
     *
     * @param resource The resource to evaluate
     * @param requiredProperties list of properties to check for non-blank state
     * @return true is the resource has non-blank values for all properties listed in requiredProperties, else false
     */
    boolean hasRequiredProperties(Resource resource, String[] requiredProperties);
}
