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
package com.adobe.acs.commons.wcm.properties.shared;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.annotation.versioning.ProviderType;

@ProviderType
@SuppressWarnings("squid:S1214")
public interface SharedComponentProperties {
    /**
     * Bindings key for the root page path containing the shared and global properties resources
     */
    String SHARED_PROPERTIES_PAGE_PATH = "sharedPropertiesPagePath";
    /**
     * Bindings key for the resource path evaluated for shared properties
     */
    String SHARED_PROPERTIES_PATH = "sharedPropertiesPath";
    /**
     * Bindings key for the resource path evaluated for merged properties
     */
    String MERGED_PROPERTIES_PATH = "mergedPropertiesPath";
    String SHARED_PROPERTIES = "sharedProperties";
    String GLOBAL_PROPERTIES = "globalProperties";
    String MERGED_PROPERTIES = "mergedProperties";

    String SHARED_PROPERTIES_RESOURCE = SHARED_PROPERTIES + "Resource";
    String GLOBAL_PROPERTIES_RESOURCE = GLOBAL_PROPERTIES + "Resource";

    String NN_GLOBAL_COMPONENT_PROPERTIES = "global-component-properties";
    String NN_SHARED_COMPONENT_PROPERTIES = "shared-component-properties";

    enum ValueTypes {
        SHARED,
        GLOBAL,
        MERGED
    }

    /**
     * Construct an absolute root page path containing the shared and global properties resources appropriate for the
     * given component resource.
     *
     * @param resource the component resource to evaluate
     * @return an absolute path to a parent resource for shared and global properties
     */
    String getSharedPropertiesPagePath(Resource resource);

    /**
     * Construct an absolute resource path for retrieval of a global component properties value map.
     *
     * @param resource the resource to evaluate
     * @return an absolute path to a possible global component properties resource or null
     */
    String getGlobalPropertiesPath(Resource resource);

    /**
     * Get the global properties of the current resource, or an empty map.
     *
     * @param resource the current resource
     * @return global properties or empty
     */
    ValueMap getGlobalProperties(Resource resource);

    /**
     * Construct an absolute resource path for retrieval of a shared component properties value map.
     *
     * @param resource the resource to evaluate
     * @return an absolute path to a possible shared component properties resource or null
     */
    String getSharedPropertiesPath(Resource resource);

    /**
     * Get the shared component properties of the current resource, or an empty map.
     *
     * @param resource the current resource
     * @return shared component properties or empty
     */
    ValueMap getSharedProperties(Resource resource);

    /**
     * Merge global and shared properties into the valuemap retrieved from the provided resource.
     *
     * @param globalProperties global properties or null
     * @param sharedProperties shared component properties or null
     * @param resource         the component resource
     * @return merged properties
     */
    ValueMap mergeProperties(ValueMap globalProperties, ValueMap sharedProperties, Resource resource);
}
