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
package com.adobe.acs.commons.mcp.impl.processes;

import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.fam.actions.Actions;
import com.adobe.acs.commons.functions.CheckedConsumer;
import com.adobe.acs.commons.mcp.HiddenProcessDefinition;
import com.adobe.acs.commons.mcp.ProcessDefinition;
import com.adobe.acs.commons.mcp.ProcessInstance;
import com.adobe.acs.commons.mcp.form.FormField;
import com.adobe.acs.commons.mcp.form.PathfieldComponent;
import com.adobe.acs.commons.mcp.form.RadioComponent;
import com.adobe.acs.commons.mcp.model.FieldFormat;
import com.adobe.acs.commons.mcp.model.GenericReport;
import com.adobe.acs.commons.mcp.model.ValueFormat;
import com.day.cq.commons.jcr.JcrUtil;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.AssetManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.mime.MimeTypeService;

/**
 * Asset Ingestor reads a directory structure recursively and imports it as-is into AEM.
 */
public class AssetIngestor implements ProcessDefinition, HiddenProcessDefinition {

    private final MimeTypeService mimetypeService;

    public AssetIngestor(MimeTypeService mimeTypeService) {
        this.mimetypeService = mimeTypeService;
    }
    
    public static enum AssetAction {
        skip, version, replace
    }
    
    @FormField(
            name = "Source",
            description = "Source folder for content ingestion",
            hint = "/var/mycontent, /tmp, /mnt/all_the_things, ...",
            required = true
    )    
    String fileBasePath;
    File baseFolder;
    
    @FormField(
            name = "Target JCR Folder",
            description = "Base folder for ingestion",
            hint = "/content/dam",
            component = PathfieldComponent.FolderSelectComponent.class,
            required = true,
            options = {"default=/content/dam", "base=/content/dam"}
    )
    String jcrBasePath;
    @FormField(
            name = "Ignore folders",
            description = "List of folder names to be ignored",
            hint = "tmp,.DS_STORE",
            options = {"default=tmp,ds_store,.ds_store,.thumbs,.appledouble"}
    )
    String ignoreFolders;
    List<String> ignoreFolderList;

    @FormField(
            name = "Ignore files",
            description = "List of file names to ignore",
            hint = "full file names, comma separated",
            options = {"default=ds_store,.ds_store"}
    )
    String ignoreFiles;
    List<String> ignoreFileList;
    
    @FormField(
            name = "Ignore extensions",
            description = "List of file extensions to ignore",
            hint = "mp4,txt, etc.",
            options = {"default=txt,html,css,js,thm,exe,db"}
    )
    String ignoreExtensions;
    List<String> ignoreExtensionList;
    
    @FormField(
            name = "Existing action",
            description = "What to do if an asset exists",
            component = RadioComponent.EnumerationSelector.class,
            options={"default=skip","vertical"}
    )    
    AssetAction existingAssetAction;
    @FormField(
            name = "Minimum size",
            description = "Min size to import (in bytes), 0=none",
            hint = "1024...",
            options = {"default=1024"}
    )
    long minimumSize;
    @FormField(
            name = "Maximum size",
            description = "Max size to import (in bytes), 0=none",
            hint = "1gb = 1073741824",
            options = {"default=1073741824"}
    )
    long maximumSize;

    private static final String DEFAULT_FOLDER_TYPE = "sling:Folder";
    private static final String JCR_TITLE = "jcr:title";
    private static final String CHANGED_BY_WORKFLOW = "changedByWorkflowProcess";
    
    int folderCount = 0;
    int assetCount = 0;
    int filesSkipped = 0;
    long totalImportedData = 0;
    
    @Override
    public String getName() {
        return "Asset Ingestor";
    }
    
    @Override
    public void init() throws RepositoryException {
        baseFolder = new File(fileBasePath);
        if (!baseFolder.exists()) {
            throw new RepositoryException("Source folder does not exist!");
        }
        if (ignoreFolders == null) {
            ignoreFolders = "";
        }
        ignoreFolderList = Arrays.asList(ignoreFolders.trim().toLowerCase().split(","));
        if (ignoreFiles == null) {
            ignoreFiles = "";
        }
        ignoreFileList = Arrays.asList(ignoreFiles.trim().toLowerCase().split(","));
        if (ignoreExtensions == null) {
            ignoreExtensions = "";
        }
        ignoreExtensionList = Arrays.asList(ignoreExtensions.trim().toLowerCase().split(","));
    }

    @Override
    public void buildProcess(ProcessInstance instance, ResourceResolver rr) throws LoginException, RepositoryException {
        instance.getInfo().setDescription(fileBasePath + "->" + jcrBasePath);
        instance.defineCriticalAction("Create Folders", rr, this::createFolders);
        instance.defineCriticalAction("Import Assets", rr, this::importAssets);
    }
    
    private void createFolders(ActionManager manager) throws IOException {
        manager.deferredWithResolver(r->{
            manager.setCurrentItem(fileBasePath);
            Files.walk(baseFolder.toPath()).map(Path::toFile).filter(File::isDirectory).filter(this::canImportFolder).forEach(f->{
                manager.deferredWithResolver(Actions.retry(10, 100, rr-> {
                    manager.setCurrentItem(f.getPath());
                    createFolderNode(folderToNodePath(f), f, rr);
                }));
            });
        });
    }

    private String folderToNodePath(File current) {
        if (baseFolder.equals(current)) {
            return jcrBasePath;
        } else {
            return folderToNodePath(current.getParentFile()) + "/" + JcrUtil.createValidName(current.getName());
        }
    }
    
