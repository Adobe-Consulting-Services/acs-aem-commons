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

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.analysis.jcrchecksum.ChecksumGenerator;
import com.adobe.acs.commons.oak.EnsureOakIndexManager;

//@formatter:off
@Component(
        label = "ACS AEM Commons - Ensure Oak Index",
        description = "Component Factory to manage Oak indexes.",
        configurationFactory = true,
        policy = ConfigurationPolicy.REQUIRE,
        metatype = true
)
@Properties({
        @Property(
                name = "webconsole.configurationFactory.nameHint",
                value = "Definitions: {ensure-definitions.path}, Indexes: {oak-indexes.path}",
                propertyPrivate = true
        )
})
@Service
//@formatter:on
public class EnsureOakIndex implements AppliableEnsureOakIndex {
    static final Logger log = LoggerFactory.getLogger(EnsureOakIndex.class);

    //@formatter:off
    private static final String DEFAULT_ENSURE_DEFINITIONS_PATH = StringUtils.EMPTY;
    @Property(label = "Ensure Definitions Path",
            description = "The absolute path to the resource containing the "
                    + "ACS AEM Commons ensure definitions",
            value = DEFAULT_ENSURE_DEFINITIONS_PATH)
    public static final String PROP_ENSURE_DEFINITIONS_PATH = "ensure-definitions.path";


    private static final String DEFAULT_OAK_INDEXES_PATH = "/oak:index";
    @Property(label = "Oak Indexes Path",
            description = "The absolute path to the oak:index to update; Defaults to [ /oak:index ]",
            value = DEFAULT_OAK_INDEXES_PATH)
    public static final String PROP_OAK_INDEXES_PATH = "oak-indexes.path";

    private static final boolean DEFAULT_IMMEDIATE = true;
    @Property(
            label = "Immediate",
            description = "Apply the indexes on startup of service. Defaults to [ true ]",
            boolValue = DEFAULT_IMMEDIATE
    )
    public static final String PROP_IMMEDIATE = "immediate";
    
    private static final String[] DEFAULT_ADDITIONAL_IGNORE_PROPERTIES = new String[]{};
    @Property(label = "Additional ignore properties",
            description = "Property names that are to be ignored when determining if an oak index has changed, as well as what properties should be removed/updated.",
            cardinality = Integer.MAX_VALUE,
            value = {})
    public static final String PROP_ADDITIONAL_IGNORE_PROPERTIES = "properties.ignore";

    @Reference
    private ChecksumGenerator checksumGenerator;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private Scheduler scheduler;
    
    @Reference
    private EnsureOakIndexManager indexManager;

    private String ensureDefinitionsPath;
    private String oakIndexesPath;
    private boolean immediate = DEFAULT_IMMEDIATE;
    private boolean applied = false;
    private CopyOnWriteArrayList<String> ignoreProperties = new CopyOnWriteArrayList<String>();
    //@formatter:on

    @Activate
    protected final void activate(Map<String, Object> config) throws RepositoryException {

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

        this.immediate = PropertiesUtil.toBoolean(config.get(PROP_IMMEDIATE), DEFAULT_IMMEDIATE);
        
        String[] ignoredProps = PropertiesUtil.toStringArray(config.get(PROP_ADDITIONAL_IGNORE_PROPERTIES), DEFAULT_ADDITIONAL_IGNORE_PROPERTIES);
        String[] indexManagerIgnoredProps = getIndexManagerConfiguredIgnoreProperties();
        
        if (ignoredProps.length == 0) {
            // Legacy: check if EnsureOakIndexManagerImpl has this property configured -- https://github.com/Adobe-Consulting-Services/acs-aem-commons/issues/1966
            if (indexManagerIgnoredProps.length != 0) {
                this.ignoreProperties = new CopyOnWriteArrayList<String>(indexManagerIgnoredProps);
                log.warn("The configuration of ignoredProperties on the EnsureOakIndexManagerImpl is deprecated, these properties should be configured on the EnsureOakIndex service instead. "
                        + "For convenience they are respected for now, but please move them over.");
            }
        } else {
            // properties are configured on this class
            this.ignoreProperties = new CopyOnWriteArrayList<String>(ignoredProps);
            // but we should warn nevertheless if this property is still configured on EnsureOakIndexManagerImpl
            if (indexManagerIgnoredProps.length != 0) {
                log.warn("Configuration of the ignoredProperties is present on EnsureOakIndex, but there is also a (legacy) configuration at EnsureOakIndexManagerImpl"
                        + "; please delete it");
            }
        }
        if (this.immediate) {
            apply(false);
        }
    }

    
    private String[] getIndexManagerConfiguredIgnoreProperties() {
        if (indexManager instanceof EnsureOakIndexManagerImpl) {
            EnsureOakIndexManagerImpl impl = (EnsureOakIndexManagerImpl) indexManager;
            return impl.getIgnoredProperties();
        }
        return new String[] {};
    }

    /**
     * {@inheritDoc}
     **/
    @Override
    public final void apply(boolean force) {

        if (!force && this.applied) {
            return;
        }

        log.info("Ensuring Oak Indexes [ {} ~> {} ]", ensureDefinitionsPath, oakIndexesPath);

        // Start the indexing process asynchronously, so the activate won't get blocked
        // by rebuilding a synchronous index

        EnsureOakIndexJobHandler jobHandler =
                new EnsureOakIndexJobHandler(this, oakIndexesPath, ensureDefinitionsPath);
        ScheduleOptions options = scheduler.NOW();
        options.name(toString());
        options.canRunConcurrently(false);
        scheduler.schedule(jobHandler, options);

        applied = true;

        log.info("Job scheduled for ensuring Oak Indexes [ {} ~> {} ]", ensureDefinitionsPath, oakIndexesPath);
    }

    @Override
    public final boolean isApplied() {
        return this.applied;
    }

    @Override
    public boolean isImmediate() {
        return this.immediate;
    }

    @Override
    public List<String> getIgnoreProperties() {
        return this.ignoreProperties;
    }

    @Override
    public final String getEnsureDefinitionsPath() {
        return StringUtils.trim(this.ensureDefinitionsPath);
    }

    @Override
    public String getOakIndexesPath() {
        return StringUtils.trim(this.oakIndexesPath);
    }

    public final String toString() {
        return String.format("EnsureOakIndex( %s => %s )",
                ensureDefinitionsPath, oakIndexesPath);
    }

    ChecksumGenerator getChecksumGenerator() {
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