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
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicyOption;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.apache.sling.models.spi.Injector;
import org.osgi.framework.Constants;

import javax.servlet.ServletRequest;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.Optional;

import static com.adobe.acs.commons.models.injectors.impl.InjectorUtils.getResource;

/**
 * The SharedValueMapValueInjector depends on
 * {@link com.adobe.acs.commons.wcm.properties.shared.impl.SharedComponentPropertiesBindingsValuesProvider} to execute
 * first.
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
    public Object getValue(Object adaptable, String name, Type declaredType,
                           AnnotatedElement element, DisposalCallbackRegistry callbackRegistry) {
        // sanity check
        if (element.getAnnotation(SharedValueMapValue.class) == null) {
            return null;
        }

        final ValueMap valueMap;
        if (sharedComponentProperties != null) {
            valueMap = getValueMap(adaptable, element);
        } else {
            valueMap = Optional.ofNullable(getResource(adaptable))
                    .map(Resource::getValueMap)
                    .orElse(ValueMap.EMPTY);
        }

        return ReflectionUtil.convertValueMapValue(valueMap, name, declaredType);
    }

    private ValueMap getValueMap(Object adaptable, AnnotatedElement element) {
        final SharedComponentProperties.ValueTypes valueType = getValueType(element);
        if (valueType == null) {
            return ValueMap.EMPTY;
        }

        final Resource resource = getResource(adaptable);
        // we always need a resource, if only to determine if cached global properties are valid for the resource path
        if (resource == null) {
            return ValueMap.EMPTY;
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
            final ValueMap fromBindings = getValueMapFromBindings(bindings, valueType, resource, rootPagePath);
            if (fromBindings != null) {
                return fromBindings;
            }
        }

        switch (valueType) {
            case GLOBAL:
                return getGlobalProperties(resource);
            case SHARED:
                return getSharedProperties(resource);
            case MERGED:
                return getMergedProperties(resource);
            default:
                return ValueMap.EMPTY;
        }
    }

    ValueMap getValueMapFromBindings(final SlingBindings bindings,
                                     final SharedComponentProperties.ValueTypes valueType,
                                     final Resource resource,
                                     final String rootPagePath) {
        // if the merged path in bindings matches the resource path, just assume that the merged properties in
        // bindings are sufficient
        if (valueType == SharedComponentProperties.ValueTypes.MERGED
                && resource.getPath().equals(bindings.get(SharedComponentProperties.MERGED_PROPERTIES_PATH))) {
            return Optional.ofNullable((ValueMap) bindings.get(SharedComponentProperties.MERGED_PROPERTIES))
                    .orElse(ValueMap.EMPTY);
        }

        // next check that the root page path matches the shared properties page path in bindings
        // this might not match if a request wrapper was constructed for a non-child resource of the original request
        // but which was not subjected to its own script bindings phase
        if (!rootPagePath.equals(bindings.get(SharedComponentProperties.SHARED_PROPERTIES_PAGE_PATH))) {
            // return null to indicate that the Injector should construct the value map
            return null;
        }

        final ValueMap globalVmBound = (ValueMap) bindings.get(SharedComponentProperties.GLOBAL_PROPERTIES);
        // if GLOBAL is requested, just return it from bindings
        if (valueType == SharedComponentProperties.ValueTypes.GLOBAL) {
            return Optional.ofNullable(globalVmBound).orElse(ValueMap.EMPTY);
        }

        final String sharedPropertiesPath = sharedComponentProperties.getSharedPropertiesPath(resource);
        // it is possible for getSharedPropertiesPath to return null for a resource if its resource type is invalid
        // i.e., String.EMPTY, sling:nonexisting, nt:unstructured, or /var/absolute/path/not/in/resolver/search/path
        if (sharedPropertiesPath == null) {
            // if SHARED is requested in this case, return ValueMap.EMPTY
            if (valueType == SharedComponentProperties.ValueTypes.SHARED) {
                return ValueMap.EMPTY;
            } else { // otherwise, pass null for sharedProperties and merge with global and resource
                return sharedComponentProperties.mergeProperties(globalVmBound, null, resource);
            }
        }

        // it is also possible for the shared properties path to differ from the path in bindings for the same
        // reason that shared properties page path may differ (a wrapped request for a child resource of a
        // different resource type)
        final ValueMap sharedVmBound;
        if (sharedPropertiesPath.equals(bindings.get(SharedComponentProperties.SHARED_PROPERTIES_PATH))) {
            sharedVmBound = (ValueMap) bindings.get(SharedComponentProperties.SHARED_PROPERTIES);
        } else {
            sharedVmBound = Optional.ofNullable(resource.getResourceResolver().getResource(sharedPropertiesPath))
                    .map(Resource::getValueMap).orElse(ValueMap.EMPTY);
        }

        // now that shared properties have been retrieved either return them directly if SHARED is requested
        if (valueType == SharedComponentProperties.ValueTypes.SHARED) {
            return Optional.ofNullable(sharedVmBound).orElse(ValueMap.EMPTY);
        } else { // or return everything merged
            return sharedComponentProperties.mergeProperties(globalVmBound, sharedVmBound, resource);
        }
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
    protected ValueMap getSharedProperties(Resource resource) {
        return Optional.ofNullable(sharedComponentProperties.getSharedPropertiesPath(resource))
                .map(resource.getResourceResolver()::getResource)
                .map(Resource::getValueMap)
                .orElse(ValueMap.EMPTY);
    }

    /**
     * Get global properties ValueMap for the current resource.
     */
    protected ValueMap getGlobalProperties(Resource resource) {
        return Optional.ofNullable(sharedComponentProperties.getGlobalPropertiesPath(resource))
                .map(resource.getResourceResolver()::getResource)
                .map(Resource::getValueMap)
                .orElse(ValueMap.EMPTY);
    }

    /**
     * Get merged properties ValueMap for the current resource.
     */
    protected ValueMap getMergedProperties(Resource resource) {
        return sharedComponentProperties.mergeProperties(
                getGlobalProperties(resource),
                getSharedProperties(resource),
                resource);
    }

}
