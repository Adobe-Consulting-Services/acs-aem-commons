/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2020 Adobe
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
package com.adobe.acs.commons.mcp.form;

import com.adobe.acs.commons.mcp.impl.DialogResourceProviderImpl;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * Provides an OSGi factory service for a resource provider.  Service
 * implementations are usually generated by the AnnotationProcessor so to reduce
 * the amount of generated code, as much of the underlying implementation is
 * here in default methods.
 */
@SuppressWarnings("squid:S1214") // There are no constants declared here, not sure why this code smell is being detected
public interface DialogResourceProvider {
    Class getTargetClass();

    default DialogProvider getDialogProvider() {
        return (DialogProvider) getTargetClass().getAnnotation(DialogProvider.class);
    }

    public static Map<Class, ServiceRegistration> registeredProviders = Collections.synchronizedMap(new HashMap<>());

    @SuppressWarnings("squid:S1149") // Yes HashTable sucks but it's required here.
    default void doActivate(BundleContext bundleContext) throws RuntimeException, ReflectiveOperationException {
        DialogResourceProviderImpl provider = new DialogResourceProviderImpl(getTargetClass(), getDialogProvider());
        @SuppressWarnings("UseOfObsoleteCollectionType")
        Dictionary<String, Object> props = new Hashtable<>();
        props.put(ResourceProvider.PROPERTY_NAME, provider.getRoot());
        props.put(ResourceProvider.PROPERTY_ROOT, provider.getRoot());
        props.put(ResourceProvider.PROPERTY_USE_RESOURCE_ACCESS_SECURITY, Boolean.FALSE);
        ServiceRegistration providerRegistration = bundleContext.registerService(ResourceProvider.class, provider, props);
        registeredProviders.put(getTargetClass(), providerRegistration);
    }

    default void doDeactivate() {
        ServiceRegistration providerRegistration = registeredProviders.get(getTargetClass());
        if (providerRegistration != null) {
            providerRegistration.unregister();
        }
        registeredProviders.remove(getTargetClass());
    }

    public static String getServiceClassName(String modelClass) {
        String[] parts = modelClass.split("\\.");
        String name = "";
        String separator = ".";
        for (String part : parts) {
            char firstChar = part.charAt(0);
            String newSeparator = separator;
            if (firstChar >= 'A' && firstChar <= 'Z' && separator.equals(".")) {
                newSeparator = "$";
                name += ".impl";
            }
            if (name.length() > 0) {
                name += separator;
            }
            name += part;
            separator = newSeparator;
        }
        return name + "_dialogResourceProvider";
    }

}
