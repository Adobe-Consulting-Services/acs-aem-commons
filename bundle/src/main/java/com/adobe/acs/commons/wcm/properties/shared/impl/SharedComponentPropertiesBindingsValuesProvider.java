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
import org.apache.felix.scr.annotations.Activate;
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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

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
     * The LazyBindings class, and its Supplier child interface, are introduced in org.apache.sling.api version 2.22.0,
     * which is first included in AEM 6.5 SP7.
     */
    protected static final String FQDN_LAZY_BINDINGS = "org.apache.sling.api.scripting.LazyBindings";
    protected static final String SUPPLIER_PROXY_LABEL = "ACS AEM Commons SCP BVP reflective Proxy for LazyBindings.Supplier";

    /**
     * Bind if available, check for null when reading.
     */
    @Reference(policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL_UNARY)
    SharedComponentProperties sharedComponentProperties;

    /**
     * Added for pre-6.5.7 support for LazyBindings. This holds the LazyBindings interface
     * if it is discovered on activation, and is used to check if the {@link #addBindings(Bindings)} param
     * is an instance of LazyBindings. This hack is necessary until this bundle can drop support for
     * AEM versions prior to 6.5.7, at which point this variable can be removed, and the {@link #isLazy(Bindings)}
     * method can be simplified to return {@code bindings instanceof LazyBindings}.
     */
    private Class<? extends Bindings> lazyBindingsType;

    /**
     * Added for pre-6.5.7 support for LazyBindings. This holds the LazyBindings.Supplier interface
     * if it is discovered on activation, and is used to create reflection Proxy instances as a hack
     * until this bundle can drop support for AEM versions prior to 6.5.7, at which point this variable
     * can be removed, and the {@link #wrapSupplier(Supplier)} method can be simplified to accept a
     * LazyBindings.Supplier instead of a java.util.function.Supplier and return it (for matching a
     * lambda expression passed at the call site), or to simply return a lambda that calls the get()
     * method on the java.util.function.Supplier argument.
     */
    private Class<? extends Supplier> supplierType;

    /**
     * This variable only exists to facilitate testing for pre-6.5.7 LazyBindings support, so that a non-classpath
     * class loader can be injected, to provide the LazyBindings class.
     */
    private ClassLoader lazyBindingsClassLoader = SlingBindings.class.getClassLoader();

    /**
     * Called by the unit test to inject a URL class loader that provides a LazyBindings instance
     * at {@link #FQDN_LAZY_BINDINGS}.
     *
     * @param classLoader a new class loader
     * @return the old class loader
     */
    protected ClassLoader swapLazyBindingsClassLoaderForTesting(ClassLoader classLoader) {
        if (classLoader != null) {
            ClassLoader oldClassLoader = this.lazyBindingsClassLoader;
            this.lazyBindingsClassLoader = classLoader;
            return oldClassLoader;
        }
        return null;
    }

    /**
     * Return the resolved lazyBindingsType for testing.
     *
     * @return the lazyBindingsType
     */
    protected Class<? extends Bindings> getLazyBindingsType() {
        return this.lazyBindingsType;
    }

    /**
     * Return the resolved supplierType for testing.
     *
     * @return the supplierType
     */
    protected Class<? extends Supplier> getSupplierType() {
        return this.supplierType;
    }

    /**
     * This method ensures that the provided supplier is appropriately typed for insertion into a SlingBindings
     * object. It primarily facilitates lambda type inference (i.e., {@code wrapSupplier(() -> something)} forces
     * inference to the functional interface type of the method parameter). And so long as pre-6.5.7 AEMs are supported,
     * this method is also responsible for constructing the {@link Proxy} instance when LazyBindings is present at
     * runtime, and for immediately returning {@code Supplier.get()} when it is not present.
     * After support for pre-6.5.7 AEMs is dropped, the method return type can be changed from {@code Object} to
     * {@code <T> LazyBindings.Supplier<T>} to fully support lazy injection.
     *
     * @param supplier the provided supplier
     * @return the Supplier as a LazyBindings.Supplier if supported, or the value of the provided supplier if not
     */
    protected Object wrapSupplier(final Supplier<?> supplier) {
        if (this.supplierType != null) {
            return Proxy.newProxyInstance(lazyBindingsClassLoader, new Class[]{this.supplierType},
                    new SupplierWrapper(supplier));
        }
        return supplier.get();
    }

    /**
     * The only purpose of this class is to drive the pre-6.5.7 reflection-based Proxy instance returned
     * by {@link #wrapSupplier(Supplier)}.
     */
    protected static class SupplierWrapper implements InvocationHandler {
        private final Supplier<?> wrapped;

        public SupplierWrapper(final Supplier<?> supplier) {
            this.wrapped = supplier;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // we are implementing a @FunctionalInterface, so don't get carried away with implementing
            // Object methods.
            if ("get".equals(method.getName())) {
                return wrapped.get();
            } else if ("toString".equals(method.getName())) {
                // return this marker string for visibility in debugging tools. Otherwise,
                // the default toString is "\"null\"", which is confusing
                return SUPPLIER_PROXY_LABEL;
            }
            return method.getDefaultValue();
        }
    }

    /**
     * The purpose of this activate method is to determine if we are running in a 6.5.7+ AEM environment
     * without having to explicitly require {@code org.apache.sling.api.scripting} package version 2.5.0.
     */
    @Activate
    protected void activate() {
        // use SlingBindings class loader to check for LazyBindings class,
        // to minimize risk involved with using reflection.
        try {
            this.checkAndSetLazyBindingsType(lazyBindingsClassLoader.loadClass(FQDN_LAZY_BINDINGS));
        } catch (ReflectiveOperationException cnfe) {
            log.info("LazyBindings not found, will resort to injecting immediate Bindings values", cnfe);
        }
    }

    /**
     * Check that the provided {@code lazyBindingsType} implements {@link Bindings} and defines an enclosed marker
     * interface named {@code Supplier} that extends {@link Supplier}, and if so, set {@code this.lazyBindingsType} and
     * {@code this.supplierType}. Otherwise, set both to {@code null}.
     */
    @SuppressWarnings({"squid:S1872", "unchecked"})
    protected void checkAndSetLazyBindingsType(final Class<?> lazyBindingsType) {
        if (lazyBindingsType != null && Bindings.class.isAssignableFrom(lazyBindingsType)) {
            this.supplierType = (Class<? extends Supplier>) Stream.of(lazyBindingsType.getDeclaredClasses())
                    .filter(clazz -> Supplier.class.getSimpleName().equals(clazz.getSimpleName())
                            && Supplier.class.isAssignableFrom(clazz)).findFirst().orElse(null);
            this.lazyBindingsType = (Class<? extends Bindings>) lazyBindingsType;
        } else {
            log.info("Supplier interface not declared by lazyBindingsType: {}, will resort to immediate Bindings values",
                    lazyBindingsType);
            this.supplierType = null;
            this.lazyBindingsType = null;
        }
    }

    /**
     * Check if provided {@code bindings} implements LazyBindings.
     *
     * @param bindings the parameter from {@link #addBindings(Bindings)}
     * @return true if bindings implements LazyBindings
     */
    private boolean isLazy(Bindings bindings) {
        return Optional.ofNullable(this.lazyBindingsType)
                .map(clazz -> clazz.isInstance(bindings))
                .orElse(false);
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
