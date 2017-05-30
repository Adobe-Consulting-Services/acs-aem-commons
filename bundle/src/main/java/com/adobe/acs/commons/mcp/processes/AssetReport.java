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

import com.adobe.acs.commons.mcp.form.FormField;
import com.adobe.acs.commons.mcp.ProcessDefinition;
import com.adobe.acs.commons.mcp.ProcessInstance;
import com.adobe.acs.commons.mcp.model.GenericReport;
import com.adobe.acs.commons.mcp.form.CheckboxComponent;
import com.adobe.acs.commons.mcp.form.PathfieldComponent;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.jcr.RepositoryException;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.wrappers.ValueMapDecorator;

/**
 * Reports number and size of assets within a given folder structure.
 */
@Component
@Service(ProcessDefinition.class)
public class AssetReport implements ProcessDefinition {
    public static enum Column {
        total_asset_count, total_subfolder_count, 
        total_asset_renditions, total_asset_versions, total_subassets,
        total_original_size, total_version_size, total_subasset_size, total_combined_size, 
        local_asset_count, local_size, local_subfolder_count,
        local_asset_renditions, local_asset_versions, local_subassets,
        local_original_size, local_version_size, local_subasset_size, local_combined_size
    }
    GenericReport report = new GenericReport();
    Map<String, EnumMap<Column, Long>> reportData = new LinkedHashMap<>();

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
    
    private EnumMap<Column, Long> getReportRow(String path) {
        if (!reportData.containsKey(path)) {
            reportData.put(path, new EnumMap<>(Column.class));
        }
        return reportData.get(path);
    }

    @Override
    public void storeReport(ProcessInstance instance, ResourceResolver rr) throws RepositoryException, PersistenceException {        
        report.setRows(reportData, "Path", Column.class);
        report.persist(rr, instance.getPath()+"/jcr:content/report");
    }
}