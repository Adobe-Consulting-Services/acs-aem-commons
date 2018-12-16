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

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

//@formatter:off
@Component(property = { "webconsole.configurationFactory.nameHint" + "="
      + "Expires Daily at: {expires.time} for Patterns: [{filter.pattern}]", }, factory = "com.adobe.acs.commons.http.headers.impl.DailyExpiresHeaderFilter", configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = DailyExpiresHeaderFilter.Config.class, factory=true)
// @formatter:on
public class DailyExpiresHeaderFilter extends AbstractExpiresHeaderFilter {
   
   @ObjectClassDefinition(name = "ACS AEM Commons - Dispatcher Expires Header - Daily", description = "Adds an Expires header to content to enable Dispatcher TTL support.")
   public @interface Config {

      @AttributeDefinition(name = "Filter Patterns", description = "Patterns on which to apply this Expires rule.", cardinality = Integer.MAX_VALUE)
      String[] filter_pattern();

      @AttributeDefinition(name = "Expires Time", description = "Time each day at which resources will expire. Must match SimpleDateFormat of 'HH:mm'.")
      String expires_time();

   }

   @Override
   protected void adjustExpires(Calendar next) {
      if (next.before(Calendar.getInstance())) {
         next.add(Calendar.DAY_OF_MONTH, 1);
      }
   }
}
