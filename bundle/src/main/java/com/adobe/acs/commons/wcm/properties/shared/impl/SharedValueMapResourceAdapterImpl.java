/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2021 Adobe
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
package com.adobe.acs.commons.wcm.properties.shared.impl;

import com.adobe.acs.commons.wcm.properties.shared.SharedValueMapResourceAdapter;
import org.apache.sling.api.resource.ValueMap;

import java.util.Optional;

/**
 * Implementation of {@link SharedValueMapResourceAdapter}.
 */
public class SharedValueMapResourceAdapterImpl implements SharedValueMapResourceAdapter {

    private final ValueMap globalProperties;
    private final ValueMap sharedProperties;
    private final ValueMap mergedProperties;

    public SharedValueMapResourceAdapterImpl(final ValueMap globalProperties,
                                             final ValueMap sharedProperties,
                                             final ValueMap mergedProperties) {
        this.globalProperties = Optional.ofNullable(globalProperties).orElse(ValueMap.EMPTY);
        this.sharedProperties = Optional.ofNullable(sharedProperties).orElse(ValueMap.EMPTY);
        this.mergedProperties = Optional.ofNullable(mergedProperties).orElse(ValueMap.EMPTY);
    }

    @Override
    public ValueMap getGlobalProperties() {
        return globalProperties;
    }

    @Override
    public ValueMap getSharedProperties() {
        return sharedProperties;
    }

    @Override
    public ValueMap getMergedProperties() {
        return mergedProperties;
    }
}
