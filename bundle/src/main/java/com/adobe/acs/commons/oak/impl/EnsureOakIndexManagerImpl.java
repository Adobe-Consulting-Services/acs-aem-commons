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

import com.adobe.acs.commons.util.RequireAem;
import com.adobe.granite.jmx.annotation.AnnotatedStandardMBean;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import javax.management.NotCompliantMBeanException;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.TabularData;
import java.util.Arrays;
import java.util.Optional;


/**
 * This class had a circular reference, to fix this,
 * we moved everything to EnsureOakIndexManagerExecutor and let this one only handle the ignore properties so that the PID is not lost
 */

@Component
@Designate(ocd = EnsureOakIndexManagerImpl.Config.class)
public class EnsureOakIndexManagerImpl implements EnsureOakIndexManagerProperties {

    //@formatter:off
    private static final String[] DEFAULT_ADDITIONAL_IGNORE_PROPERTIES = new String[]{};
    private String[] additionalIgnoreProperties = DEFAULT_ADDITIONAL_IGNORE_PROPERTIES;
    public static final String PROP_ADDITIONAL_IGNORE_PROPERTIES = "properties.ignore";

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
    @Reference(target = "(distribution=classic)")
    RequireAem requireAem;


    @Activate
    protected void activate(Config config) {
        additionalIgnoreProperties = config.properties_ignore();
    }

    @Override
    public String[] getIgnoredProperties() {
        return Optional.ofNullable(this.additionalIgnoreProperties)
                .map(array -> Arrays.copyOf(array, array.length))
                .orElse(DEFAULT_ADDITIONAL_IGNORE_PROPERTIES);
    }

}
