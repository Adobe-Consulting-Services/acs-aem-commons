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
package com.adobe.acs.commons.util.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Service
@Property(name = Constants.SERVICE_RANKING, intValue = 200)
public class ComponentDisablerDriverDS13 implements ComponentDisablerDriver {

    // purposely using a different logger name
    private static final Logger log = LoggerFactory.getLogger(ComponentDisabler.class);

    @Reference
    private ServiceComponentRuntime scr;

    private BundleContext bundleContext;

    @Override
    public void disable(String componentName) {
        for (Bundle bundle : bundleContext.getBundles()) {
            ComponentDescriptionDTO dto = scr.getComponentDescriptionDTO(bundle, componentName);
            if (dto != null && scr.isComponentEnabled(dto)) {
                log.info("Component {} disabled by configuration.", dto.implementationClass);
                scr.disableComponent(dto);
            }
        }
    }

    @Activate
    public void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

}
