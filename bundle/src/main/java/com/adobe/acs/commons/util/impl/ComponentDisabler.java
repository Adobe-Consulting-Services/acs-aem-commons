/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.util.impl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OSGi DS Component disabler service.
 * 
 * In Apache Felix the state of DS components is not persisted across restarts of its containing bundle.
 * For example, when you have a Bundle S containing a component S, and you manually stop the component S; after a
 * deactivate and activate of the bundle the component S is up again.
 * <p>
 * This service allows you to specify the names of DS components, which shouldn't be running. Whenever an OSGi service event is
 * fired, which services checks the status of this components and stops them if required.
 * <p>
 * Note 1: The component is always started, but this service takes care, that it is stopped immediately after. So if a behaviour
 * you don't like already happens during the activation of this service, you cannot prevent it using the mechanism here.
 * Particularly components not implementing an OSGi service may be running for a long time until a service registered or bundle
 * started event finally stops them.
 * <p>
 * Note 2: Using this service should always be considered as a workaround. The primary focus should be to fix the component
 * you want to disable, so it's no longer required to disable it. If this component is part of Adobe AEM please raise a Support 
 * ticket for it.
 * @see <a href="https://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.component.html#service.component-introspection">OSGi Declarative Services 1.4, Service Component Runtime, Introspection</a>
 * @see ServiceComponentRuntime#disableComponent(ComponentDescriptionDTO)
 */
@Component(immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
@EventTopics(value = { "org/osgi/framework/BundleEvent/STARTED", "org/osgi/framework/ServiceEvent/REGISTERED"} )
@Designate(ocd = ComponentDisabler.Config.class)
public class ComponentDisabler implements EventHandler {

    @ObjectClassDefinition(name = "ACS AEM Commons - OSGi DS Component Disabler", description = "Disables OSGi DS components by configuration")
    static @interface Config {
        @AttributeDefinition(name = "Disabled components", description = "The names of the components you want to disable (usually their fully class name)", cardinality = Integer.MAX_VALUE)
        String[] components();
    }

    @Component(service = ConfigAmendment.class, property = "webconsole.configurationFactory.nameHint={components}")
    @Designate(factory = true, ocd = ComponentDisabler.Config.class)
    public static final class ConfigAmendment {
        private final Config config;

        @Activate
        public ConfigAmendment(Config config) {
            this.config = config;
        }

        public Config getConfig() {
            return config;
        }
    }

    private static final Logger log = LoggerFactory.getLogger(ComponentDisabler.class);

    private final BundleContext bundleContext;

    private final ServiceComponentRuntime scr;
    
    private final Set<String> disabledComponents;

    @Activate
    public ComponentDisabler(BundleContext bundleContext, Config config, @Reference ServiceComponentRuntime scr, @Reference(policyOption = ReferencePolicyOption.GREEDY) List<ConfigAmendment> configAmendments) {
        this.bundleContext = bundleContext;
        // merge amendments
        disabledComponents = new HashSet<>();
        disabledComponents.addAll(Arrays.asList(config.components()));
        configAmendments.stream().forEach(amendment -> disabledComponents.addAll(Arrays.asList(amendment.getConfig().components())));
        this.scr = scr;
        handleEvent(null);
    }

    @Override
    public void handleEvent(Event event) {
        // We don't care about the event, we just need iterate all configured
        // components and try to disable them
        log.trace("Disabling OSGi DS components {}", String.join(",", disabledComponents));

        for (String component : disabledComponents) {
            disable(component);
        }
    }

    public void disable(String componentName) {
        for (Bundle bundle : bundleContext.getBundles()) {
            ComponentDescriptionDTO dto = scr.getComponentDescriptionDTO(bundle, componentName);
            if (dto != null && scr.isComponentEnabled(dto)) {
                log.info("Disabling OSGi DS Component {}.", dto.implementationClass);
                scr.disableComponent(dto);
            }
        }
    }
}
