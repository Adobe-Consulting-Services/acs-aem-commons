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

import aQute.bnd.annotation.ProviderType;

@ProviderType
@SuppressWarnings("squid:S1214")
public interface SharedComponentProperties {
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
}
