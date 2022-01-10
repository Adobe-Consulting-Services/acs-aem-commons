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
package com.adobe.acs.commons.wcm.properties.shared.impl;

import com.adobe.acs.commons.wcm.properties.shared.SharedComponentProperties;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicyOption;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.scripting.api.BindingsValuesProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Bindings;

/**
 * Bindings Values Provider that adds bindings for globalProperties,
 * sharedProperties, and mergedProperties maps.
 * <p>
 * globalProperties contains the shared properties accessible by
 * all components.
 * <p>
 * sharedProperties contains the shared properties specific to the
 * current component.
 * <p>
 * mergedProperties is a merge of the instance-level, shared, and
 * global properties for the current component, giving preference
 * to instance-level values, then shared values, and finally global
 * values when properties exist at multiple levels with the same name.
 */
@Component
@Service
public class SharedComponentPropertiesBindingsValuesProvider implements BindingsValuesProvider {
    private static final Logger log = LoggerFactory.getLogger(SharedComponentPropertiesBindingsValuesProvider.class);

    /**
     * Bind if available, check for null when reading.
     */
    @Reference(policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL_UNARY)
    SharedComponentProperties sharedComponentProperties;

    @Override
    public void addBindings(final Bindings bindings) {
        final SlingHttpServletRequest request = (SlingHttpServletRequest) bindings.get(SlingBindings.REQUEST);
        final Resource resource = (Resource) bindings.get(SlingBindings.RESOURCE);
        if (request != null && resource != null) {
            final SharedPropertiesRequestCache cache = SharedPropertiesRequestCache.fromRequest(request);
            if (sharedComponentProperties != null) {
                setSharedProperties(bindings, resource, cache);
            } else {
                log.debug("Shared Component Properties must be configured to enable this provider");
            }
        }
        setDefaultBindings(bindings, resource);
    }

    private void setSharedProperties(final Bindings bindings,
                                     final Resource resource,
                                     final SharedPropertiesRequestCache cache) {
        String rootPagePath = sharedComponentProperties.getSharedPropertiesPagePath(resource);
        if (rootPagePath != null) {
            // set this value even when global or shared resources are not found to indicate cache validity downstream
            bindings.put(SharedComponentProperties.SHARED_PROPERTIES_PAGE_PATH, rootPagePath);
            String globalPropsPath = sharedComponentProperties.getGlobalPropertiesPath(resource);
            if (globalPropsPath != null) {
                bindings.putAll(cache.getBindings(globalPropsPath, (newBindings) -> {
                    final Resource globalPropsResource = resource.getResourceResolver().getResource(globalPropsPath);
                    if (globalPropsResource != null) {
                        newBindings.put(SharedComponentProperties.GLOBAL_PROPERTIES, globalPropsResource.getValueMap());
                        newBindings.put(SharedComponentProperties.GLOBAL_PROPERTIES_RESOURCE, globalPropsResource);
                    }
                }));
            }

            final String sharedPropsPath = sharedComponentProperties.getSharedPropertiesPath(resource);
            if (sharedPropsPath != null) {
                bindings.putAll(cache.getBindings(sharedPropsPath, (newBindings) -> {
                    Resource sharedPropsResource = resource.getResourceResolver().getResource(sharedPropsPath);
                    if (sharedPropsResource != null) {
                        newBindings.put(SharedComponentProperties.SHARED_PROPERTIES, sharedPropsResource.getValueMap());
                        newBindings.put(SharedComponentProperties.SHARED_PROPERTIES_RESOURCE, sharedPropsResource);
                    }
                }));
                bindings.put(SharedComponentProperties.SHARED_PROPERTIES_PATH, sharedPropsPath);
            }

            final String mergedPropertiesPath = resource.getPath();
            bindings.putAll(cache.getBindings(mergedPropertiesPath, (newBindings) -> {
                ValueMap globalPropertyMap = (ValueMap) bindings.get(SharedComponentProperties.GLOBAL_PROPERTIES);
                ValueMap sharedPropertyMap = (ValueMap) bindings.get(SharedComponentProperties.SHARED_PROPERTIES);
                newBindings.put(SharedComponentProperties.MERGED_PROPERTIES,
                        sharedComponentProperties.mergeProperties(globalPropertyMap, sharedPropertyMap, resource));
            }));
            // set this value to indicate cache validity downstream
            bindings.put(SharedComponentProperties.MERGED_PROPERTIES_PATH, resource.getPath());
        }
    }

    private void setDefaultBindings(final Bindings bindings,
                                    final Resource resource) {
        if (!bindings.containsKey(SharedComponentProperties.GLOBAL_PROPERTIES)) {
            bindings.put(SharedComponentProperties.GLOBAL_PROPERTIES, ValueMap.EMPTY);
        }
        if (!bindings.containsKey(SharedComponentProperties.SHARED_PROPERTIES)) {
            bindings.put(SharedComponentProperties.SHARED_PROPERTIES, ValueMap.EMPTY);
        }
        if (!bindings.containsKey(SharedComponentProperties.MERGED_PROPERTIES)) {
            bindings.put(SharedComponentProperties.MERGED_PROPERTIES,
                    resource == null ? ValueMap.EMPTY : resource.getValueMap());
        }
    }

}
