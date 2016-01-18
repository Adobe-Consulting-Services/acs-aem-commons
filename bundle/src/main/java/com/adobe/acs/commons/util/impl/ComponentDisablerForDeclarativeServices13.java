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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.util.ComponentDisabler;

/**
 * Disables components with the help of the declarative services API in {@link ServiceComponentRuntime} which has been added with version 1.3.
 */
@Component
@Properties({
    @Property(name = Constants.SERVICE_RANKING, intValue=100)
})
public class ComponentDisablerForDeclarativeServices13 implements ComponentDisabler {

	private static final Logger log = LoggerFactory.getLogger(ComponentDisablerForDeclarativeServices13.class);
	
	@Reference
	ServiceComponentRuntime scr;

	private BundleContext bundleContext;

	@Override
	public void disable(String componentName) {
	    for (Bundle bundle :  bundleContext.getBundles()) {
	        ComponentDescriptionDTO dto = scr.getComponentDescriptionDTO(bundle, componentName);
	        if (dto != null) {
	            if (scr.isComponentEnabled(dto)) {
	                log.info("Component {} disabled by configuration.", dto.implementationClass);
	                scr.disableComponent(dto);
	            }
	        }
	    }
	}

	@Activate
	public void activate(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

}
