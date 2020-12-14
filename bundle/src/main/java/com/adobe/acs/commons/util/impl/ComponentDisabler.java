/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
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
package com.adobe.acs.commons.util.impl;

import java.util.Arrays;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component disabler service
 * 
 * In Apache Felix the state of components and services is not persisted across restarts of its containing bundle.
 * For example, when you have a Bundle S containing a service S, and you manually stop the service S; after a
 * deactivate and activate of the bundle the service S is up again.
 * 
 * This service allows you to specify the names of components, which shouldn't be running. Whenever an OSGI service event is
 * fired, which services checks the status of this components and stops them if required.
 * 
 * Note 1: The component is always started, but this service takes care, that it is stopped immediately after. So if a behaviour
 * you don't like already happens during the activation of this service, you cannot prevent it using the mechanism here.
 * 
 * Note 2: Using this service should always be considered as a workaround. The primary focus should be to fix the component
 * you want to disable, so it's no longer required to disable it. If this component is part of Adobe AEM please raise a Daycare 
 * ticket for it.
 * 
 *
 */
@Component(immediate = true, 
        configurationPolicy = ConfigurationPolicy.REQUIRE, 
        property = { 
            EventConstants.EVENT_TOPIC + "=org/osgi/framework/BundleEvent/STARTED",
            EventConstants.EVENT_TOPIC + "=org/osgi/framework/ServiceEvent/REGISTERED"
        })
@Designate(ocd = ComponentDisabler.Configuration.class)
public class ComponentDisabler implements EventHandler {

    private static final Logger log = LoggerFactory.getLogger(ComponentDisabler.class);

    private String[] disabledComponents;
    private BundleContext bundleContext;
    
    @Reference
    private ServiceComponentRuntime scr;
    
    @ObjectClassDefinition(name = "ACS AEM Commons - OSGI Component Disabler", description =  "Disables components by configuration")
    public @interface Configuration {

        @AttributeDefinition(name = "Disabled components", description = "The names of the components/services you want to disable")
        String[] components();
    }

    @Activate
    public void activate(BundleContext bundleContext, Configuration configuration) {
        disabledComponents = configuration.components();
        this.bundleContext = bundleContext;
        handleEvent(null);
    }

    @Override
    public void handleEvent(Event event) {
        // We don't care about the event, we just need iterate all configured
        // components and try to disable them
        log.trace("Disabling components and services {}", Arrays.toString(disabledComponents));

        for (String component : disabledComponents) {
            disable(component);
        }
    }

    public void disable(String componentName) {
        for (Bundle bundle : bundleContext.getBundles()) {
            ComponentDescriptionDTO dto = scr.getComponentDescriptionDTO(bundle, componentName);
            if (dto != null && scr.isComponentEnabled(dto)) {
                log.info("Component {} disabled by configuration.", dto.implementationClass);
                scr.disableComponent(dto);
            }
        }
    }
}
