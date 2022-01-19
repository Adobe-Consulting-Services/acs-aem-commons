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
package com.adobe.acs.commons.oak.impl;

import com.adobe.acs.commons.oak.EnsureOakIndexManager;
import com.adobe.acs.commons.util.RequireAem;
import com.adobe.granite.jmx.annotation.AnnotatedStandardMBean;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.DynamicMBean;
import javax.management.NotCompliantMBeanException;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * This implementation of the OakIndexManager also provides a small
 * interface via the OSGI console to check the status of all
 * of EnsureOakIndex instances.
 */

//@formatter:off
@Component(
        label = "ACS AEM Commons - Ensure Oak Index Manager",
        description = "Manage for ensuring oak indexes.",
        immediate = true,
        metatype = true
)
@Properties({
        @Property(
                name = "webconsole.configurationFactory.nameHint",
                value = "Additional Ignore properties: {properties.ignore}",
                propertyPrivate = true
        ),
        @Property(
                name = "felix.webconsole.title",
                value = "Ensure Oak Index",
                propertyPrivate = true
        ),
        @Property(
                name = "felix.webconsole.label",
                value = "ensureOakIndex",
                propertyPrivate = true
        ),
        @Property(
                name = "felix.webconsole.category",
                value = "Sling",
                propertyPrivate = true
        ),
        @Property(
                name = "jmx.objectname",
                value = "com.adobe.acs.commons.oak:type=Ensure Oak Index",
                propertyPrivate = true
        )
})
@Service(value = {DynamicMBean.class, EnsureOakIndexManager.class})
//@formatter:on
public class EnsureOakIndexManagerImpl extends AnnotatedStandardMBean implements EnsureOakIndexManager, EnsureOakIndexManagerMBean {
    private static final Logger log = LoggerFactory.getLogger(EnsureOakIndexManagerImpl.class);

    //@formatter:off
    private static final String[] DEFAULT_ADDITIONAL_IGNORE_PROPERTIES = new String[]{};
    private String[] additionalIgnoreProperties = DEFAULT_ADDITIONAL_IGNORE_PROPERTIES;
    @Property(label = "Additional ignore properties",
            description = "Property names that are to be ignored when determining if an oak index has changed, as well as what properties should be removed/updated.",
            cardinality = Integer.MAX_VALUE,
            value = {})
    public static final String PROP_ADDITIONAL_IGNORE_PROPERTIES = "properties.ignore";


    // Disable this feature on AEM as a Cloud Service
    @Reference(target="(distribution=classic)")
    RequireAem requireAem;

    @Reference(
        cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
        referenceInterface = AppliableEnsureOakIndex.class,
        policy = ReferencePolicy.DYNAMIC
    )
    // Thread-safe ArrayList to track EnsureIndex service registrations
    private CopyOnWriteArrayList<AppliableEnsureOakIndex> ensureIndexes =
            new CopyOnWriteArrayList<AppliableEnsureOakIndex>();
    //@formatter:on

    public EnsureOakIndexManagerImpl() throws NotCompliantMBeanException {
        super(EnsureOakIndexManagerMBean.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int ensureAll(boolean force) {
        log.info("Applying all un-applied ensure index definitions");

        int count = 0;
        for (AppliableEnsureOakIndex index : this.ensureIndexes) {
            if (!index.isApplied() || force) {
                index.apply(force);
                count++;
                log.debug("Started applying index definition on [ {} ]", index);
            } else {
                log.debug("Skipping... [ {} ] is already applied.", index);
            }
        }

        return count;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int ensure(final boolean force,
                            final String ensureDefinitionPath) {
        int count = 0;
        for (AppliableEnsureOakIndex index : this.ensureIndexes) {
            if ((!index.isApplied() || force)
                    && StringUtils.equals(ensureDefinitionPath, index.getEnsureDefinitionsPath())) {
                index.apply(force);
                count++;
                log.debug("Started async job applying index definition for {}", index);
            } else {
                log.debug("Skipping... [ {} ] is already applied.", index);
            }
        }
        return count;
    }

    protected final void bindAppliableEnsureOakIndex(AppliableEnsureOakIndex index) {
        if (index != null && !this.ensureIndexes.contains(index)) {
            this.ensureIndexes.add(index);
        }
    }

    protected final void unbindAppliableEnsureOakIndex(AppliableEnsureOakIndex index) {
        if (index != null && this.ensureIndexes.contains(index)) {
            this.ensureIndexes.remove(index);
        }
    }

    /**
     * Method for displaying Ensure Oak Index state in in the MBean
     *
     * @return the Ensure Oak Index data in a Tabular Format for the MBean
     * @throws OpenDataException
     */
    @Override
    @SuppressWarnings("squid:S1192")
    public final TabularData getEnsureOakIndexes() throws OpenDataException {

        final CompositeType configType = new CompositeType(
                "Ensure Oak Index Configurations",
                "Ensure Oak Index Configurations",
                new String[]{"Ensure Definitions Path", "Oak Indexes Path", "Applied", "Immediate"},
                new String[]{"Ensure Definitions Path", "Oak Indexes Path", "Applied", "Immediate"},
                new OpenType[]{SimpleType.STRING, SimpleType.STRING, SimpleType.BOOLEAN, SimpleType.BOOLEAN});

        final TabularDataSupport tabularData = new TabularDataSupport(new TabularType(
                "Ensure Oak Index Configuration",
                "Ensure Oak Index Configuration",
                configType,
                new String[]{"Ensure Definitions Path", "Oak Indexes Path"}));


        for (final AppliableEnsureOakIndex index : this.ensureIndexes) {
            final Map<String, Object> data = new HashMap<String, Object>();

            data.put("Ensure Definitions Path", index.getEnsureDefinitionsPath());
            data.put("Oak Indexes Path", index.getOakIndexesPath());
            data.put("Applied", index.isApplied());
            data.put("Immediate", index.isImmediate());

            tabularData.put(new CompositeDataSupport(configType, data));
        }

        return tabularData;
    }


    @Activate
    protected void activate(Map<String, Object> config) {
        additionalIgnoreProperties = PropertiesUtil.toStringArray(config.get(PROP_ADDITIONAL_IGNORE_PROPERTIES), DEFAULT_ADDITIONAL_IGNORE_PROPERTIES);
    }
    
    
    protected String[] getIgnoredProperties() {
        return Optional.ofNullable(this.additionalIgnoreProperties)
                .map(array -> Arrays.copyOf(array, array.length))
                .orElse(DEFAULT_ADDITIONAL_IGNORE_PROPERTIES);
    }
}
