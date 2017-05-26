/*
 * Copyright 2017 Adobe.
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
package com.adobe.acs.commons.mcp.processes;

import com.adobe.acs.commons.mcp.FormField;
import com.adobe.acs.commons.mcp.ProcessDefinition;
import com.adobe.acs.commons.mcp.ProcessInstance;
import com.adobe.acs.commons.mcp.model.CheckboxComponent;
import com.adobe.acs.commons.mcp.model.PathfieldComponent;
import javax.jcr.RepositoryException;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Reports number and size of assets within a given folder structure.
 */
@Component
@Service(ProcessDefinition.class)
public class AssetReport implements ProcessDefinition {

    @FormField(
            name = "Folder",
            description = "Examines everying in this folder and all other subfolders below it",
            hint = "/content/dam",
            component = PathfieldComponent.FolderSelectComponent.class,
            options = {"default=/content/dam", "base=/cotent/dam"}
    )
    private String baseFolder;
    @FormField(
            name = "Levels",
            description = "Determines how many levels down are included in report summary -- all levels below are rolled up into that the level.",
            hint = "5",
            options = {"default=5"}
    )
    private int folderLevels;
    @FormField(
            name = "Include subassets",
            description = "If checked, subassets are counted and evaluated as part of the total folder size.  This takes additional time to process.",
            component = CheckboxComponent.class,
            options = {"checked"}
    )
    private boolean includeSubassets;
    @FormField(
            name = "Include versions",
            description = "If checked, versions are counted and evaluated as part of the asset size.  This takes additional time to process",
            component = CheckboxComponent.class,
            options = {"checked"}
    )
    private boolean includeVersions;

    @Override
    public void init() throws RepositoryException {
    }

    @Override
    public String getName() {
        return "Asset Report";
    }

    @Override
    public void buildProcess(ProcessInstance instance, ResourceResolver rr) throws LoginException, RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void storeReport(ProcessInstance instance, ResourceResolver rr) throws RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}