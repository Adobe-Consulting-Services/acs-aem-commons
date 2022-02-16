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

import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.fam.actions.ActionBatch;
import com.adobe.acs.commons.functions.CheckedFunction;
import com.adobe.acs.commons.mcp.ProcessDefinition;
import com.adobe.acs.commons.mcp.ProcessInstance;
import com.adobe.acs.commons.mcp.form.CheckboxComponent;
import com.adobe.acs.commons.mcp.form.FormField;
import com.adobe.acs.commons.mcp.form.PathfieldComponent;
import com.adobe.acs.commons.mcp.form.RadioComponent;
import com.adobe.acs.commons.mcp.model.GenericBlobReport;
import com.adobe.acs.commons.util.visitors.TreeFilteringResourceVisitor;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.contentsync.handler.util.RequestResponseFactory;
import com.day.cq.dam.api.DamConstants;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.engine.SlingRequestProcessor;

/**
 * Replace folder thumbnails under a user-definable set of circumstances As a business user, I would like an easy way to
 * scan and repair missing thumbnails, or just regenerate all thumbnails under a given tree of the DAM.
 */
public class RefreshFolderTumbnails extends ProcessDefinition {

    protected static enum ThumbnailScanLogic {
        MISSING(RefreshFolderTumbnails::isThumbnailMissing),
        PLACEHOLDERS(RefreshFolderTumbnails::isThumbnailMissing,
                RefreshFolderTumbnails::isPlaceholderThumbnail),
        OUTDATED(RefreshFolderTumbnails::isThumbnailMissing,
                RefreshFolderTumbnails::isPlaceholderThumbnail,
                RefreshFolderTumbnails::isThumbnailContentsOutdated),
        ALL_AUTOMATIC_OR_MISSING(RefreshFolderTumbnails::isThumbnailMissing,
                RefreshFolderTumbnails::isThumbnailAutomatic),
        ALL(r -> true);

        CheckedFunction<Resource, Boolean> test;

        @SuppressWarnings("squid:UnusedPrivateMethod")
        private ThumbnailScanLogic(CheckedFunction<Resource, Boolean>... tests) {
            this.test = CheckedFunction.or(tests);
        }

        @SuppressWarnings("squid:S00112")
        public boolean shouldReplace(Resource r) throws Exception {
            return this.test.apply(r);
        }
    }

    public static final String FOLDER_THUMBNAIL = "/jcr:content/folderThumbnail";

    private static Map<String, Object> THUMBNAIL_PARAMS = new HashMap<>();

    static {
        THUMBNAIL_PARAMS.put("width", "200");
        THUMBNAIL_PARAMS.put("height", "120");
    }

    private static final int PLACEHOLDER_SIZE = 1024;

    private RequestResponseFactory requestFactory;
    private SlingRequestProcessor slingProcessor;

    @FormField(name = "Starting Path",
            component = PathfieldComponent.FolderSelectComponent.class)
    private String startingPath = "/content/dam";

    @FormField(name = "Mode",
            component = RadioComponent.EnumerationSelector.class,
            options = "default=placeholders"
    )
    private ThumbnailScanLogic scanMode = ThumbnailScanLogic.PLACEHOLDERS;

    @FormField(name = "Dry Run",
            component = CheckboxComponent.class,
            options = "checked"
    )
    private boolean dryRun = true;

    private transient List<String> foldersToReplace = Collections.synchronizedList(new ArrayList<>());

    public RefreshFolderTumbnails(RequestResponseFactory reqRspFactory, SlingRequestProcessor slingProcessor) {
        this.requestFactory = reqRspFactory;
        this.slingProcessor = slingProcessor;
    }

    @Override
    public void init() {
        // Nothing to do here
    }

    @Override
    public void buildProcess(ProcessInstance instance, ResourceResolver rr) throws LoginException, RepositoryException {
        instance.defineCriticalAction("Scan folders", rr, this::scanFolders);
        if (!dryRun) {
            instance.defineAction("Remove old thumbnails", rr, this::removeOldThumbnails);
            instance.defineAction("Rebuild thumbnails", rr, this::rebuildThumbnails);
        }
    }

    public static enum ReportColumns {
        PATH, ACTION, DESCRIPTION
    }

    List<EnumMap<ReportColumns, String>> reportData = Collections.synchronizedList(new ArrayList<>());

    private void record(String path, String action, String description) {
        EnumMap<ReportColumns, String> row = new EnumMap<ReportColumns, String>(ReportColumns.class);
        row.put(ReportColumns.PATH, path);
        row.put(ReportColumns.ACTION, action);
        row.put(ReportColumns.DESCRIPTION, description);
        reportData.add(row);
    }

    @Override
    public void storeReport(ProcessInstance instance, ResourceResolver rr) throws RepositoryException, PersistenceException {
        GenericBlobReport report = new GenericBlobReport();
        report.setName("Rebuild thumbnails " + startingPath);
        report.setRows(reportData, ReportColumns.class);
        report.persist(rr, instance.getPath() + "/jcr:content/report");
    }

    static ThreadLocal<String> scanResult = new ThreadLocal<>();

