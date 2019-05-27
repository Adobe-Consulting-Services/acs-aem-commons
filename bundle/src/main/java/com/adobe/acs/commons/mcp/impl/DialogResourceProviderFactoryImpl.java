package com.adobe.acs.commons.mcp.impl;

import com.adobe.acs.commons.mcp.DialogResourceProviderFactory;
import com.adobe.acs.commons.mcp.form.GeneratedDialog;
import java.util.*;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    service = DialogResourceProviderFactory.class,
    immediate = true
)
public class DialogResourceProviderFactoryImpl implements DialogResourceProviderFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(DialogResourceProviderFactoryImpl.class);
    private static final String IMPLEMENTATION_CLASS = "models.adapter.implementationClass";
    private final Map<Class, ServiceRegistration<ResourceProvider>> resourceProviders = Collections.synchronizedMap(new HashMap<>());

    @Reference(
            service = AdapterFactory.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.STATIC,
            policyOption = ReferencePolicyOption.GREEDY,
            bind = "bind",
            unbind = "unbind"
    )
    volatile List<AdapterFactory> adapterFactory;
    public void bind(AdapterFactory adapterFactory, Map<String, ?> properties) {
        getModelClass(properties).ifPresent(this::registerClass);
    }

    public void unbind(AdapterFactory adapterFactory, Map<String, ?> properties) {
        getModelClass(properties).ifPresent(this::unregisterClass);
    }

    @Deactivate
    public void deactivate() {
        resourceProviders.values().forEach(ServiceRegistration::unregister);
        resourceProviders.clear();
    }

    private Optional<String> getModelClass(Map<String, ?> properties) {
        if (properties != null && properties.containsKey(IMPLEMENTATION_CLASS)) {
            return Optional.of(String.valueOf(properties.get(IMPLEMENTATION_CLASS)));
        }
        return Optional.empty();
    }

    private Optional<Class> getClassIfAvailable(String className) {
        LOGGER.debug(String.format("looking up class %s", className));
        Class clazz = null;
        try {
            clazz = Class.forName(className);
            LOGGER.debug(String.format("found class %s", className));
        } catch (ClassNotFoundException e) {
            // Skip it.
            LOGGER.debug(String.format("COULD NOT FIND %s", className));
        }
        return Optional.ofNullable(clazz);
    }

    @Override
    public void registerClass(String className) {
        getClassIfAvailable(className).ifPresent(this::registerClass);
    }

    @Override
    public void registerClass(Class c) {
        if (resourceProviders.containsKey(c)) {
            unregisterClass(c);
        }
        if (GeneratedDialog.class.isAssignableFrom(c)) {
            synchronized (resourceProviders) {
                DialogResourceProviderImpl provider = null;
                try {
                    provider = new DialogResourceProviderImpl(c);
                    resourceProviders.put(c, registerResourceProvider(provider));
                } catch (InstantiationException | IllegalAccessException e) {
                    // TODO: Better error handling
                }
            }
        }
    }

    private ServiceRegistration<ResourceProvider> registerResourceProvider(DialogResourceProviderImpl provider) {
        BundleContext context = FrameworkUtil.getBundle(DialogResourceProviderFactory.class).getBundleContext();
        Dictionary<String, Object> props = new Hashtable<>();
        props.put(ResourceProvider.PROPERTY_NAME, provider.getRoot());
        props.put(ResourceProvider.PROPERTY_ROOT, provider.getRoot());
//        props.put(ResourceProvider.PROPERTY_MODIFIABLE, Boolean.FALSE);
        props.put(ResourceProvider.PROPERTY_USE_RESOURCE_ACCESS_SECURITY, Boolean.FALSE);
        LOGGER.debug(String.format("Registering at path %s", provider.getRoot()));
        return context.registerService(ResourceProvider.class, provider, props);
    }

    @Override
    public void unregisterClass(String className) {
        getClassIfAvailable(className).ifPresent(this::unregisterClass);
    }

    @Override
    public void unregisterClass(Class c) {
        synchronized (resourceProviders) {
            if (resourceProviders.containsKey(c)) {
                ServiceRegistration<ResourceProvider> provider = resourceProviders.get(c);
                resourceProviders.remove(c);
                provider.unregister();
            }
        }
    }

    @Override
    public Map<Class, ServiceRegistration<ResourceProvider>> getActiveProviders() {
        return resourceProviders;
    }
}
