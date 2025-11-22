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

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Component;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.Dictionary;

import static com.adobe.acs.commons.http.headers.impl.AbstractDispatcherCacheHeaderFilter.PROP_DISPATCHER_FILTER_ENGINE;
import static com.adobe.acs.commons.http.headers.impl.AbstractDispatcherCacheHeaderFilter.PROP_DISPATCHER_FILTER_ENGINE_SLING;

@Component(configurationFactory = true)
public class PropertyBasedDispatcherMaxAgeHeaderFilter extends ResourceBasedDispatcherMaxAgeHeaderFilter {

    private static final Logger log = LoggerFactory.getLogger(PropertyBasedDispatcherMaxAgeHeaderFilter.class);

        public static final String PROP_PROPERTY_NAME = "property.name";

        public static final String PROP_INHERIT_PROPERTY_VALUE = "inherit.property.value";

    private String propertyName;
    private String inheritPropertyValue;

    @Override
    @SuppressWarnings("unchecked")
    protected boolean accepts(final HttpServletRequest request) {
        if (!super.accepts(request)) {
            log.debug("Not accepting request because it is not coming from the dispatcher.");
            return false;
        }
        if (request instanceof SlingHttpServletRequest) {
            SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;
            Resource resource = getResource(slingRequest);
            if (resource == null) {
                log.debug("Could not find resource for request, not accepting");
                return false;
            }
            String headerValue = resource.getValueMap().get(propertyName, String.class);
            if (!StringUtils.isBlank(headerValue) && !inheritPropertyValue.equals(headerValue)) {
                log.debug("Found a max age header value for request {} that is not the inherit value, accepting", resource.getPath());
                return true;
            }
            log.debug("Resource property is blank or INHERIT, not taking this filter ");
            return false;
        }
        return false;
    }

    @Override
    protected String getHeaderValue(HttpServletRequest request) {
        if (request instanceof SlingHttpServletRequest) {
            SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;
            Resource resource = getResource(slingRequest);
            if (resource != null) {
                String headerValue = resource.getValueMap().get(propertyName, String.class);
                if (isValidMaxAgeValue(headerValue)) {
                    return HEADER_PREFIX + headerValue;
                } else {
                    log.debug("Invalid value <{}> found in property <{}> for max-age header", headerValue, propertyName);
                }
            }
        }
        log.debug("An error occurred, falling back to the default max age value of this filter");
        return super.getHeaderValue(request);
    }

    @SuppressWarnings("squid:S1149")
    protected final void doActivate(ComponentContext context) throws Exception {
        super.doActivate(context);
        Dictionary<?, ?> properties = context.getProperties();

        propertyName = PropertiesUtil.toString(properties.get(PROP_PROPERTY_NAME), null);
        if (propertyName == null) {
            throw new ConfigurationException(PROP_PROPERTY_NAME, "Property name should be specified.");
        }
        inheritPropertyValue = PropertiesUtil.toString(properties.get(PROP_INHERIT_PROPERTY_VALUE), "INHERIT");
    }

    private boolean isValidMaxAgeValue(String headerValue) {
        try {
            Integer.parseInt(headerValue);
            return true;
        } catch(NumberFormatException e) {
            return false;
        }
    }

    public String toString() {
        return this.getClass().getName() + "[property-name:" + propertyName + ",fallback-max-age:" + super.getHeaderValue(null) + "]";
    }

}
