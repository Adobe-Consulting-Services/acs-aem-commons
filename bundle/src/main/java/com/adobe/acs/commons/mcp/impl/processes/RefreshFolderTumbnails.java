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
import com.adobe.acs.commons.functions.CheckedFunction;
import com.adobe.acs.commons.mcp.ProcessDefinition;
import com.adobe.acs.commons.mcp.ProcessInstance;
import com.adobe.acs.commons.util.visitors.TreeFilteringResourceVisitor;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.DamConstants;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.jcr.RepositoryException;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Replace folder thumbnails under a user-definable set of circumstances
 * As a business user, I would like an easy way to scan and repair missing thumbnails, or just regenerate all thumbnails under a given tree of the DAM.
 */

public class RefreshFolderTumbnails extends ProcessDefinition {
    private static enum ThumbnailScanLogic {
        MISSING(RefreshFolderTumbnails::isThumbnailMissing),
        PLACEHOLDERS(RefreshFolderTumbnails::isThumbnailMissing, 
                RefreshFolderTumbnails::isPlaceholderThumbnail),
        OUTDATED(RefreshFolderTumbnails::isThumbnailMissing, 
                RefreshFolderTumbnails::isPlaceholderThumbnail,
                RefreshFolderTumbnails::isThumbnailContentsOutdated
        ),
        ALL(r->true);
        
        CheckedFunction<Resource, Boolean> test;
        ThumbnailScanLogic(CheckedFunction<Resource, Boolean>... tests) {
            this.test = CheckedFunction.or(tests);
        }
        
        public boolean shouldReplace(Resource r) throws Exception {
            return this.test.apply(r);
        }
    }
    
    private static final int PLACEHOLDER_SIZE = 1024;
    private String startingPath;
    private ThumbnailScanLogic scanLogic = ThumbnailScanLogic.PLACEHOLDERS;
    
    private transient List<String> foldersToReplace = Collections.synchronizedList(new ArrayList<>());
    
    @Override
    public void init() throws RepositoryException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
                if (scanLogic.shouldReplace(rrr.getResource(path))) {
                    foldersToReplace.add(path);
                }                
            });
        });
    }
    
    private void removeOldThumbnails(ActionManager manager) {
        // Remove folder/jcr:content/folderThumbnail
        
    }
    
    private void rebuildThumbnails(ActionManager manager) {
        //Haven't figured out a clean way to regenerate the thumbnail but the .folderthumbnail.jpg selector/suffix does it from the browser.
        // If the folder thumbnail generation is in a "public" API then it would make sense to call it directly, otherwise an internal Sling Request will have to suffice to trigger that servlet.

    }
    
    private static boolean isThumbnailMissing(Resource damFolder) {
        //Thumbnail is under folder/jcr:content/folderThumbnail
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
        return thumbnailData == null || thumbnailData.length  <= PLACEHOLDER_SIZE;
    }
    
    private static boolean isThumbnailContentsOutdated(Resource damFolder) {
        //Look at /jcr:content/folderThumbnail/jcr:content/@dam:folderThumbnailPaths that list 3 images in that folder.
        //jcr:lastModified property in the jcr:content might be a useful determining factor if a thumbnail is older than the content of the folder
        //Confirm if all assets exist and if any modified dates are later than thumbnail
        
        return false;
    }
}
