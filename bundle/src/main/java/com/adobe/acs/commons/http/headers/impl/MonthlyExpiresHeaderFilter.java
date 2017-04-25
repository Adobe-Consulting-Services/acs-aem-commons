/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2015 Adobe
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
package com.adobe.acs.commons.http.headers.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;

import java.util.Calendar;
import java.util.Dictionary;

//@formatter:off
@Component(
    label = "ACS AEM Commons - Dispatcher Expires Header - Monthly",
    description = "Adds an Expires header to content to enable Dispatcher TTL support.",
    metatype = true,
    configurationFactory = true,
    policy = ConfigurationPolicy.REQUIRE)
@Properties({
  @Property(label = "Filter Patterns",
      description = "Patterns on which to apply this Expires rule.",
      cardinality = Integer.MAX_VALUE,
      name = AbstractDispatcherCacheHeaderFilter.PROP_FILTER_PATTERN,
      propertyPrivate = false,
      value = { }),
  @Property(label = "Expires Time",
      description = "Time of day at which resources will expire. Must match SimpleDateFormat of 'HH:mm'.",
      name = AbstractExpiresHeaderFilter.PROP_EXPIRES_TIME,
      propertyPrivate = false),
  @Property(
        name = "webconsole.configurationFactory.nameHint",
        value = "Expires each month on the {expires.day-of-month} day at {expires.time} for Patterns: [{filter.pattern}]",
        propertyPrivate = true)
})
//@formatter:on
public class MonthlyExpiresHeaderFilter extends AbstractExpiresHeaderFilter {

    private static final String LAST = "LAST";

    @Property(label = "Expires Day", description = "Day of month on which content expires. "
            + "Use keyword 'LAST' to enable last day of month, as setting to 31 will generate errors in February.")
    static final String PROP_EXPIRES_DAY_OF_MONTH = "expires.day-of-month";

    private String dayOfMonth;

    @Override
    protected void adjustExpires(Calendar next) {

        if (StringUtils.equalsIgnoreCase(LAST, dayOfMonth)) {
            next.set(Calendar.DAY_OF_MONTH, next.getActualMaximum(Calendar.DAY_OF_MONTH));
        } else {
            next.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dayOfMonth));
        }
        if (next.before(Calendar.getInstance())) {
            next.add(Calendar.MONTH, 1);
        }
    }

    @Override
    protected void doActivate(ComponentContext context) throws Exception {
        super.doActivate(context);

        @SuppressWarnings("unchecked")
        Dictionary<String, Object> props = context.getProperties();
        dayOfMonth = PropertiesUtil.toString(props.get(PROP_EXPIRES_DAY_OF_MONTH), null);

        if (StringUtils.isBlank(dayOfMonth)) {
            throw new ConfigurationException(PROP_EXPIRES_DAY_OF_MONTH, "Day of month must be specified.");
        }

        if (!StringUtils.equalsIgnoreCase(LAST, dayOfMonth)) {
            // Make sure it's a valid value for Calendar.
            try {
                int intDay = Integer.parseInt(dayOfMonth);
                Calendar test = Calendar.getInstance();
                if (intDay < test.getMinimum(Calendar.DAY_OF_MONTH)) {
                    throw new ConfigurationException(PROP_EXPIRES_DAY_OF_MONTH,
                            "Day of month is smaller than minimum allowed value.");
                }
                if (intDay > test.getActualMaximum(Calendar.DAY_OF_MONTH)) {
                    throw new ConfigurationException(PROP_EXPIRES_DAY_OF_MONTH,
                            "Day of month is larger than least maximum allowed value.");
                }
            } catch (NumberFormatException ex) {
                throw new ConfigurationException(PROP_EXPIRES_DAY_OF_MONTH, "Day of month is not a valid value.");
            }
        }
    }
}
