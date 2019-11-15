/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2019 Adobe
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
package com.adobe.acs.commons.mcp.impl;

import com.adobe.acs.commons.mcp.DialogResourceProviderConfiguration;
import com.adobe.acs.commons.mcp.DialogResourceProviderFactory;
import com.adobe.acs.commons.mcp.form.DialogProvider;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.StreamSupport;
import org.apache.commons.lang3.ClassUtils;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.LoggerFactory;

@Component(
        service = DialogResourceProviderFactory.class,
        immediate = true
)
@Designate(ocd = DialogResourceProviderConfiguration.class)
public class DialogResourceProviderFactoryImpl implements DialogResourceProviderFactory {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(DialogResourceProviderFactoryImpl.class);
    private static final String IMPLEMENTATION_CLASS = "models.adapter.implementationClass";
    private final Map<String, ServiceRegistration<ResourceProvider>> resourceProviderRegistrations
            = Collections.synchronizedMap(new HashMap<>());
    private final Set<String> allKnownModels = Collections.synchronizedSet(new HashSet<>());
    private final String[] ignoredPackages = new String[]{
        "com.adobe.cq.",
        "com.adobe.aemds.",
        "com.adobe.fd.ccm.",
        "com.adobe.forms.",
        "com.adobe.granite.",
        "com.day.cq.",
        "we.retail."
    };

    @Reference(
            service = AdapterFactory.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            policyOption = ReferencePolicyOption.GREEDY,
            bind = "bind",
            unbind = "unbind"
    )
    volatile List<AdapterFactory> adapterFactory;

    @Reference
    volatile DynamicClassLoaderManager dynamicClassLoaderManager;

    volatile BundleContext bundleContext;

    private boolean enabled = false;

    public boolean isEnabled() {
        return enabled;
    }

    private List<Map<String, ?>> unregisteredAdapterFactories = new CopyOnWriteArrayList<>();

    public void bind(AdapterFactory adapterFactory, Map<String, ?> properties) {
        if (dynamicClassLoaderManager == null) {
            unregisteredAdapterFactories.add(properties);
        } else {
            getModelClass(properties).ifPresent(this::registerClass);
        }
    }

    public void unbind(AdapterFactory adapterFactory, Map<String, ?> properties) {
        getModelClass(properties).ifPresent(this::unregisterClass);
    }

    @Activate
    public void activate(BundleContext context, DialogResourceProviderConfiguration config) {
        this.bundleContext = context;
        unregisteredAdapterFactories.forEach(p -> getModelClass(p).ifPresent(this::registerClass));
        unregisteredAdapterFactories.clear();
        setEnabled(config != null && config.enabled());
    }

    public void setEnabled(boolean state) {
        // This was made separate from activation because mock OSGi can't support reconfigration currently.
        this.enabled = state;
        if (isEnabled()) {
            allKnownModels.forEach(this::registerClass);
        } else {
            deactivate();
        }
    }

    @Deactivate
    public void deactivate() {
        allKnownModels.forEach(this::unregisterClass);
        resourceProviderRegistrations.clear();
    }

    private Optional<String> getModelClass(Map<String, ?> properties) {
        if (properties != null && properties.containsKey(IMPLEMENTATION_CLASS)) {
            return Optional.of(String.valueOf(properties.get(IMPLEMENTATION_CLASS)));
        }
        return Optional.empty();
    }

    @SuppressWarnings("squid:S2658") // class name is from a trusted source
    private Optional<Class> getClassIfAvailable(String className) {
        for (String ignored : ignoredPackages) {
            if (className.startsWith(ignored)) {
                return Optional.empty();
            }
        }
        Class clazz = null;
        try {
            clazz = Class.forName(className, true, dynamicClassLoaderManager.getDynamicClassLoader());
        } catch (ClassNotFoundException e) {
            for (Bundle bundle : FrameworkUtil.getBundle(this.getClass()).getBundleContext().getBundles()) {
                try {
                    clazz = bundle.loadClass(className);
                    return Optional.of(clazz);
                } catch (ClassNotFoundException ex) {
                    // Skip
                }
            }
            LOG.debug(String.format("COULD NOT RESOLVE CLASS %s", className));
        }
        return Optional.ofNullable(clazz);
    }

    @Override
    public void registerClass(String className) {
        getClassIfAvailable(className).ifPresent(this::registerClass);
    }

    @Override
    public void registerClass(Class c) {
        allKnownModels.add(c.getName());
        if (isEnabled() && isDialogProvider(c)) {
            unregisterClass(c);
            DialogResourceProviderImpl provider = null;
            try {
                provider = new DialogResourceProviderImpl(c, getDialogProviderAnnotation(c).orElse(null));
                resourceProviderRegistrations.put(c.getName(), registerResourceProvider(provider));
            } catch (InstantiationException | IllegalAccessException e) {
                LOG.error("Error when registering resource provider", e);
            }
        }
    }

    @SuppressWarnings("squid:S1149")
    private ServiceRegistration<ResourceProvider> registerResourceProvider(DialogResourceProviderImpl provider) {
        @SuppressWarnings("UseOfObsoleteCollectionType")
        Dictionary<String, Object> props = new Hashtable<>();
        props.put(ResourceProvider.PROPERTY_NAME, provider.getRoot());
        props.put(ResourceProvider.PROPERTY_ROOT, provider.getRoot());
        props.put(ResourceProvider.PROPERTY_USE_RESOURCE_ACCESS_SECURITY, Boolean.FALSE);
        LOG.debug(String.format("Registering at path %s", provider.getRoot()));
        return bundleContext.registerService(ResourceProvider.class, provider, props);
    }

    @Override
    public void unregisterClass(Class c) {
        unregisterClass(c.getName());
    }

    @Override
    public void unregisterClass(String c) {
        ServiceRegistration<ResourceProvider> serviceRegistration = resourceProviderRegistrations.remove(c);
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
        }
    }

    @Override
    public Map<String, ServiceRegistration<ResourceProvider>> getActiveProviders() {
        return Collections.unmodifiableMap(resourceProviderRegistrations);
    }

    private boolean isDialogProvider(Class c) {
        return getDialogProviderAnnotation(c).isPresent();
    }

    private Optional<DialogProvider> getDialogProviderAnnotation(Class c) {
        return StreamSupport.stream(ClassUtils.hierarchy(c, ClassUtils.Interfaces.INCLUDE).spliterator(), false)
                .filter(clazz -> clazz.isAnnotationPresent(DialogProvider.class))
                .findFirst()
                .map(clazz -> clazz.getAnnotation(DialogProvider.class));
    }
}
