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
package com.adobe.acs.commons.mcp.impl;

import com.adobe.acs.commons.mcp.McpLocalizationService;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component(service = {McpLocalizationService.class})
@Designate(ocd = McpLocalizationConfiguration.class)
public class McpLocalizationServiceImpl implements McpLocalizationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(McpLocalizationServiceImpl.class);

    private boolean localizationEnabled;

    private String overlayedLanguagesResourcePath;
    
    @Activate
    protected void activate(final McpLocalizationConfiguration config) throws Exception {
       LOGGER.debug("Start ACTIVATE GenericListService");
       
       this.localizationEnabled = config.localizationEnabled();
       validateAndAssignOverlayedLanguagePath(config.overlayedLanguagesResourcePath());

       LOGGER.debug("Config values are localizationEnabled:{}, overlayedLanguagesResourcePath:{} ", this.localizationEnabled, this.overlayedLanguagesResourcePath);

       LOGGER.debug("End ACTIVATE GenericListService");
    }
 
    private void validateAndAssignOverlayedLanguagePath(String overlayedLanguagesResourcePath) {

        if(StringUtils.startsWith(overlayedLanguagesResourcePath, "/apps") && StringUtils.endsWith(overlayedLanguagesResourcePath, "languages")){
            this.overlayedLanguagesResourcePath = overlayedLanguagesResourcePath;
        }else{
            LOGGER.warn("Not a valid overlayed languages resource path, thus ignoring it");
        }
    }

    @Override
    public boolean isLocalizationEnabled() {
        return this.localizationEnabled;
    }

    @Override
    public String getOverlayedLanguagesResourcePath() {
        return this.overlayedLanguagesResourcePath;
    }
}
