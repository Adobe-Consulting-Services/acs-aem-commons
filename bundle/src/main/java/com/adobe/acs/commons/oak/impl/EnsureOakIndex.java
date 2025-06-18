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
package com.adobe.acs.commons.oak.impl;

import com.adobe.acs.commons.analysis.jcrchecksum.ChecksumGenerator;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

//@formatter:off
@Component(
        configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(ocd = EnsureOakIndex.Config.class, factory = true)
//@formatter:on
public class EnsureOakIndex implements AppliableEnsureOakIndex {
    static final Logger log = LoggerFactory.getLogger(EnsureOakIndex.class);

    //@formatter:off
    private static final String DEFAULT_ENSURE_DEFINITIONS_PATH = StringUtils.EMPTY;
    private static final String DEFAULT_OAK_INDEXES_PATH = "/oak:index";
    private static final boolean DEFAULT_IMMEDIATE = true;
    public static final String PROP_ENSURE_DEFINITIONS_PATH = "ensure-definitions.path";
    public static final String PROP_OAK_INDEXES_PATH = "oak-indexes.path";
    public static final String PROP_IMMEDIATE = "immediate";
    public static final String PROP_ADDITIONAL_IGNORE_PROPERTIES = "properties.ignore";

    @ObjectClassDefinition(
            name = "ACS AEM Commons - Ensure Oak Index",
            description = "Component Factory to manage Oak indexes."
    )
    @interface Config {

        @AttributeDefinition(
                name = "Ensure Definitions Path",
                description = "The absolute path to the resource containing the "
                        + "ACS AEM Commons ensure definitions"
        )
        String ensure$_$definitions_path() default DEFAULT_ENSURE_DEFINITIONS_PATH;

        @AttributeDefinition(
                name = "Oak Indexes Path",
                description = "The absolute path to the oak:index to update; Defaults to [ /oak:index ]"
        )
        String oak$_$indexes_path() default DEFAULT_OAK_INDEXES_PATH;

        @AttributeDefinition(
                name = "Immediate",
                description = "Apply the indexes on startup of service. Defaults to [ true ]"
        )
        boolean immediate() default DEFAULT_IMMEDIATE;

        @AttributeDefinition(
                name = "Additional ignore properties",
                description = "Property names that are to be ignored when determining if an oak index has changed, "
                        + "as well as what properties should be removed/updated.",
                cardinality = Integer.MAX_VALUE
        )
        String[] properties_ignore() default {};

        String webconsole_configurationFactory_nameHint() default "Definitions: {ensure-definitions.path}, Indexes: {oak-indexes.path}";

    }

    @Reference
    private ChecksumGenerator checksumGenerator;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private Scheduler scheduler;

    @Reference
    private ConfigurationAdmin configurationAdmin;

    private String ensureDefinitionsPath;
    private String oakIndexesPath;
    private boolean immediate = DEFAULT_IMMEDIATE;
    private boolean applied = false;
    private CopyOnWriteArrayList<String> ignoreProperties = new CopyOnWriteArrayList<String>();
    //@formatter:on

    @Activate
    protected final void activate(Config config) throws RepositoryException {

        ensureDefinitionsPath = config.ensure$_$definitions_path();

        oakIndexesPath = config.oak$_$indexes_path();

        if (StringUtils.isBlank(ensureDefinitionsPath)) {
            throw new IllegalArgumentException("OSGi Configuration Property `"
                    + PROP_ENSURE_DEFINITIONS_PATH + "` " + "cannot be blank.");
        } else if (StringUtils.isBlank(oakIndexesPath)) {
            throw new IllegalArgumentException("OSGi Configuration Property `"
                    + PROP_OAK_INDEXES_PATH + "` " + "cannot be blank.");
        }

        this.immediate = config.immediate();

        String[] ignoredProps = config.properties_ignore();
        String[] indexManagerConfiguredIgnoreProperties = getIndexManagerConfiguredIgnoreProperties();

        setIgnoredProperties(ignoredProps, indexManagerConfiguredIgnoreProperties);

        if (this.immediate) {
            apply(false);
        }
    }

    private void setIgnoredProperties(String[] ignoredProps, String[] indexManagerIgnoredProps) {
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
    }

    /**
     * @return the ignore properties, using the ConfigurationAdmin to avoid having a circular reference between EnsureOakIndexManagerImpl and EnsureOakIndex OSGi services
     */
    private String[] getIndexManagerConfiguredIgnoreProperties() {
        try {
             Configuration configuration = configurationAdmin.getConfiguration(EnsureOakIndexManagerImpl.class.getName());
            if (configuration != null && configuration.getProperties() != null) {
                return getIndexManagerConfiguredIgnoreProperties(configuration.getProperties());
            }
        } catch (IOException e) {
            log.warn("Could not get ignored properties from index manager configuration", e);
        }
        return new String[]{};
    }

    private static String[] getIndexManagerConfiguredIgnoreProperties(Dictionary properties) {
        Object indexManagerIgnoredProps = properties.get(PROP_ADDITIONAL_IGNORE_PROPERTIES);
        if (indexManagerIgnoredProps != null) {
            if (indexManagerIgnoredProps instanceof String[]) {
                return (String[]) indexManagerIgnoredProps;
            } else if (indexManagerIgnoredProps instanceof String) {
                return new String[]{(String) indexManagerIgnoredProps};
            }
        }
        return new String[]{};
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
        return Collections.unmodifiableList(this.ignoreProperties);
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
