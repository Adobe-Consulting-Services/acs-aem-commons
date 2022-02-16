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
import com.adobe.acs.commons.fam.actions.Actions;
import com.adobe.acs.commons.mcp.form.FormField;
import com.adobe.acs.commons.mcp.ProcessDefinition;
import com.adobe.acs.commons.mcp.ProcessInstance;
import com.adobe.acs.commons.mcp.form.CheckboxComponent;
import com.adobe.acs.commons.mcp.form.PathfieldComponent;
import com.adobe.acs.commons.mcp.model.FieldFormat;
import com.adobe.acs.commons.mcp.model.GenericBlobReport;
import com.adobe.acs.commons.mcp.model.ValueFormat;
import com.adobe.acs.commons.mcp.util.FrozenAsset;
import com.adobe.acs.commons.util.visitors.TreeFilteringResourceVisitor;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.dam.api.Rendition;
import com.day.cq.dam.api.Revision;
import com.day.cq.dam.commons.util.DamUtil;
import java.io.Serializable;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import javax.jcr.RepositoryException;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Reports number and size of assets within a given folder structure.
 */
public class AssetReport extends ProcessDefinition implements Serializable {
    private static final long serialVersionUID = 7526472295622776160L;

    public static final transient String SHA1 = "dam:sha1";
    public static final String NAME = "Asset Report";

    @SuppressWarnings("squid:S00115")
    public enum Column {
        level, asset_count, subfolder_count,
        rendition_count, version_count, subasset_count,
        @FieldFormat(ValueFormat.storageSize)
        original_size,
        @FieldFormat(ValueFormat.storageSize)
        rendition_size,
        @FieldFormat(ValueFormat.storageSize)
        version_size,
        @FieldFormat(ValueFormat.storageSize)
        subasset_size,
        @FieldFormat(ValueFormat.storageSize)
        combined_size;
    }

