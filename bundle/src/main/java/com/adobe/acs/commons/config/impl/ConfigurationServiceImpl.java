/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2014 Adobe
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
package com.adobe.acs.commons.config.impl;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.config.Configuration;
import com.adobe.acs.commons.config.ConfigurationService;
import com.adobe.acs.commons.replication.impl.ReplicateVersionImpl;
import com.day.cq.commons.inherit.HierarchyNodeInheritanceValueMap;
import com.day.cq.wcm.api.Page;

@Component(label = "ACS AEM Commons - config service",
        description = "gets configuration object of the path requested",
        immediate = false, metatype = true)
@Service
public class ConfigurationServiceImpl implements ConfigurationService {

    private static final Logger log = LoggerFactory
            .getLogger(ConfigurationServiceImpl.class);

    @Override
    public final Configuration getConfiguration(Page page, String configPagePath) {

        Page configpage = page.getPageManager().getPage(configPagePath);
        if (configpage != null) {
            log.info("config page found at " + configPagePath );
            return new ConfigurationImpl(configpage);
        }
        log.error("config page not found at " + configPagePath );
        return new NullConfigurationImpl();
    }

    @Override
    public final Configuration getConfiguration(Page page) {
        Resource contentResource = page.getContentResource();
        HierarchyNodeInheritanceValueMap hvm =
                new HierarchyNodeInheritanceValueMap(contentResource);
        String configPage = hvm.getInherited("configPage", "");
        if (!configPage.isEmpty()) {
            log.info("config page found for page " + page.getPath() );
            return new ConfigurationImpl(page.getPageManager().getPage(
                    configPage));
        }
        log.error("config page not found for " + page.getPath() );
        return new NullConfigurationImpl();
    }
}
