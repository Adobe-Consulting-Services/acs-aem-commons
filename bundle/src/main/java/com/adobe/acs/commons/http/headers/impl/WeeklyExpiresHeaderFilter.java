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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;

import java.util.Calendar;
import java.util.Dictionary;

//@formatter:off
@Component(
    label = "ACS AEM Commons - Dispatcher Expires Header - Weekly",
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
        value = "Expires Each week on day {expires.day-of-week} at {expires.time} for Patterns: [{filter.pattern}]",
        propertyPrivate = true)
})
//@formatter:on
public class WeeklyExpiresHeaderFilter extends AbstractExpiresHeaderFilter {

    
    @Property(
            label = "Expires Day",
            description = "Day of week on which content expires.",
            options = {
                    @PropertyOption(name = "" + Calendar.SUNDAY, value = "Sunday"),
                    @PropertyOption(name = "" + Calendar.MONDAY, value = "Monday"),
                    @PropertyOption(name = "" + Calendar.TUESDAY, value = "Tuesday"),
                    @PropertyOption(name = "" + Calendar.WEDNESDAY, value = "Wednesday"),
                    @PropertyOption(name = "" + Calendar.THURSDAY, value = "Thursday"),
                    @PropertyOption(name = "" + Calendar.FRIDAY, value = "Friday"),
                    @PropertyOption(name = "" + Calendar.SATURDAY, value = "Saturday"),
            })
    static final String PROP_EXPIRES_DAY_OF_WEEK = "expires.day-of-week";

    private int dayOfWeek;

    @Override
    protected void adjustExpires(Calendar next) {
        next.set(Calendar.DAY_OF_WEEK, dayOfWeek);
        if (next.before(Calendar.getInstance())) {
            next.add(Calendar.DAY_OF_WEEK, next.getMaximum(Calendar.DAY_OF_WEEK));;
        }
    }

    @Override
    protected void doActivate(ComponentContext context) throws Exception {
        super.doActivate(context);

        @SuppressWarnings("unchecked")
        Dictionary<String, Object> props = context.getProperties();
        dayOfWeek = PropertiesUtil.toInteger(props.get(PROP_EXPIRES_DAY_OF_WEEK), -1);
        if (dayOfWeek < Calendar.SUNDAY || dayOfWeek > Calendar.SATURDAY) {
            throw new ConfigurationException(PROP_EXPIRES_DAY_OF_WEEK, "Day of week must be valid value from Calendar DAY_OF_WEEK attribute.");
        }
    }
}
