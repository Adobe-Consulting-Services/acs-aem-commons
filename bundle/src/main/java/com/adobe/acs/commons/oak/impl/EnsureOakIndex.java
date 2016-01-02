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
 */
package com.adobe.acs.commons.oak.impl;

import com.adobe.acs.commons.analysis.jcrchecksum.ChecksumGenerator;
import com.adobe.acs.commons.util.AemCapabilityHelper;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.Map;

//@formatter:off
@Component(label = "ACS AEM Commons - Ensure Oak Index",
        description = "Component Factory to manage Oak indexes.",
        configurationFactory = true,
        metatype = true)
@Properties({
        @Property(
                name = "webconsole.configurationFactory.nameHint",
                value = "Definitions: {ensure-definitions.path}, Indexes: {oak-indexes.path}",
                propertyPrivate = true
        )
})
//@formatter:on
public class EnsureOakIndex {

    static final Logger log = LoggerFactory.getLogger(EnsureOakIndex.class);

    @Reference
    private AemCapabilityHelper capabilityHelper;

    @Reference
    private ChecksumGenerator checksumGenerator;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private Scheduler scheduler;

    private static final String DEFAULT_ENSURE_DEFINITIONS_PATH = StringUtils.EMPTY;

    @Property(label = "Ensure Definitions Path",
            description = "The absolute path to the resource containing the "
                    + "ACS AEM Commons ensure definitions",
            value = DEFAULT_ENSURE_DEFINITIONS_PATH)
    public static final String PROP_ENSURE_DEFINITIONS_PATH = "ensure-definitions.path";
    private String ensureDefinitionsPath;

    private static final String DEFAULT_OAK_INDEXES_PATH = "/oak:index";

    @Property(label = "Oak Indexes Path",
            description = "The absolute path to the oak:index to update; Defaults to [ /oak:index ]",
            value = DEFAULT_OAK_INDEXES_PATH)
    public static final String PROP_OAK_INDEXES_PATH = "oak-indexes.path";
    private String oakIndexesPath;
    
    @Property(label = "Apply on startup",
    		description = "Apply the indexes on startup of service",
    		boolValue = true)
    public static final String PROP_APPLY_ON_START = "oak-index.applyOnStartup";

    @Activate
    protected final void activate(Map<String, Object> config) throws RepositoryException {
        if (!capabilityHelper.isOak()) {
            log.info("Cowardly refusing to create indexes on non-Oak instance.");
            return;
        }

        ensureDefinitionsPath = PropertiesUtil.toString(config.get(PROP_ENSURE_DEFINITIONS_PATH),
                DEFAULT_ENSURE_DEFINITIONS_PATH);

        oakIndexesPath = PropertiesUtil.toString(config.get(PROP_OAK_INDEXES_PATH),
                DEFAULT_OAK_INDEXES_PATH);

        if (StringUtils.isBlank(ensureDefinitionsPath)) {
            throw new IllegalArgumentException("OSGi Configuration Property `"
                    + PROP_ENSURE_DEFINITIONS_PATH + "` " + "cannot be blank.");
        } else if (StringUtils.isBlank(oakIndexesPath)) {
            throw new IllegalArgumentException("OSGi Configuration Property `"
                    + PROP_OAK_INDEXES_PATH + "` " + "cannot be blank.");
        }
        
        final boolean applyOnStartup = PropertiesUtil.toBoolean(config.get(PROP_APPLY_ON_START), true);

        if (applyOnStartup) {
            StartJobHandler ();
        }
    }





	private void StartJobHandler() {
		log.info("Ensuring Oak Indexes [ {} ~> {} ]", ensureDefinitionsPath, oakIndexesPath);

        // Start the indexing process asynchronously, so the activate won't get blocked
        // by rebuilding a synchronous index

        EnsureOakIndexJobHandler jobHandler =
                new EnsureOakIndexJobHandler(this, oakIndexesPath, ensureDefinitionsPath);
        ScheduleOptions options = scheduler.NOW();
        String name = String.format("Ensure index %s => %s", new Object[]{ oakIndexesPath, ensureDefinitionsPath });
        options.name(name);
        options.canRunConcurrently(false);
        scheduler.schedule(jobHandler, options);

        log.info("Job scheduled for ensuring Oak Indexes [ {} ~> {} ]", ensureDefinitionsPath, oakIndexesPath);
	}
    
    
    
    

    final ChecksumGenerator getChecksumGenerator() {
        return checksumGenerator;
    }

    final ResourceResolverFactory getResourceResolverFactory() {
        return resourceResolverFactory;
    }

    static class OakIndexDefinitionException extends Exception {
        OakIndexDefinitionException(String message) {
            super(message);
        }
    }
}