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

package com.adobe.acs.commons.synth.impl;

import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Better SyntheticResource
 */
public class SynthesizedResource extends SyntheticResource {

    private ValueMap synthProperties = new ValueMapDecorator(new HashMap<String, Object>());

    public SynthesizedResource(ResourceResolver resourceResolver, String path, String resourceType, Map<String, Object> properties) {
        super(resourceResolver, path, resourceType);

        putProperties(properties);
    }

    public SynthesizedResource(ResourceResolver resourceResolver, ResourceMetadata rm, String resourceType, Map<String, Object> properties) {
        super(resourceResolver, rm, resourceType);

        putProperties(properties);
    }

    public SynthesizedResource putProperty(String key, Object value) {
        synthProperties.put(key, value);

        return this; // yeah, i like fluent APIs
    }

    public SynthesizedResource putProperties(Map<String, Object> properties) {
        synthProperties.putAll(properties);

        return this; // yeah, i like fluent APIs
    }

    @Override
    public ValueMap getValueMap() {
        ValueMap superMap = super.getValueMap();

        for (Map.Entry<String, Object> entry : superMap.entrySet()) {
            if (!synthProperties.containsKey(entry.getKey())) {
                synthProperties.put(entry.getKey(), entry.getValue());
            }
        }

        return synthProperties;
    }

}
