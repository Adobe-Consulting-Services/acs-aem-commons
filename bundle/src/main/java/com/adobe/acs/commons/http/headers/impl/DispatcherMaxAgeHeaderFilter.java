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

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

//@formatter:off
@Component(
      factory = "DispatcherMaxAgeHeaderFilter",
      configurationPolicy = ConfigurationPolicy.REQUIRE, property= {
            "webconsole.configurationFactory.nameHint" + "=" + "Max Age: {max.age} for Patterns: [{filter.pattern}]"
      })
@Designate(ocd=DispatcherMaxAgeHeaderFilter.Config.class)
//@formatter:on
public class DispatcherMaxAgeHeaderFilter extends AbstractDispatcherCacheHeaderFilter {

    protected static final String CACHE_CONTROL_NAME = "Cache-Control";
    
    @ObjectClassDefinition( name = "ACS AEM Commons - Dispacher Cache Control Header - Max Age",
      description = "Adds a Cache-Control max-age header to content to enable Dispatcher TTL support.")
    public @interface Config {
        @AttributeDefinition(name = "Cache-Control Max Age",
                description = "Max age value (in seconds) to put in Cache Control header.")
           int max_age();
        @AttributeDefinition(name = "Filter Patterns",
                description = "Patterns on which to apply this Max Age cache-control rule.",
                cardinality = Integer.MAX_VALUE)
        String[] filter_pattern();
    }

    private static final String HEADER_PREFIX = "max-age=";

    private long maxage;

    @Override
    protected String getHeaderName() {
        return CACHE_CONTROL_NAME;
    }

    @Override
    protected String getHeaderValue() {
        return HEADER_PREFIX + maxage;
    }
    
   @Override
	protected void doActivate(ComponentContext context) throws Exception {
		// TODO Auto-generated method stub
		
	}

   @Activate
    protected void activate(DispatcherMaxAgeHeaderFilter.Config config) throws Exception {
         maxage = config.max_age();
        if (maxage < 0) {
            throw new ConfigurationException("max.age", "Max Age must be specified and greater than 0.");
        }
    }

    public String toString() {
        return this.getClass().getName() + "[" + getHeaderValue() + "]";
    }
}
