/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
package com.adobe.acs.commons.mcp.impl.processes.asset;

import com.adobe.acs.commons.mcp.AuthorizedGroupProcessDefinitionFactory;
import com.adobe.acs.commons.mcp.ProcessDefinition;
import com.adobe.acs.commons.mcp.ProcessDefinitionFactory;
import com.amazonaws.services.s3.AmazonS3Client;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.commons.mime.MimeTypeService;

@Component
@Service(ProcessDefinitionFactory.class)
public class S3AssetIngestorFactory extends AuthorizedGroupProcessDefinitionFactory<ProcessDefinition> {

    @Reference
    MimeTypeService mimetypeService;

    @Override
    public String getName() {
        return "S3 Asset Ingestor";
    }

    @Override
    public ProcessDefinition createProcessDefinitionInstance() {
        return new S3AssetIngestor(mimetypeService);
    }

    @Override
    public boolean isAllowed(User user) {
        if (super.isAllowed(user)) {
            // check if S3 SDK is available
            try {
                new AmazonS3Client();
                return true;
            } catch (NoClassDefFoundError e) {
                // ignore
            }
        }
        return false;
    }
    
    @Override
    protected final String[] getAuthorizedGroups() {
        return AssetIngestor.AUTHORIZED_GROUPS;
    }    
}
