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
package com.adobe.acs.commons.mcp.impl.processes.asset;

import com.adobe.acs.commons.mcp.AuthorizedGroupProcessDefinitionFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import com.adobe.acs.commons.mcp.ProcessDefinitionFactory;
import com.adobe.acs.commons.util.RequireAem;

import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.apache.sling.commons.mime.MimeTypeService;

/**
 * Factory for asset import processes
 */
@Component
public class UrlAssetImportFactory extends AuthorizedGroupProcessDefinitionFactory<UrlAssetImport> {

    // Disable this feature on AEM as a Cloud Service
    @Reference(target="(distribution=classic)")
    RequireAem requireAem;

    @Reference
    private transient MimeTypeService mimeTypeService;

    @Reference
    private transient HttpClientBuilderFactory httpClientService;

    @Override
    protected UrlAssetImport createProcessDefinitionInstance() {
        return new UrlAssetImport(mimeTypeService, httpClientService);
    }

    @Override
    public String getName() {
        return "URL Asset Import";
    }

    @Override
    protected final String[] getAuthorizedGroups() {
        return AssetIngestor.AUTHORIZED_GROUPS;
    }    
}
