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
package com.adobe.acs.commons.http.headers.impl;

import org.apache.sling.api.SlingHttpServletRequest;
import org.osgi.service.component.annotations.Component;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;

import javax.servlet.http.HttpServletRequest;
import java.util.Dictionary;
import java.util.Enumeration;

//@formatter:off
@Component(
      label = "ACS AEM Commons - Dispacher Cache Control Header - Max Age",
      description = "Adds a Cache-Control max-age header to content to enable Dispatcher TTL support.",
      metatype = true,
      configurationFactory = true,
      policy = ConfigurationPolicy.REQUIRE)
//@formatter:on
public class DispatcherMaxAgeHeaderFilter extends AbstractDispatcherCacheHeaderFilter {

    protected static final String CACHE_CONTROL_NAME = "Cache-Control";

    @Property(label = "Cache-Control Max Age",
            description = "Max age value (in seconds) to put in Cache Control header.")
    public static final String PROP_MAX_AGE = "max.age";

    protected static final String HEADER_PREFIX = "max-age=";

    private long maxage;

    @Override
    protected String getHeaderName() {
        return CACHE_CONTROL_NAME;
    }

    @Override
    protected String getHeaderValue(HttpServletRequest request) {
        return HEADER_PREFIX + maxage;
    }

    @Override
    protected void doActivate(ComponentContext context) throws Exception {
        Dictionary<?, ?> properties = context.getProperties();
        maxage = PropertiesUtil.toLong(properties.get(PROP_MAX_AGE), -1);
        if (maxage < 0) {
            throw new ConfigurationException(PROP_MAX_AGE, "Max Age must be specified and greater than 0.");
        }
    }

    public String toString() {
        return this.getClass().getName() + "[" + getHeaderValue(null) + "]";
    }
}
