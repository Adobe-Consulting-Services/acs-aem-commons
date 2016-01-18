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

import org.apache.felix.scr.ScrService;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.util.ComponentDisabler;

/**
 * Disables components with the help of the Apache Felix SCR API {@link ScrService}, which has been deprecated with the advent of OSGi declarative services 1.3.
 */
@Component
public class ComponentDisablerForFelixScrPre20 implements ComponentDisabler {

	private static final Logger log = LoggerFactory.getLogger(ComponentDisablerForFelixScrPre20.class);

	@Reference
	ScrService scr;

	@Override
	public void disable(String componentName) {
		for (org.apache.felix.scr.Component component : scr.getComponents(componentName)) {
			if (component.getState() != org.apache.felix.scr.Component.STATE_DISABLED) {
				log.info("Component {} disabled by configuration (pid={}) ", component.getClassName(),
						component.getConfigurationPid());
				component.disable();
			}
		}
	}

}
