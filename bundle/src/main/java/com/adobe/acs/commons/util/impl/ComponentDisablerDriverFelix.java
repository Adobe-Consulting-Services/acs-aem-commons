/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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
 */package com.adobe.acs.commons.util.impl;

import org.apache.felix.scr.ScrService;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Service
@Property(name = Constants.SERVICE_RANKING, intValue = 100)
public class ComponentDisablerDriverFelix implements ComponentDisablerDriver {

    // purposely using a different logger name
    private static final Logger log = LoggerFactory.getLogger(ComponentDisabler.class);

    @Reference
    private ScrService scr;

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
