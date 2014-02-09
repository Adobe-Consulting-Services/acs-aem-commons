/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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

package com.adobe.acs.commons.configuration.impl;

import com.adobe.acs.commons.configuration.OsgiConfigConstants;
import com.adobe.acs.commons.configuration.OsgiConfigHelper;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component(label = "ACS AEM Commons - OSGi Config Helper",
        description = "Helper utility in support of managing Sling Osgi Configurations",
        metatype = false)
@Service
public class OsgiConfigHelperImpl implements OsgiConfigHelper {
    private static final Logger log = LoggerFactory.getLogger(OsgiConfigHelperImpl.class);

    /**
     * {@inheritDoc}
     */
    public final String getPID(final Resource resource)  {
        final ValueMap properties = resource.adaptTo(ValueMap.class);
        final String pid = properties.get(OsgiConfigConstants.PN_PID, String.class);

        if(StringUtils.isBlank(pid)) {
            log.error("Resource [ {} ] must have non-blank property for: {}",
                    resource.getPath(), OsgiConfigConstants.PN_PID);
            return null;
        }

        final String configurationType = properties.get(
                OsgiConfigConstants.PN_CONFIGURATION_TYPE,
                OsgiConfigConstants.ConfigurationType.SINGLE.name());

        if(StringUtils.equalsIgnoreCase(OsgiConfigConstants.ConfigurationType.FACTORY.name(),
                configurationType)) {
            return pid + "-" + DigestUtils.md5Hex(resource.getPath());
        } else {
            return pid;
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasRequiredProperties(final Resource resource,
                                          final String[] requiredProperties) {
        final ValueMap properties = resource.adaptTo(ValueMap.class);

        for(final String requiredProperty : requiredProperties) {
            final String tmp = properties.get(requiredProperty, String.class);
            if(StringUtils.isBlank(tmp)) {
                return false;
            }
        }

        return true;
    }
}
