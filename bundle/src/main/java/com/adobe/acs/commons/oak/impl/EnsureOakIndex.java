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
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.analysis.jcrchecksum.ChecksumGenerator;

//@formatter:off
@Component(
        service = AppliableEnsureOakIndex.class,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {
                "webconsole.configurationFactory.nameHint=Definitions: {ensure-definitions.path}, Indexes: {oak-indexes.path}"
        }
)
@Designate(
        ocd=EnsureOakIndex.Config.class,
        factory=true
)
//@formatter:on
public class EnsureOakIndex implements AppliableEnsureOakIndex {
    static final Logger log = LoggerFactory.getLogger(EnsureOakIndex.class);

    @ObjectClassDefinition(
            name = "ACS AEM Commons - Ensure Oak Index",
            description = "Component Factory to manage Oak indexes."
    )
    public @interface Config {
        @AttributeDefinition(
                name = "Ensure Definitions Path",
                description = "The absolute path to the resource containing the "
                        + "ACS AEM Commons ensure definitions",
                defaultValue = DEFAULT_ENSURE_DEFINITIONS_PATH)
        String ensure$_$definitions_path();


        @AttributeDefinition(
                name = "Oak Indexes Path",
                description = "The absolute path to the oak:index to update; Defaults to [ /oak:index ]",
                defaultValue = DEFAULT_OAK_INDEXES_PATH
        )
        String oak$_$indexes_path();

        @AttributeDefinition(
                name = "Immediate",
                description = "Apply the indexes on startup of service. Defaults to [ true ]",
                defaultValue = "" + DEFAULT_IMMEDIATE
        )
        boolean immediate();
    }

    //@formatter:off
    private static final String DEFAULT_ENSURE_DEFINITIONS_PATH = StringUtils.EMPTY;

    public static final String PROP_ENSURE_DEFINITIONS_PATH = "ensure-definitions.path";

    private static final String DEFAULT_OAK_INDEXES_PATH = "/oak:index";

    public static final String PROP_OAK_INDEXES_PATH = "oak-indexes.path";

    private static final boolean DEFAULT_IMMEDIATE = true;

    public static final String PROP_IMMEDIATE = "immediate";

    @Reference
    private ChecksumGenerator checksumGenerator;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private Scheduler scheduler;

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

        if (this.immediate) {
            apply(false);
        }
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
    public void setIgnoreProperties(String[] ignoreProperties) {
        this.ignoreProperties = new CopyOnWriteArrayList<String>(ignoreProperties);
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