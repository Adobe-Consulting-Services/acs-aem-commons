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
 */package com.adobe.acs.commons.version.impl;

import org.apache.sling.api.resource.Resource;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.version.EvolutionAnalyser;
import com.adobe.acs.commons.version.EvolutionContext;

@Component(service=EvolutionAnalyser.class)
@Designate(ocd=EvolutionAnalyserImpl.Config.class)
public class EvolutionAnalyserImpl implements EvolutionAnalyser {
   
   @ObjectClassDefinition(name = "ACS AEM Commons - Resource Evolution Analyser",
           description = "Have a look at the evolution of a resource on a property/resource level.")
   public @interface Config {
      @AttributeDefinition(name = "Ignored property names",
                description = "Property names (regex possible) listed here will be excluded from the version compare feature.",
                defaultValue = { "(.*/)?jcr:uuid", "(.*/)?(cq|jcr):lastModified", "(.*/)?(cq|jcr):lastModifiedBy", "(.*/)?jcr:frozenUuid", "(.*/)?jcr:primaryType", "(.*/)?jcr:frozenPrimaryType" }, cardinality = Integer.MAX_VALUE)
      String[] properties_ignore();
      @AttributeDefinition(name = "Ignored resource names",
                description = "Resource names (regex possible) listed here will be excluded from the version compare feature.",
                cardinality = Integer.MAX_VALUE)
      String[] resources_ignore();
   }

    private static final Logger log = LoggerFactory.getLogger(EvolutionAnalyserImpl.class);

    protected static final String PROPERTY_IGNORES = "properties.ignore";
    protected static final String RESOURCE_IGNORES = "resources.ignore";
    private String[] propertyIgnores;
    private String[] resourceIgnores;

    private EvolutionConfig evolutionConfig;

    @Override
    public EvolutionContext getEvolutionContext(Resource resource) {
        return new EvolutionContextImpl(resource, evolutionConfig);
    }

    @Activate
    protected void activate(EvolutionAnalyserImpl.Config config) {
        propertyIgnores = config.properties_ignore();
        resourceIgnores = config.resources_ignore();
        evolutionConfig = new EvolutionConfig(propertyIgnores, resourceIgnores);
        log.debug("Ignored properties: {}", propertyIgnores);
        log.debug("Ignored resources: {}", resourceIgnores);
    }

}
