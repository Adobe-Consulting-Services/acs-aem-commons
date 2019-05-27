package com.adobe.acs.commons.mcp;

import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.osgi.framework.ServiceRegistration;
import java.util.Map;

public interface DialogResourceProviderFactory {

    void registerClass(String className);

    void registerClass(Class c);

    void unregisterClass(String className);

    void unregisterClass(Class c);

    Map<Class, ServiceRegistration<ResourceProvider>> getActiveProviders();
}
