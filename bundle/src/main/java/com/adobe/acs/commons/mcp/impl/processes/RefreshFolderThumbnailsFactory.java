/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2018 Adobe
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
package com.adobe.acs.commons.mcp.impl.processes;

import com.adobe.acs.commons.mcp.ProcessDefinitionFactory;
import com.day.cq.contentsync.handler.util.RequestResponseFactory;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.engine.SlingRequestProcessor;

@Component
@Service(ProcessDefinitionFactory.class)
public class RefreshFolderThumbnailsFactory extends ProcessDefinitionFactory<RefreshFolderTumbnails> {
    @Reference
    private SlingRequestProcessor slingProcessor;
    
    @Reference
    RequestResponseFactory reqRspFactory;
    
    @Override
    public String getName() {
        return "Refresh asset folder thumbnails";
    }

    @Override
    protected RefreshFolderTumbnails createProcessDefinitionInstance() {
        return new RefreshFolderTumbnails(reqRspFactory, slingProcessor);
    }
    
}
