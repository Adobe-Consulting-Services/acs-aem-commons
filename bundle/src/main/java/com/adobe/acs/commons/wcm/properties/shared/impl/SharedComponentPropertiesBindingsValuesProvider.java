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

import com.adobe.acs.commons.wcm.PageRootProvider;
import com.adobe.acs.commons.wcm.properties.shared.SharedComponentProperties;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.components.Component;
import com.day.cq.wcm.commons.WCMUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.scripting.api.BindingsValuesProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Bindings;
import java.util.HashMap;
import java.util.Map;

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
@org.apache.felix.scr.annotations.Component
@Service
public class SharedComponentPropertiesBindingsValuesProvider implements BindingsValuesProvider {
    private static final Logger log = LoggerFactory.getLogger(SharedComponentPropertiesBindingsValuesProvider.class);

    @Reference
    private PageRootProvider pageRootProvider;

    @Reference
    private SharedComponentProperties sharedComponentProperties;

    @Override
    public void addBindings(Bindings bindings) {
        Resource resource = (Resource) bindings.get("resource");
        Component component = WCMUtils.getComponent(resource);
        if (component != null) {
            if (pageRootProvider != null) {
                setSharedProperties(bindings, resource, component);
            } else {
                log.debug("Page Root Provider must be configured for shared component properties to be supported");
            }
            setMergedProperties(bindings, resource);
        }
    }

    private void setSharedProperties(Bindings bindings, Resource resource, Component component) {
        Page pageRoot = pageRootProvider.getRootPage(resource);
        if (pageRoot != null) {
            String globalPropsPath = pageRoot.getPath() + "/jcr:content/" + SharedComponentProperties.NN_GLOBAL_COMPONENT_PROPERTIES;
            Resource globalPropsResource = resource.getResourceResolver().getResource(globalPropsPath);
            if (globalPropsResource != null) {
                bindings.put(SharedComponentProperties.GLOBAL_PROPERTIES, globalPropsResource.getValueMap());
                bindings.put(SharedComponentProperties.GLOBAL_PROPERTIES_RESOURCE, globalPropsResource);
            }

            String sharedPropsPath = pageRoot.getPath() + "/jcr:content/" + SharedComponentProperties.NN_SHARED_COMPONENT_PROPERTIES +  "/"
                    + component.getResourceType();
            Resource sharedPropsResource = resource.getResourceResolver().getResource(sharedPropsPath);
            if (sharedPropsResource != null) {
                bindings.put(SharedComponentProperties.SHARED_PROPERTIES, sharedPropsResource.getValueMap());
                bindings.put(SharedComponentProperties.SHARED_PROPERTIES_RESOURCE, sharedPropsResource);
            }
        } else {
            log.debug("Could not determine shared properties root for resource {}", resource.getPath());
        }
    }

    private void setMergedProperties(Bindings bindings, Resource resource) {
        ValueMap globalPropertyMap = (ValueMap) bindings.get(SharedComponentProperties.GLOBAL_PROPERTIES);
        ValueMap sharedPropertyMap = (ValueMap) bindings.get(SharedComponentProperties.SHARED_PROPERTIES);
        ValueMap localPropertyMap = resource.getValueMap();

        bindings.put(SharedComponentProperties.MERGED_PROPERTIES, mergeProperties(localPropertyMap, sharedPropertyMap, globalPropertyMap));
    }

    private ValueMap mergeProperties(ValueMap instanceProperties, ValueMap sharedProperties, ValueMap globalProperties) {
        Map<String, Object> mergedProperties = new HashMap<String, Object>();

        // Add Component Global Configs
        if (globalProperties != null) {
            mergedProperties.putAll(globalProperties);
        }

        // Add Component Shared Configs
        if (sharedProperties != null) {
            mergedProperties.putAll(sharedProperties);
        }

        // Merge in the Component Local Configs
        if (instanceProperties != null) {
            mergedProperties.putAll(instanceProperties);
        }

        return new ValueMapDecorator(mergedProperties);
    }
}
