/*
 * Copyright 2018 Adobe.
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
import com.adobe.acs.commons.fam.actions.ActionBatch;
import com.adobe.acs.commons.functions.CheckedFunction;
import com.adobe.acs.commons.mcp.ProcessDefinition;
import com.adobe.acs.commons.mcp.ProcessInstance;
import com.adobe.acs.commons.mcp.form.FormField;
import com.adobe.acs.commons.util.visitors.TreeFilteringResourceVisitor;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.contentsync.handler.util.RequestResponseFactory;
import com.day.cq.dam.api.DamConstants;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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

    private static enum ThumbnailScanLogic {
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

        ThumbnailScanLogic(CheckedFunction<Resource, Boolean>... tests) {
            this.test = CheckedFunction.or(tests);
        }

        public boolean shouldReplace(Resource r) throws Exception {
            return this.test.apply(r);
        }
    }

    public static final String FOLDER_THUMBNAIL = "jcr:content/folderThumbnail";

    private static final Map<String, Object> THUMBNAIL_PARAMS = new HashMap<String, Object>(){{
        put("width", "200");
        put("height", "120");
    }};
    private static final int PLACEHOLDER_SIZE = 1024;
    

    private RequestResponseFactory requestFactory;
    private SlingRequestProcessor slingProcessor;
    
    @FormField(name="Starting Path")
    private String startingPath = "/content/dam";
    
    @FormField(name="Mode")
    private ThumbnailScanLogic scanMode = ThumbnailScanLogic.PLACEHOLDERS;

    private transient List<String> foldersToReplace = Collections.synchronizedList(new ArrayList<>());

    public RefreshFolderTumbnails(RequestResponseFactory reqRspFactory, SlingRequestProcessor slingProcessor) {
        this.requestFactory = reqRspFactory;
        this.slingProcessor = this.slingProcessor;
    }

    @Override
    public void init() throws RepositoryException {
    }

    @Override
    public void buildProcess(ProcessInstance instance, ResourceResolver rr) throws LoginException, RepositoryException {
        instance.defineCriticalAction("Scan folders", rr, this::scanFolders);
        instance.defineAction("Remove old thumbnails", rr, this::removeOldThumbnails);
        instance.defineAction("Rebuild thumbnails", rr, this::rebuildThumbnails);
    }

    @Override
    public void storeReport(ProcessInstance instance, ResourceResolver rr) throws RepositoryException, PersistenceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void scanFolders(ActionManager manager) {
        TreeFilteringResourceVisitor visitor = new TreeFilteringResourceVisitor();
        visitor.setBreadthFirstMode();
        visitor.setResourceVisitor((folder, level) -> {
            String path = folder.getPath();
            manager.deferredWithResolver(rrr -> {
                if (scanMode.shouldReplace(rrr.getResource(path))) {
                    foldersToReplace.add(path);
                }
            });
        });
    }

    private void removeOldThumbnails(ActionManager manager) {
        ActionBatch batch = new ActionBatch(manager, 20);
        foldersToReplace.forEach(path -> { 
            batch.add(rr->rr.delete(rr.getResource(path + FOLDER_THUMBNAIL)));
        });
        batch.commitBatch();
    }

    private void rebuildThumbnails(ActionManager manager) {
        foldersToReplace.forEach(path -> { 
            manager.deferredWithResolver(rr -> rebuildThumbnail(rr, path));
        });
    }
    
    private void rebuildThumbnail(ResourceResolver rr, String folderPath) throws ServletException, IOException { 
        HttpServletRequest req = requestFactory.createRequest("GET", folderPath + FOLDER_THUMBNAIL + ".png", THUMBNAIL_PARAMS);
        HttpServletResponse res = requestFactory.createResponse(new NullOutputStream());
        slingProcessor.processRequest(req, res, rr);
    }

    private static boolean isThumbnailMissing(Resource damFolder) {
        Resource jcrContent = damFolder.getChild(JcrConstants.JCR_CONTENT);
        if (jcrContent == null) {
            return true;
        }
        Resource thumbnail = jcrContent.getChild(DamConstants.THUMBNAIL_NODE);
        return thumbnail == null;
    }

    private static boolean isPlaceholderThumbnail(Resource damFolder) {
        Resource jcrContent = damFolder.getChild(JcrConstants.JCR_CONTENT);
        Resource thumbnail = jcrContent.getChild(DamConstants.THUMBNAIL_NODE);
        byte[] thumbnailData = thumbnail.getValueMap().get(JcrConstants.JCR_DATA, byte[].class);
        return thumbnailData == null || thumbnailData.length <= PLACEHOLDER_SIZE;
    }

    private static boolean isThumbnailContentsOutdated(Resource damFolder) {
        //Look at /jcr:content/folderThumbnail/jcr:content/@dam:folderThumbnailPaths that list 3 images in that folder.
        //jcr:lastModified property in the jcr:content might be a useful determining factor if a thumbnail is older than the content of the folder
        //Confirm if all assets exist and if any modified dates are later than thumbnail
//        PreviewGenerator.validateExistingPreview
        return false;
    }
    
    private static boolean isThumbnailAutomatic(Resource damFolder) {
        return false;
    }
}
