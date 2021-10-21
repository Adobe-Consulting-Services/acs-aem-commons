/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2018 Adobe
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
package com.adobe.acs.commons.models.injectors.impl;

import com.adobe.acs.commons.models.injectors.annotation.SharedValueMapValue;
import com.adobe.acs.commons.util.impl.ReflectionUtil;
import com.adobe.acs.commons.wcm.properties.shared.SharedComponentProperties;
import com.day.cq.commons.jcr.JcrConstants;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicyOption;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.apache.sling.models.spi.Injector;
import org.osgi.framework.Constants;

import javax.servlet.ServletRequest;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.adobe.acs.commons.models.injectors.impl.InjectorUtils.getResource;

/**
 * The SharedValueMapValueInjector depends on two other services related to script bindings executing first.
 * 1. The {@link com.adobe.acs.commons.wcm.properties.shared.impl.SharedComponentPropertiesBindingsValuesProvider}
 * defines bindings for "globalProperties", "sharedProperties", and "mergedProperties".
 * 2. The {@code org.apache.sling.models.impl.injectors.BindingsInjector} provides those
 */
@Component
@Service
@Property(name = Constants.SERVICE_RANKING, intValue = 4500)
public class SharedValueMapValueInjector implements Injector {

    /**
     * Bind if available, check for null when reading.
     */
    @Reference(policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL_UNARY)
    private SharedComponentProperties sharedComponentProperties;

    @Override
    public String getName() {
        return SharedValueMapValue.SOURCE;
    }

    @Override
    public Object getValue(Object adaptable, String name, Type declaredType, AnnotatedElement element, DisposalCallbackRegistry callbackRegistry) {
        // sanity check
        if (element.getAnnotation(SharedValueMapValue.class) == null) {
            return null;
        }

        final ValueMap valueMap = getValueMap(adaptable, element);

        if (valueMap != null) {
            return ReflectionUtil.convertValueMapValue(valueMap, name, declaredType);
        }

        return null;
    }

    private ValueMap getValueMap(Object adaptable, AnnotatedElement element) {
        final SharedComponentProperties.ValueTypes valueType = getValueType(element);
        if (valueType == null) {
            return null;
        }

        final Resource resource = getResource(adaptable);
        // we always need a resource, if only to determine if cached global properties are valid for the resource path
        if (resource == null) {
            return null;
        }

        // the root page path is used to test the validity of bindings
        final String rootPagePath = sharedComponentProperties.getSharedPropertiesPagePath(resource);
        if (rootPagePath == null) {
            // when we have a resource but no root page path, we can at least satisfy MERGED with the resource valuemap
            return valueType == SharedComponentProperties.ValueTypes.MERGED ? resource.getValueMap() : null;
        }

        // first attempt to retrieve from bindings
        final SlingBindings bindings = getBindings(adaptable);
        if (bindings != null) {
            if (valueType == SharedComponentProperties.ValueTypes.MERGED
                    && resource.getPath().equals(bindings.get(SharedComponentProperties.MERGED_PROPERTIES_PATH))) {
                return (ValueMap) bindings.get(SharedComponentProperties.MERGED_PROPERTIES);
            }

            if (rootPagePath.equals(bindings.get(SharedComponentProperties.SHARED_PROPERTIES_PAGE_PATH))) {
                final ValueMap globalVmBound = (ValueMap) bindings.get(SharedComponentProperties.GLOBAL_PROPERTIES);
                if (valueType == SharedComponentProperties.ValueTypes.GLOBAL) {
                    return globalVmBound;
                }

                final String sharedPropertiesPath = sharedComponentProperties.getSharedPropertiesPath(resource);
                if (valueType == SharedComponentProperties.ValueTypes.SHARED && sharedPropertiesPath == null) {
                    return null;
                }

                if (sharedPropertiesPath != null
                        && sharedPropertiesPath.equals(bindings.get(SharedComponentProperties.SHARED_PROPERTIES_PATH))) {
                    final ValueMap sharedVmBound = (ValueMap) bindings.get(SharedComponentProperties.SHARED_PROPERTIES);
                    if (valueType == SharedComponentProperties.ValueTypes.SHARED) {
                        return sharedVmBound;
                    } else if (valueType == SharedComponentProperties.ValueTypes.MERGED) {
                        return sharedComponentProperties.mergeProperties(globalVmBound, sharedVmBound, resource);
                    }
                } else if (sharedPropertiesPath == null && valueType == SharedComponentProperties.ValueTypes.MERGED) {
                    return sharedComponentProperties.mergeProperties(globalVmBound, null, resource);
                }
            }
        }

        switch (valueType) {
            case GLOBAL:
                return getGlobalProperties(rootPagePath, resource);
            case SHARED:
                return getSharedProperties(rootPagePath, resource);
            case MERGED:
                return getMergedProperties(rootPagePath, resource);
        }

        return null;
    }

    private SharedComponentProperties.ValueTypes getValueType(final AnnotatedElement element) {
        return element.getAnnotation(SharedValueMapValue.class).type();
    }

    private SlingBindings getBindings(Object adaptable) {
        if (adaptable instanceof ServletRequest) {
            ServletRequest request = (ServletRequest) adaptable;
            return (SlingBindings) request.getAttribute(SlingBindings.class.getName());
        } else {
            return null;
        }
    }

    /**
     * Get shared properties ValueMap the current resource.
     */
    protected ValueMap getSharedProperties(String rootPagePath, Resource resource) {
        return Optional.ofNullable(sharedComponentProperties.getSharedPropertiesPath(resource))
                .map(resource.getResourceResolver()::getResource)
                .map(Resource::getValueMap)
                .orElse(ValueMap.EMPTY);
    }

    /**
     * Get global properties ValueMap for the current resource.
     */
    protected ValueMap getGlobalProperties(String rootPagePath, Resource resource) {
        return Optional.ofNullable(sharedComponentProperties.getGlobalPropertiesPath(resource))
                .map(resource.getResourceResolver()::getResource)
                .map(Resource::getValueMap)
                .orElse(ValueMap.EMPTY);
    }

    /**
     * Get merged properties ValueMap for the current resource.
     */
    protected ValueMap getMergedProperties(String rootPagePath, Resource resource) {
        return sharedComponentProperties.mergeProperties(
                getGlobalProperties(rootPagePath, resource),
                getSharedProperties(rootPagePath, resource),
                resource);
    }

}
