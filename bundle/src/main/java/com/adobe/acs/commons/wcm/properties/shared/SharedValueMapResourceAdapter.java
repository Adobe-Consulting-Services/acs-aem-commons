/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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
package com.adobe.acs.commons.wcm.properties.shared;

import org.apache.sling.api.resource.ValueMap;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Sling Resource Adapter providing access to Shared Component Properties evaluated for a particular resource, in a type
 * that is friendly to the {@link org.apache.sling.api.adapter.SlingAdaptable} adapters cache.
 */
@ProviderType
public interface SharedValueMapResourceAdapter {

    /**
     * Get the global properties value map or {@link ValueMap#EMPTY}.
     *
     * @return the global properties value map or {@link ValueMap#EMPTY}.
     */
    ValueMap getGlobalProperties();

    /**
     * Get the shared properties for this resource type or {@link ValueMap#EMPTY}.
     *
     * @return the shared properties for this resource type or {@link ValueMap#EMPTY}.
     */
    ValueMap getSharedProperties();

    /**
     * Get the merged properties for the adaptable resource.
     *
     * @return the merged properties for the adaptable resource.
     */
    ValueMap getMergedProperties();
}
