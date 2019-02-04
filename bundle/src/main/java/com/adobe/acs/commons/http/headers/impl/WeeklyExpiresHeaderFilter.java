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


import java.util.Calendar;
import java.util.Dictionary;

import javax.servlet.Filter;

import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

//@formatter:off
@Component(
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        service = Filter.class,
        factory = "WeeklyExpiresHeaderFilter",
        property = {
                "webconsole.configurationFactory.nameHint" + "=" + "Expires Each week on day {expires.day-of-week} at {expires.time} for Patterns: [{filter.pattern}]"
        }
)
@Designate(ocd=WeeklyExpiresHeaderFilter.Config.class,factory=true)
//@formatter:on
public class WeeklyExpiresHeaderFilter extends AbstractExpiresHeaderFilter {

    @ObjectClassDefinition(
            name = "ACS AEM Commons - Dispatcher Expires Header - Weekly",
            description = "Adds an Expires header to content to enable Dispatcher TTL support."
    )
    public @interface Config {

        @AttributeDefinition(
                name = "Filter Patterns",
                description = "Patterns on which to apply this Expires rule.",
                cardinality = Integer.MAX_VALUE
        )
        String[] filter_pattern();

        @AttributeDefinition(
                name = "Expires Time",
                description = "Time of day at which resources will expire. Must match SimpleDateFormat of 'HH:mm'."
        )
        String expires_time();

        @AttributeDefinition(
                name = "Expires Day",
                description = "Day of week on which content expires.",
                options = {
                        @Option(value = "" + Calendar.SUNDAY, label = "Sunday"),
                        @Option(value = "" + Calendar.MONDAY, label = "Monday"),
                        @Option(value = "" + Calendar.TUESDAY, label = "Tuesday"),
                        @Option(value = "" + Calendar.WEDNESDAY, label = "Wednesday"),
                        @Option(value = "" + Calendar.THURSDAY, label = "Thursday"),
                        @Option(value = "" + Calendar.FRIDAY, label = "Friday"),
                        @Option(value = "" + Calendar.SATURDAY, label = "Saturday"),
                }
        )
        int expires_day$_$of$_$week();

    }

    static final String PROP_EXPIRES_DAY_OF_WEEK = "expires.dayofweek";

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
            throw new ConfigurationException("expires.dayofweek", "Day of week must be valid value from Calendar DAY_OF_WEEK attribute.");
        }
    }
}