    private boolean createFolderNode(String node, File folder, ResourceResolver r) throws RepositoryException, PersistenceException {
        Session s = r.adaptTo(Session.class);
        if (s.nodeExists(node)) {
            Node folderNode = s.getNode(node);
            if (folderNode.hasProperty(JCR_TITLE) && folderNode.getProperty(JCR_TITLE).getString().equals(folder.getName())) {
                return false;
            } else {
                folderNode.setProperty(JCR_TITLE, folder.getName());
                r.commit();
                r.refresh();
                return true;
            }
        }
        String parentNode = node.substring(0, node.lastIndexOf("/"));
        String childNode = node.substring(node.lastIndexOf("/") + 1);
        if (!folder.getParentFile().equals(baseFolder)) {
            createFolderNode(parentNode, folder.getParentFile(), r);
        }
        Node child = s.getNode(parentNode).addNode(childNode, DEFAULT_FOLDER_TYPE);
        folderCount++;
        child.setProperty(JCR_TITLE, folder.getName());
        r.commit();
        r.refresh();
        return true;
    }

    private void importAssets(ActionManager manager) throws IOException {
        manager.deferredWithResolver(rr->{
            Actions.setCurrentItem(fileBasePath);
            Files.walk(baseFolder.toPath()).map(Path::toFile).filter(File::isFile).filter(f->canImportFolder(f.getParentFile())).forEach(f->{
                if (canImportFile(f)) {
                    manager.deferredWithResolver(Actions.retry(5, 25, importFile(f)));
                } else {
                    filesSkipped++;
                }
            });        
        });
    }

    private boolean canImportFolder(File f) {
        if (f.equals(baseFolder)) {
            return true;
        }
        if (ignoreFolderList.contains(f.getName().toLowerCase())) {
            return false;
        }
        return canImportFolder(f.getParentFile());
    }
    
    private boolean canImportFile(File f) {
        String name = f.getName().toLowerCase();
        if (minimumSize > 0 && f.length() < minimumSize) {
            return false;
        }
        if (maximumSize > 0 && f.length() > maximumSize) {
            return false;
        }
        if (name.startsWith(".") || ignoreFileList.contains(name)) {
            return false;
        }
        if (name.contains(".")) {
            int extPos = name.lastIndexOf('.');
            String ext = name.substring(extPos+1);
            if (ignoreExtensionList.contains(ext)) {
                return false;
            }
        }
        return true;
    }
    
    private CheckedConsumer<ResourceResolver> importFile(final File sourceFile) {
        return (ResourceResolver r) -> {
            String basePath = folderToNodePath(sourceFile.getParentFile());
            createFolderNode(basePath, sourceFile.getParentFile(), r);
            String destPath = basePath + "/" + sourceFile.getName();
            Actions.setCurrentItem(destPath);
            handleExistingAsset(sourceFile, destPath, r);
        };
    }

    private void createAsset(File sourceFile, String assetPath, ResourceResolver r, boolean versioning) throws Exception {
        r.adaptTo(Session.class).getWorkspace().getObservationManager().setUserData(CHANGED_BY_WORKFLOW);
        AssetManager assetManager = r.adaptTo(AssetManager.class);
        String type = mimetypeService.getMimeType(sourceFile.getName());
        if (versioning) {
            //is asset is null, no version gets created
            Asset asset = r.getResource(assetPath).adaptTo(Asset.class);
            //once you are past this first version, default behavior is to start numbering 1.0, 1.1 and so on
            assetManager.createRevision(asset, "initial version of asset", asset.getName());
            r.commit();
            r.refresh();
            //once version is committed we are safe to create, which only replaces the original version
        }
        assetManager.createAsset(assetPath, new FileInputStream(sourceFile), type, false);
        r.commit();
        r.refresh();
        totalImportedData += sourceFile.length();
        assetCount++;
    }

    private void handleExistingAsset(File sourceFile, String assetPath, ResourceResolver r) throws Exception {
        switch (existingAssetAction) {
            case skip:
                //if skip then we only create asset if it doesn't exist
                if (r.getResource(assetPath) == null) {
                    createAsset(sourceFile, assetPath, r, false);
                } else {
                    filesSkipped++;
                }
                break;
            case replace:
                //if replace we just create a new one and the old one goes away
                createAsset(sourceFile, assetPath, r, false);
                break;
            case version:
                //only option left is replace, we'll save current version as a version and then replace it
                versionExistingAsset(sourceFile, assetPath, r);
        }
    }

    private void versionExistingAsset(File sourceFile, String assetPath, ResourceResolver r) throws Exception {
        createAsset(sourceFile, assetPath, r, r.getResource(assetPath) != null);
    }    

    enum ReportColumns {folder_count, asset_count, files_skipped, @FieldFormat(ValueFormat.storageSize) data_imported};
    GenericReport report = new GenericReport();
    @Override
    public void storeReport(ProcessInstance instance, ResourceResolver rr) throws RepositoryException, PersistenceException {
        EnumMap<ReportColumns, Object> values = new EnumMap<>(ReportColumns.class);
        List<EnumMap<ReportColumns,Object>> rows = new ArrayList<>();
        rows.add(values);
        values.put(ReportColumns.folder_count, folderCount);
        values.put(ReportColumns.asset_count, assetCount);
        values.put(ReportColumns.files_skipped, filesSkipped);
        values.put(ReportColumns.data_imported, totalImportedData);
        report.setRows(rows, ReportColumns.class);
        report.persist(rr, instance.getPath() + "/jcr:content/report");
    }    
}