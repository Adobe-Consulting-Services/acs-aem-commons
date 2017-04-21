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
import org.apache.felix.scr.annotations.Service;

import java.util.Calendar;


//@formatter:off
@Component(
    label = "ACS AEM Commons - Dispatcher Expires Header - Daily",
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
      description = "Time each day at which resources will expire. Must match SimpleDateFormat of 'HH:mm'.",
      name = AbstractExpiresHeaderFilter.PROP_EXPIRES_TIME,
      propertyPrivate = false),
  @Property(
        name = "webconsole.configurationFactory.nameHint",
        value = "Expires Daily at: {expires.time} for Patterns: [{filter.pattern}]",
        propertyPrivate = true)
})
//@formatter:on
public class DailyExpiresHeaderFilter extends AbstractExpiresHeaderFilter {

    @Override
    protected void adjustExpires(Calendar next) {
        if (next.before(Calendar.getInstance())) {
            next.add(Calendar.DAY_OF_MONTH, 1);
        }
    }
}