    @FormField(
            name = "Folder",
            description = "Examines everying in this folder and all other subfolders below it",
            hint = "/content/dam",
            component = PathfieldComponent.FolderSelectComponent.class,
            options = {"default=/content/dam", "base=/content/dam"}
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
    private boolean includeSubassets = false;
    @FormField(
            name = "Include versions",
            description = "If checked, versions are counted and evaluated as part of the asset size.  This takes additional time to process",
            component = CheckboxComponent.class,
            options = {"checked"}
    )
    private boolean includeVersions = false;
    private transient int depthLimit;

    @Override
    public void init() throws RepositoryException {
        depthLimit = getDepth(baseFolder) + folderLevels;
    }

    public int getDepth(String path) {
        return (int) path.chars().filter(c -> c == '/').count();
    }

    @Override
    public void buildProcess(ProcessInstance instance, ResourceResolver rr) throws LoginException, RepositoryException {
        report.setName(instance.getName());
        instance.defineCriticalAction("Evaluate structure", rr, this::evaluateStructure);
        instance.defineAction("First pass", rr, this::examineAssets);
        instance.defineAction("Deep scan", rr, this::evaluateDeepStructure);
        instance.defineAction("Final pass", rr, this::examineAssets);
        String detail;
        if (includeSubassets && includeVersions) {
            detail = "full";
        } else if (includeSubassets || includeVersions) {
            detail = "partial";
        } else {
            detail = "light";
        }
        instance.getInfo().setDescription(baseFolder + " - " + detail);        
    }

    private final transient GenericBlobReport report = new GenericBlobReport();
    private final transient Map<String, EnumMap<Column, Long>> reportData = new TreeMap<>();

    private final transient Queue<String> assetList = new ConcurrentLinkedQueue<>();
    private final transient Queue<String> folderList = new ConcurrentLinkedQueue<>();

    public void evaluateStructure(ActionManager manager) {
        TreeFilteringResourceVisitor visitor = new TreeFilteringResourceVisitor();
        visitor.setBreadthFirstMode();
        visitor.setTraversalFilter(r -> visitor.isFolder(r) && getDepth(r.getPath()) < depthLimit);
        visitor.setLeafVisitor((r, depth) -> {
            if (isAsset(r)) {
                tabulate(getParentPath(r.getPath()), Column.asset_count, 1);
                assetList.add(r.getPath());
            } else if (visitor.isFolder(r)) {
                tabulate(getParentPath(r.getPath()), Column.subfolder_count, 1);
                setValue(r.getPath(), Column.level, depth + 1);
                folderList.add(r.getPath());
            }
        });
        visitor.setResourceVisitor((r, depth) -> {
            setValue(r.getPath(), Column.level, depth + 1);
            tabulate(getParentPath(r.getPath()), Column.subfolder_count, 1);
        });
        manager.deferredWithResolver(rr -> visitor.accept(rr.getResource(baseFolder)));
    }

    public void evaluateDeepStructure(ActionManager manager) {
        folderList.stream().peek(folderList::remove).forEach(folder -> {
            manager.deferredWithResolver(rr -> {
                Actions.setCurrentItem(folder);
                TreeFilteringResourceVisitor visitor = new TreeFilteringResourceVisitor();
                visitor.setBreadthFirstMode();
                visitor.setLeafVisitor((r, depth) -> {
                    if (isAsset(r)) {
                        tabulate(getParentPath(r.getPath()), Column.asset_count, 1);
                        assetList.add(r.getPath());
                    }
                });
                visitor.setResourceVisitor((r, depth) -> {
                    tabulate(getParentPath(r.getPath()), Column.subfolder_count, 1);
                });
                visitor.accept(rr.getResource(folder));
            });
        });
    }

    public void examineAssets(ActionManager manager) {
        assetList.stream().peek(assetList::remove).forEach(path -> {
            manager.deferredWithResolver(rr -> examineAsset(rr, path));
        });
        assetList.clear(); // Should already be empty but just in case...
    }

    public boolean isAsset(Resource r) {
        String nodeType = r.getValueMap().get(JcrConstants.JCR_PRIMARYTYPE, String.class);
        return DamConstants.NT_DAM_ASSET.equals(nodeType);
    }

    private String getParentPath(String path) {
        return path.substring(0, path.lastIndexOf('/'));
    }

    private void setValue(String path, Column field, long amount) {
        if (getDepth(path) < depthLimit) {
            synchronized (report) {
                EnumMap<Column, Long> row = getReportRow(path);
                row.put(field, amount);
            }
        }
    }

    private synchronized void tabulate(String path, Column counter, long amount) {
        if (getDepth(path) < depthLimit && path.length() >= baseFolder.length()) {
            synchronized (report) {
                EnumMap<Column, Long> row = getReportRow(path);
                if (row.containsKey(counter)) {
                    row.put(counter, row.get(counter) + amount);
                } else {
                    row.put(counter, amount);
                }
            }
        }
        if (path.length() > baseFolder.length()) {
            tabulate(getParentPath(path), counter, amount);
        }
    }

    @SuppressWarnings("squid:S00112")
    private void examineAsset(ResourceResolver rr, String assetPath) throws RepositoryException, Exception {
        Actions.setCurrentItem(assetPath);
        String folderPath = getParentPath(assetPath);
        Set<String> observedHashes = new HashSet<>();

        Asset asset = rr.getResource(assetPath).adaptTo(Asset.class);
        String hash = asset.getMetadataValue(SHA1);
        if (hash != null) {
            observedHashes.add(hash);
        }
        Map<String, Rendition> renditions = new HashMap<>();
        asset.listRenditions().forEachRemaining(r -> renditions.put(r.getName(), r));
        Rendition original = renditions.remove("original");
        tabulate(folderPath, Column.rendition_count, renditions.size());

        if (original != null) {
            long size = original.getSize();
            tabulate(folderPath, Column.original_size, size);
            tabulate(folderPath, Column.combined_size, size);
        }

        renditions.values().forEach(rendition -> {
            long size = rendition.getSize();
            tabulate(folderPath, Column.rendition_size, size);
            tabulate(folderPath, Column.combined_size, size);
        });

        if (includeSubassets) {
            DamUtil.getSubAssets(asset.adaptTo(Resource.class)).stream().forEach(subasset -> {
                tabulate(folderPath, Column.subasset_count, 1);
                long size = subasset.getRenditions().stream().collect(Collectors.summingLong(Rendition::getSize));
                tabulate(folderPath, Column.subasset_size, size);
                tabulate(folderPath, Column.combined_size, size);
            });
        }
        if (includeVersions) {
            for (Revision rev : asset.getRevisions(null)) {
                tabulate(folderPath, Column.version_count, 1);
                Asset assetVersion = FrozenAsset.createFrozenAsset(asset, rev);
                String versionHash = assetVersion.getMetadataValue(SHA1);
                if (versionHash != null) {
                    if (observedHashes.contains(versionHash)) {
                        return;
                    } else {
                        observedHashes.add(versionHash);
                    }
                }
                long size = getTotalAssetSize(assetVersion);
                tabulate(folderPath, Column.version_size, size);
                tabulate(folderPath, Column.combined_size, size);
            }
        }
    }

    private long getTotalAssetSize(Asset asset) {
        long size = asset.getRenditions().stream().collect(Collectors.summingLong(r -> r.getSize()));
        if (includeSubassets && !asset.isSubAsset()) {
            size += DamUtil.getSubAssets(asset.adaptTo(Resource.class)).stream().collect(Collectors.summingLong(this::getTotalAssetSize));
        }
        return size;
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
        report.persist(rr, instance.getPath() + "/jcr:content/report");
    }
}
