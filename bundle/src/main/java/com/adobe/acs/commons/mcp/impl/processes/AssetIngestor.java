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
package com.adobe.acs.commons.mcp.impl.processes;

import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.functions.CheckedConsumer;
import com.adobe.acs.commons.mcp.ProcessDefinition;
import com.adobe.acs.commons.mcp.ProcessInstance;
import com.adobe.acs.commons.mcp.form.FormField;
import com.adobe.acs.commons.mcp.form.PathfieldComponent;
import com.adobe.acs.commons.mcp.form.RadioComponent;
import com.adobe.acs.commons.mcp.model.FieldFormat;
import com.adobe.acs.commons.mcp.model.GenericReport;
import com.adobe.acs.commons.mcp.model.ValueFormat;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.commons.jcr.JcrUtil;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.AssetManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.mime.MimeTypeService;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public abstract class AssetIngestor extends ProcessDefinition {
    private final MimeTypeService mimetypeService;

    @SuppressWarnings("squid:S00115")
    public enum AssetAction {
        skip, version, replace
    }

    public AssetIngestor(MimeTypeService mimeTypeService) {
        this.mimetypeService = mimeTypeService;
    }

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

    protected static final String DEFAULT_FOLDER_TYPE = "sling:Folder";
    protected static final String CHANGED_BY_WORKFLOW = "changedByWorkflowProcess";

    AtomicInteger folderCount = new AtomicInteger();
    AtomicInteger assetCount = new AtomicInteger();
    AtomicInteger filesSkipped = new AtomicInteger();
    AtomicLong totalImportedData = new AtomicLong();

    @Override
    public void init() throws RepositoryException {
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

    @SuppressWarnings("squid:S00112")
    private void createAsset(Source source, String assetPath, ResourceResolver r, boolean versioning) throws Exception {
        r.adaptTo(Session.class).getWorkspace().getObservationManager().setUserData(CHANGED_BY_WORKFLOW);
        AssetManager assetManager = r.adaptTo(AssetManager.class);
        String type = mimetypeService.getMimeType(source.getName());
        if (versioning) {
            //is asset is null, no version gets created
            Asset asset = r.getResource(assetPath).adaptTo(Asset.class);
            //once you are past this first version, default behavior is to start numbering 1.0, 1.1 and so on
            assetManager.createRevision(asset, "initial version of asset", asset.getName());
            r.commit();
            r.refresh();
            //once version is committed we are safe to create, which only replaces the original version
        }
        assetManager.createAsset(assetPath, source.getStream(), type, false);
        r.commit();
        r.refresh();
        totalImportedData.accumulateAndGet(source.getLength(), (p,x) -> p+x);
        assetCount.incrementAndGet();
    }

    protected void handleExistingAsset(Source source, String assetPath, ResourceResolver r) throws Exception {
        switch (existingAssetAction) {
            case skip:
                //if skip then we only create asset if it doesn't exist
                if (r.getResource(assetPath) == null) {
                    createAsset(source, assetPath, r, false);
                } else {
                    filesSkipped.incrementAndGet();
                }
                break;
            case replace:
                //if replace we just create a new one and the old one goes away
                createAsset(source, assetPath, r, false);
                break;
            default:
                //only option left is replace, we'll save current version as a version and then replace it
                versionExistingAsset(source, assetPath, r);
        }
    }

    protected boolean createFolderNode(HierarchialElement el, ResourceResolver r) throws RepositoryException, PersistenceException {
        if (el == null || !el.isFolder()) {
            return false;
        }
        String folderPath = el.getNodePath();
        String name = el.getName();
        Session s = r.adaptTo(Session.class);
        if (s.nodeExists(folderPath)) {
            Node folderNode = s.getNode(folderPath);
            if (folderPath.equals(jcrBasePath) || (folderNode.hasProperty(JcrConstants.JCR_TITLE) && folderNode.getProperty(JcrConstants.JCR_TITLE).getString().equals(name))) {
                return false;
            } else {
                folderNode.setProperty(JcrConstants.JCR_TITLE, name);
                r.commit();
                r.refresh();
                return true;
            }
        }
        HierarchialElement parent = el.getParent();
        String parentPath;
        if (parent == null) {
            parentPath = jcrBasePath;
        } else {
            parentPath = parent.getNodePath();
        }
        if (!jcrBasePath.equals(parentPath)) {
            createFolderNode(parent, r);
        }
        Node child = s.getNode(parentPath).addNode(el.getNodeName(), DEFAULT_FOLDER_TYPE);
        folderCount.incrementAndGet();
        if (!folderPath.equals(jcrBasePath)) {
            child.setProperty(JcrConstants.JCR_TITLE, name);
        }
        r.commit();
        r.refresh();
        return true;
    }

    @SuppressWarnings("squid:S00112")
    private void versionExistingAsset(Source source, String assetPath, ResourceResolver r) throws Exception {
        createAsset(source, assetPath, r, r.getResource(assetPath) != null);
    }

    protected CheckedConsumer<ResourceResolver> importAsset(final Source source, ActionManager actionManager) {
        return (ResourceResolver r) -> {
            String path = source.getElement().getNodePath();
            createFolderNode(source.getElement().getParent(), r);
            actionManager.setCurrentItem(source.getElement().getItemName());
            handleExistingAsset(source, path, r);
        };
    }

    protected boolean canImportFile(Source source) {
        String name = source.getName().toLowerCase();
        if (minimumSize > 0 && source.getLength() < minimumSize) {
            return false;
        }
        if (maximumSize > 0 && source.getLength() > maximumSize) {
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

    protected boolean canImportFolder(HierarchialElement element) {
        String name = element.getName();
        if (ignoreFolderList.contains(name.toLowerCase())) {
            return false;
        } else {
            HierarchialElement parent = element.getParent();
            if (parent == null) {
                return true;
            } else {
                return canImportFolder(parent);
            }
        }
    }

    protected boolean canImportContainingFolder(HierarchialElement element) {
        HierarchialElement parent = element.getParent();
        if (parent == null) {
            return true;
        } else {
            return canImportFolder(parent);
        }
    }

    @SuppressWarnings("squid:S00115")
    enum ReportColumns {folder_count, asset_count, files_skipped, @FieldFormat(ValueFormat.storageSize) data_imported}

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

    protected interface Source {

        String getName();

        InputStream getStream() throws IOException;

        long getLength();

        HierarchialElement getElement();

    }

    protected interface HierarchialElement {

        boolean isFile();

        boolean isFolder();

        HierarchialElement getParent();

        String getName();

        String getItemName();

        Source getSource();

        String getJcrBasePath();

        default String getNodePath() {
            HierarchialElement parent = getParent();
            return (parent == null ? getJcrBasePath() : parent.getNodePath()) + "/" + getNodeName();
        }

        default String getNodeName() {
            String name = getName();
            if (isFile() && name.contains(".")) {
                String baseName = StringUtils.substringBeforeLast(name, ".");
                String extension = StringUtils.substringAfterLast(name, ".");
                return JcrUtil.createValidName(baseName) + "." + JcrUtil.createValidName(extension);
            } else {
                return JcrUtil.createValidName(name);
            }
        }
    }
}
