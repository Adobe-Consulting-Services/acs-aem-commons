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
 */package com.adobe.acs.commons.version.impl;

import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Activate;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.version.EvolutionAnalyser;
import com.adobe.acs.commons.version.EvolutionContext;

@Component
public class EvolutionAnalyserImpl implements EvolutionAnalyser {

    private static final Logger log = LoggerFactory.getLogger(EvolutionAnalyserImpl.class);

    protected static final String PROPERTY_IGNORES = "properties.ignore";
    protected static final String RESOURCE_IGNORES = "resources.ignore";

    private EvolutionConfig evolutionConfig;

    @Override
    public EvolutionContext getEvolutionContext(Resource resource) {
        return new EvolutionContextImpl(resource, evolutionConfig);
    }

    @Activate
    protected void activate(final Map<String, String> config) {
        String[] propertyIgnores = PropertiesUtil.toStringArray(config.get(PROPERTY_IGNORES), new String[]{""});
        String[] resourceIgnores = PropertiesUtil.toStringArray(config.get(RESOURCE_IGNORES), new String[]{""});
        evolutionConfig = new EvolutionConfig(propertyIgnores, resourceIgnores);
        log.debug("Ignored properties: {}", (Object[]) propertyIgnores);
        log.debug("Ignored resources: {}", (Object[]) resourceIgnores);
    }

}
