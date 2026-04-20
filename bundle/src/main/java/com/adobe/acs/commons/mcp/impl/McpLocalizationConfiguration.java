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

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "ACS AEM Commons - MCP Localization Configuration",
        description = "Configuration of MCP Localization such as enable location and overlayed languages path")
public @interface McpLocalizationConfiguration {

    @AttributeDefinition(name = "Enable Localization", description = "Indicates whether the Global Level localization of MCP FormFields is enabled or not.", type = AttributeType.BOOLEAN)
    boolean localizationEnabled() default false;

    @AttributeDefinition(name = "Overlayed Languages Path", description = "The overlayed languages resource path like '/libs/wcm/core/resources/languages' but should be from '/apps'. Please note only overlay the desired country or country_language nodes else authoring is suboptimal")
    String overlayedLanguagesResourcePath();
}
