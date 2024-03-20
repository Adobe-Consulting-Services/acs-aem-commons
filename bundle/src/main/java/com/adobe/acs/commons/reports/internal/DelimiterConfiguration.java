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
package com.adobe.acs.commons.reports.internal;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = DelimiterConfiguration.class)
@Designate(ocd = DelimiterConfiguration.Config.class)
public class DelimiterConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(DelimiterConfiguration.class);

    public static final String DEFAULT_FIELD_DELIMITER = ",";
    public static final String DEFAULT_MULTI_VALUE_DELIMITER = ";";

    private String fieldDelimiter;
    private String multiValueDelimiter;

    @ObjectClassDefinition(name = "ACS Commons - Report CSV Delimiter Configuration")
    @interface Config {
        @AttributeDefinition(
                name = "Field Delimiter",
                description = "The delimiter to use to limit fields."
        )
        String field_delimiter() default DEFAULT_FIELD_DELIMITER;

        @AttributeDefinition(
                name = "Multi-value Delimiter",
                description = "The delimiter to use in multi-value fields."
        )
        String multi_value_delimiter() default DEFAULT_MULTI_VALUE_DELIMITER;
    }

    @Modified
    @Activate
    public void configurationModified(Config config) {
        LOGGER.info("Activating/updating service");
        this.fieldDelimiter = config.field_delimiter();
        this.multiValueDelimiter = config.multi_value_delimiter();
        LOGGER.info("Activated/updated service: {}", this);
    }

    public String getFieldDelimiter() {
        return this.fieldDelimiter;
    }

    public String getMultiValueDelimiter() {
        return this.multiValueDelimiter;
    }

    @Override
    public String toString() {
        return "DelimiterConfiguration{" +
                "fieldDelimiter='" + fieldDelimiter + '\'' +
                ", multiValueDelimiter='" + multiValueDelimiter + '\'' +
                '}';
    }

}
