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
import org.apache.sling.api.scripting.LazyBindings;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.scripting.api.BindingsValuesProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Bindings;
import java.util.Optional;
import java.util.function.Supplier;

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

    /**
     * This method ensures that the provided supplier is appropriately typed for insertion into a SlingBindings
     * object. It primarily facilitates lambda type inference (i.e., {@code wrapSupplier(() -> something)} forces
     * inference to the functional interface type of the method parameter).
     *
     * @param supplier the provided supplier
     * @return the Supplier as a LazyBindings.Supplier
     */
    protected LazyBindings.Supplier wrapSupplier(final Supplier<?> supplier) {
        return () -> supplier != null ? supplier.get() : null;
    }

    /**
     * Check if provided {@code bindings} is an instance of {@link LazyBindings}.
     *
     * @param bindings the parameter from {@link #addBindings(Bindings)}
     * @return true if bindings implements LazyBindings
     */
    private boolean isLazy(Bindings bindings) {
        return bindings instanceof LazyBindings;
    }

    /**
     * Injects Global SCP keys into the provided bindings in one of two ways:
     * 1. lazily, if {@code bindings} is an instance of {@code LazyBindings}
     * 2. immediately, for all other kinds of {@code Bindings}
     *
     * @param bindings the bindings
     * @param supplier a global SCP resource supplier
     */
    protected void injectGlobalProps(Bindings bindings, Supplier<Optional<Resource>> supplier) {
        if (isLazy(bindings)) {
            bindings.put(SharedComponentProperties.GLOBAL_PROPERTIES_RESOURCE,
                    wrapSupplier(() -> supplier.get().orElse(null)));
            bindings.put(SharedComponentProperties.GLOBAL_PROPERTIES,
                    wrapSupplier(() -> supplier.get().map(Resource::getValueMap).orElse(null)));
        } else {
            supplier.get().ifPresent(value -> {
                bindings.put(SharedComponentProperties.GLOBAL_PROPERTIES_RESOURCE, value);
                bindings.put(SharedComponentProperties.GLOBAL_PROPERTIES, value.getValueMap());
            });
        }
    }

    /**
     * Injects Shared SCP keys into the provided bindings in one of two ways:
     * 1. lazily, if {@code bindings} is an instance of {@code LazyBindings}
     * 2. immediately, for all other kinds of {@code Bindings}
     *
     * @param bindings the bindings
     * @param supplier a shared SCP resource supplier
     */
    protected void injectSharedProps(Bindings bindings, Supplier<Optional<Resource>> supplier) {
        if (isLazy(bindings)) {
            bindings.put(SharedComponentProperties.SHARED_PROPERTIES_RESOURCE,
                    wrapSupplier(() -> supplier.get().orElse(null)));
            bindings.put(SharedComponentProperties.SHARED_PROPERTIES,
                    wrapSupplier(() -> supplier.get().map(Resource::getValueMap).orElse(null)));
        } else {
            supplier.get().ifPresent(value -> {
                bindings.put(SharedComponentProperties.SHARED_PROPERTIES_RESOURCE, value);
                bindings.put(SharedComponentProperties.SHARED_PROPERTIES, value.getValueMap());
            });
        }
    }

    /**
     * Injects the Merged SCP Properties key into the provided bindings in one of two ways:
     * 1. lazily, if {@code bindings} is an instance of {@code LazyBindings}
     * 2. immediately, for all other kinds of {@code Bindings}
     *
     * @param bindings the bindings
     * @param supplier a merged SCP ValueMap supplier
     */
    protected void injectMergedProps(Bindings bindings, Supplier<ValueMap> supplier) {
        if (isLazy(bindings)) {
            bindings.put(SharedComponentProperties.MERGED_PROPERTIES, wrapSupplier(supplier));
        } else {
            bindings.put(SharedComponentProperties.MERGED_PROPERTIES, supplier.get());
        }
    }

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
            // perform null path check within the supplier
            final Supplier<Optional<Resource>> supplyGlobalResource = () ->
                    globalPropsPath != null
                            ? cache.getResource(globalPropsPath, resource.getResourceResolver()::getResource)
                            : Optional.empty();
            injectGlobalProps(bindings, supplyGlobalResource);

            final String sharedPropsPath = sharedComponentProperties.getSharedPropertiesPath(resource);
            // perform null path check within the supplier
            final Supplier<Optional<Resource>> supplySharedResource = () ->
                    sharedPropsPath != null
                            ? cache.getResource(sharedPropsPath, resource.getResourceResolver()::getResource)
                            : Optional.empty();
            injectSharedProps(bindings, supplySharedResource);
            bindings.put(SharedComponentProperties.SHARED_PROPERTIES_PATH, sharedPropsPath);

            final String mergedPropertiesPath = resource.getPath();
            final Supplier<ValueMap> supplyMergedProperties = () ->
                    cache.getMergedProperties(mergedPropertiesPath, (path) -> {
                        ValueMap globalPropertyMap = supplyGlobalResource.get().map(Resource::getValueMap).orElse(ValueMap.EMPTY);
                        ValueMap sharedPropertyMap = supplySharedResource.get().map(Resource::getValueMap).orElse(ValueMap.EMPTY);
                        return sharedComponentProperties.mergeProperties(globalPropertyMap, sharedPropertyMap, resource);
                    });
            injectMergedProps(bindings, supplyMergedProperties);

            // set this value to indicate cache validity downstream
            bindings.put(SharedComponentProperties.MERGED_PROPERTIES_PATH, mergedPropertiesPath);
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