    private void scanFolders(ActionManager manager) {
        TreeFilteringResourceVisitor visitor = new TreeFilteringResourceVisitor();
        visitor.setBreadthFirstMode();
        visitor.setResourceVisitor((folder, level) -> {
            String path = folder.getPath();
            manager.deferredWithResolver(rr -> {
                if (scanMode.shouldReplace(rr.getResource(path))) {
                    String result = scanResult.get();
                    scanResult.remove();
                    record(path, "Flagged", result);
                    foldersToReplace.add(path);
                }
            });
        });
        manager.deferredWithResolver(rr -> {
            record(startingPath, "Start", "Starting folder scan");
            visitor.accept(rr.getResource(startingPath));
        });
    }

    private void removeOldThumbnails(ActionManager manager) {
        ActionBatch batch = new ActionBatch(manager, 20);
        foldersToReplace.forEach(path -> {
            batch.add(rr -> {
                Resource res = rr.getResource(path + FOLDER_THUMBNAIL);
                if (res != null) {
                    rr.delete(res);
                    record(path, "Deleted", "Existing thumbnail removed");
                }
            });
        });
        batch.commitBatch();
    }

    private void rebuildThumbnails(ActionManager manager) {
        foldersToReplace.forEach(path -> {
            manager.deferredWithResolver(rr -> rebuildThumbnail(rr, path));
        });
    }

    private void rebuildThumbnail(ResourceResolver rr, String folderPath) throws ServletException, IOException {
        HttpServletRequest req = requestFactory.createRequest("GET", folderPath + ".folderthumbnail.jpg", THUMBNAIL_PARAMS);
        try (NullOutputStream out = new NullOutputStream()) {
            HttpServletResponse res = requestFactory.createResponse(out);
            slingProcessor.processRequest(req, res, rr);
            res.flushBuffer();
        }
        record(folderPath, "Rebuild", "Thumbnail was rebuilt");
    }

    protected static boolean isThumbnailMissing(Resource damFolder) {
        if (isThumbnailManual(damFolder) || isThumbnailAutomatic(damFolder)) {
            return false;
        } else {
            scanResult.set("Thumbnail missing");
            return true;
        }
    }

    protected static boolean isPlaceholderThumbnail(Resource damFolder) throws IOException {
        Resource jcrContent = damFolder.getChild(JcrConstants.JCR_CONTENT);
        if (jcrContent == null) {
            return false;
        }
        Resource thumbnail = jcrContent.getChild(DamConstants.THUMBNAIL_NODE);
        if (thumbnail == null) {
            return false;
        } else {
            long size = getBinarySize(thumbnail);
            if (size <= PLACEHOLDER_SIZE) {
                scanResult.set("Placeholder detected, " + (size <= 0 ? "no thumbnail data" : "size is " + size + " bytes"));
                return true;
            } else {
                return false;
            }
        }
    }

    private static long getBinarySize(Resource res) throws IOException {
        InputStream thumbnailData = res.adaptTo(InputStream.class);
        if (thumbnailData == null) {
            return -1;
        }
        long size = 0;
        long count = 0;
        byte[] buf = new byte[1024];
        while ((count = thumbnailData.read(buf)) > 0) {
            size += count;
        }
        thumbnailData.close();
        return size;
    }

    protected static boolean isThumbnailContentsOutdated(Resource damFolder) {
        Resource contents = damFolder.getChild("jcr:content/folderThumbnail/jcr:content");
        if (isThumbnailManual(damFolder)) {
            return false;
        }
        if (contents == null) {
            scanResult.set("No folder metadata, assuming contents outdated");
            return true;
        } else {
            Date thumbnailModified = (Date) contents.getValueMap().getOrDefault("jcr:lastModified", Date.class);
            String[] paths = contents.getValueMap().get("dam:folderThumbnailPaths", String[].class);
            if (thumbnailModified == null || paths == null || paths.length == 0) {
                scanResult.set("No folder thumbnails being tracked, assuming contents outdated");
                return true;
            } else {
                for (String assetPath : paths) {
                    Resource assetResource = damFolder.getResourceResolver().getResource(assetPath);
                    if (isAssetMissingOrNewer(assetResource, thumbnailModified)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    protected static boolean isAssetMissingOrNewer(Resource asset, Date compareDate) {
        if (asset == null) {
            scanResult.set("Referenced asset missing");
            return true;
        } else {
            Resource content = asset.getChild("jcr:content");
            if (content == null) {
                scanResult.set("Referenced asset has no content node; " + asset.getPath());
                return true;
            }
            Date assetModified = content.getValueMap().get("jcr:lastModified", Date.class);
            if (assetModified == null) {
                scanResult.set("Referenced asset has no modified date; " + asset.getPath());
                return true;
            } else if (assetModified.after(compareDate)) {
                scanResult.set("Referenced newer than folder; " + asset.getPath());
                return true;
            }
        }
        return false;
    }

    protected static boolean isThumbnailManual(Resource damFolder) {
        return damFolder.getChild("jcr:content/manualThumbnail.jpg") != null
                || damFolder.getChild("jcr:content/manualThumbnail.png") != null;
    }

    protected static boolean isThumbnailAutomatic(Resource damFolder) {
        if (isThumbnailManual(damFolder)) {
            return false;
        } else if (damFolder.getChild("jcr:content/folderThumbnail") != null) {
            scanResult.set("Detected automatic thumbnail and no manual thumbnail");
            return true;
        }
        return false;
    }
}
