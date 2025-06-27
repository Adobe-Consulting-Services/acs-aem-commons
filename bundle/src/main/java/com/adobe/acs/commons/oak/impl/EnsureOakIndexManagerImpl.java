/*-
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2025 Adobe
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
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.FieldOption;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
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
        service = {DynamicMBean.class, EnsureOakIndexManager.class},
        immediate = true,
        property = {
                "felix.webconsole.title=Ensure Oak Index",
                "felix.webconsole.label=ensureOakIndex",
                "felix.webconsole.category=Sling",
                "jmx.objectname=com.adobe.acs.commons.oak:type=Ensure Oak Index"
        }
)
@Designate(ocd = EnsureOakIndexManagerImpl.Config.class)
//@formatter:on
public class EnsureOakIndexManagerImpl extends AnnotatedStandardMBean implements EnsureOakIndexManager, EnsureOakIndexManagerMBean {

    //@formatter:off
    private static final String[] DEFAULT_ADDITIONAL_IGNORE_PROPERTIES = new String[]{};
    private String[] additionalIgnoreProperties = DEFAULT_ADDITIONAL_IGNORE_PROPERTIES;
    public static final String PROP_ADDITIONAL_IGNORE_PROPERTIES = "properties.ignore";

    /**
     * The OSGi configuration is only used by the EnsureOakIndex OSGi services and would have ideally been a separate OSGi service,
     * but we can't migrate this easily without losing the PID for existing implementations.
     */
    @ObjectClassDefinition(
            name = "ACS AEM Commons - Ensure Oak Index Manager",
            description = "Manage for ensuring oak indexes."
    )
    @interface Config {

        @AttributeDefinition(
                name = "Additional ignore properties",
                description = "Property names that are to be ignored when determining if an oak index has changed, "
                        + "as well as what properties should be removed/updated.",
                cardinality = Integer.MAX_VALUE
        )
        String[] properties_ignore() default {};

        String webconsole_configurationFactory_nameHint() default "Additional Ignore properties: {properties.ignore}";

    }
    //@formatter:on

    // Disable this feature on AEM as a Cloud Service
    @Reference(target="(distribution=classic)")
    RequireAem requireAem;

    @Reference
    EnsureOakIndexExecutor ensureOakIndexExecutor;

    public EnsureOakIndexManagerImpl() throws NotCompliantMBeanException {
        super(EnsureOakIndexManagerMBean.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int ensureAll(boolean force) {
        return ensureOakIndexExecutor.ensureAll(force);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int ensure(final boolean force,
                            final String ensureDefinitionPath) {
       return ensureOakIndexExecutor.ensure(force, ensureDefinitionPath);
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
        return ensureOakIndexExecutor.getEnsureOakIndexes();
    }

    @Activate
    protected void activate(Config config) {
        additionalIgnoreProperties = config.properties_ignore();
    }


    protected String[] getIgnoredProperties() {
        return Optional.ofNullable(this.additionalIgnoreProperties)
                .map(array -> Arrays.copyOf(array, array.length))
                .orElse(DEFAULT_ADDITIONAL_IGNORE_PROPERTIES);
    }

}
