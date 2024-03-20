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
package com.adobe.acs.commons.mcp;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Service to retrieve cofiguration values related to MCP Localization. 
 * <p>
 * 
 * This is primarily used (or introduced) after "Localization" support in MCP, {@link com.adobe.acs.commons.mcp.form.FormField#localize()}
 * 
 * <p>
 * 
 * Thus helps to override the behavior at global level and provides overlayed languages resource path. Meaning evenif some Classes that you do NOT have control are annotated to turn localization ON, that can be supressed via these configurations, for e.g. {@link com.adobe.acs.commons.genericlists.GenericList.Item#getTitle()} 
 *
 */
@ProviderType
public interface McpLocalizationService {
   
    /**
     * Determines if Global-Level localization of MCP FormFields is "enabled". By default it will be disabled and can be enabled via OSGi configuration changes.
     *
     * @return true to support "Localization"
     */
    boolean isLocalizationEnabled();
   
   /**
    * Get the overlayed (or supported) languages resource path like '/libs/wcm/core/resources/languages' but in overlayed structure.
    * 
    * @return the overlayed languages resource path
    */
   String getOverlayedLanguagesResourcePath();
   
}
