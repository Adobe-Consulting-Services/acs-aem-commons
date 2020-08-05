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

import java.util.Map;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.Designate;

/**
 * Factory for dialog resource providers, deprecated after 4.7.0 by way of the new annotation processor.
 * @deprecated Will be removed in 5.0 entirely, no longer needed
 */
@Component(
        service = com.adobe.acs.commons.mcp.DialogResourceProviderFactory.class,
        immediate = true
)
@Designate(ocd = com.adobe.acs.commons.mcp.DialogResourceProviderConfiguration.class)
@SuppressWarnings("deprecation")
@Deprecated
public class DialogResourceProviderFactoryImpl implements com.adobe.acs.commons.mcp.DialogResourceProviderFactory {

    @Override
    public void registerClass(String className) {
        // Does nothing because this OSGi service is deprecated and going away
    }

    @Override
    public void registerClass(Class c) {
        // Does nothing because this OSGi service is deprecated and going away
    }

    @Override
    public void unregisterClass(Class c) {
        // Does nothing because this OSGi service is deprecated and going away
    }

    @Override
    public void unregisterClass(String c) {
        // Does nothing because this OSGi service is deprecated and going away
    }

    @Override
    public Map<String, ServiceRegistration<ResourceProvider>> getActiveProviders() {
        return null;
    }
}
