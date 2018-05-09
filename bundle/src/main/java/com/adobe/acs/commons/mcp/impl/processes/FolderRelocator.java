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

import com.adobe.acs.commons.fam.Failure;
import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.fam.actions.ActionBatch;
import com.adobe.acs.commons.fam.actions.Actions;
import com.adobe.acs.commons.mcp.ProcessDefinition;
import com.adobe.acs.commons.mcp.ProcessInstance;
import com.adobe.acs.commons.mcp.form.PathfieldComponent;
import com.adobe.acs.commons.mcp.form.RadioComponent;
import com.adobe.acs.commons.util.visitors.SimpleFilteringResourceVisitor;
import com.adobe.acs.commons.util.visitors.TreeFilteringResourceVisitor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import com.adobe.acs.commons.mcp.form.FormField;
import java.io.Serializable;
import org.apache.commons.lang3.StringUtils;

/**
 * This utility takes an alternate approach to moving folders using a four-step
 * process. This can be used to move one or more folders as needed.
 * <ul>
 * <li>Step 1: Evaluate the requirements, check for possible authorization
 * issues; Abort sequence halts other work</li>
 * <li>Step 2: Prepare destination folder structure; Abort sequence is to remove
 * any folders created already</li>
 * <li>Step 3: Relocate the contents of the folders</li>
 * <li>Step 4: Remove the old folder structures</li>
 * </ul>
 *
 * There are different combinations of how this can be used:
 * <ul>
 * <li>Rename a folder, keeping it where it is presently located. This uses the
 * Rename mode where the source is the folder path and the destination is the
 * complete path of the folder as it should be after renaming</li>
 * <li>Move a folder, keeping its name intact. This uses the Move mode where the
 * source is the folder path and the destination is the parent node where it
 * should go. You can also use the RENAME mode for this, provided that the
 * destination path also specifies the node name. They're technically the same
 * thing, but MOVE is provided for convenience.</li>
 * <li>Move multiple folders, keeping all names intact. This uses an alternate
 * constructor which takes a list of paths as sources and a single destination
 * for the parent where all folders will be moved to. This is functionally the
 * same as moving all folders in a loop, except that the operation is batched
 * together as one big process instead of having to define each folder move as a
 * separate process.</li>
 * </ul>
 */
public class FolderRelocator extends ProcessDefinition implements Serializable {
    private static final long serialVersionUID = 7526472295622776160L;

    public enum Mode {
        RENAME, MOVE
    }

    private Map<String, String> sourceToDestination;
    @FormField(name="Source folder(s)", 
            description="One or more source folders must be provided.  Multiple folders implies a move operation.",
            hint="/content/dam/someFolder",
            component=PathfieldComponent.FolderSelectComponent.class,
            options={"base=/content/dam","multiple"})
    private String[] sourcePaths;
    @FormField(name="Destination folder", 
            description="Destination parent for move, or destination parent folder plus new name for rename",
            hint="Move: /content/dam/moveToFolder | Rename: /content/dam/moveToFolder/newName",
            component=PathfieldComponent.FolderSelectComponent.class,
            options={"base=/content/dam"})
    private String destinationPath;
    @FormField(name="Mode", 
            description="Move relocates one or more folders.  Rename relocates and takes the last part of the path as the new name for one folder.",
            required=false,
            component=RadioComponent.EnumerationSelector.class,
            options={"horizontal","default=MOVE"})
    private Mode mode;
    
    private final transient String[] requiredFolderPrivilegeNames = {
        Privilege.JCR_READ,
        Privilege.JCR_WRITE,
        Privilege.JCR_REMOVE_CHILD_NODES,
        Privilege.JCR_REMOVE_NODE
    };

    private final transient String[] requiredNodePrivilegeNames = {
        Privilege.JCR_ALL
    };

    private transient Privilege[] requiredFolderPrivileges;
    private transient Privilege[] requiredNodePrivileges;

    private int batchSize = 5;

    public FolderRelocator() {
        // Invoked when starting via the MCP servlet
    }
    
    /**
     * Prepare a folder relocation for multiple folders to be moved under the
     * same target parent node. Because there are multiple folders being moved
     * under one parent, this assumes the operation is a Move not a Rename.
     *
     * @param sourcePaths List of source paths to move
     * @param destinationPath Destination parent path
     */
    public FolderRelocator(String[] sourcePaths, String destinationPath) {
        init(sourcePaths, destinationPath);
    }

    /**
     * Prepare a folder relocation for a single folder.
     *
     * @param sourcePath Source node to be moved
     * @param destinationPath Destination path, which is either the parent (if
     * mode is MOVE) or the desired final path for the node (if mode is RENAME)
     * @param processMode MOVE if node name stays the same and needs to be under
     * a new parent; RENAME if the node needs to change its name and destination
     * contains that new name.
     */
    public FolderRelocator(String sourcePath, String destinationPath, Mode processMode) {
        init(sourcePath, destinationPath, processMode);
    }
    
    @Override
    public void init() throws RepositoryException {
        if (sourcePaths != null && sourcePaths.length == 1) {
            init(sourcePaths[0], destinationPath, mode);
        } else {
            init(sourcePaths, destinationPath);
        }
    }

    private void init(String[] sourcePaths, String destinationPath) {
        sourceToDestination = new HashMap<>();
        this.mode = Mode.MOVE;

        for (String sourcePath : sourcePaths) {
            String nodeName = sourcePath.substring(sourcePath.lastIndexOf('/'));
            String destination = destinationPath + nodeName;
            sourceToDestination.put(sourcePath, destination);
        }        
    }
    
    private void init(String sourcePath, String destinationPath, Mode processMode) {
        sourceToDestination = new HashMap<>();
        this.mode = processMode;

        String destination = destinationPath;
        if (mode == Mode.MOVE) {
            String nodeName = sourcePath.substring(sourcePath.lastIndexOf('/'));
            destination += nodeName;
        }
        sourceToDestination.put(sourcePath, destination);        
    }
        
    /**
     * Batch size determines the number of operations (folder creation or node
     * moves) performed at a time.
     *
     * @param batchSize the batchSize to set
     */
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    @Override
    public void buildProcess(ProcessInstance instance, ResourceResolver rr) throws LoginException, RepositoryException {
        validateInputs(rr);
        Session ses = rr.adaptTo(Session.class);
        requiredFolderPrivileges = getPrivilegesFromNames(ses, requiredFolderPrivilegeNames);
        requiredNodePrivileges = getPrivilegesFromNames(ses, requiredNodePrivilegeNames);
        instance.defineCriticalAction("Validate ACLs", rr, this::validateAllAcls);
        instance.defineCriticalAction("Build target folders", rr, this::buildTargetFolders)
                .onFailure(this::abortStep2);
        instance.defineCriticalAction("Move nodes", rr, this::moveNodes);
        instance.defineCriticalAction("Remove old folders", rr, this::removeSourceFolders);
        if (sourcePaths.length > 1) {
            instance.getInfo().setDescription("Move "+sourcePaths.length+" folders to "+destinationPath);
        } else {
            String verb = StringUtils.capitalize(mode.name().toLowerCase());
            instance.getInfo().setDescription(verb + " " + sourcePaths[0] + " to "+destinationPath);            
        }
    }

    @SuppressWarnings("squid:S3776")
    private void validateInputs(ResourceResolver res) throws RepositoryException {
        Optional<RepositoryException> error
                = sourceToDestination.entrySet().stream().map((pair) -> {
                    String entrySourcePath = pair.getKey();
                    String entryDestinationPath = pair.getValue();
                    if (entrySourcePath == null) {
                        return new RepositoryException("Source path should not be null");
                    }
                    if (entryDestinationPath == null) {
                        return new RepositoryException("Destination path should not be null");
                    }
                    if (entryDestinationPath.contains(entrySourcePath + "/")) {
                        return new RepositoryException("Destination must be outside of source folder");
                    }
                    if (!resourceExists(res, entrySourcePath)) {
                        if (!entrySourcePath.startsWith("/")) {
                            return new RepositoryException("Paths are not valid unless they start with a forward slash, you provided: " + entrySourcePath);
                        } else {
                            return new RepositoryException("Unable to find source " + entrySourcePath);
                        }
                    }
                    if (!resourceExists(res, entryDestinationPath.substring(0, entryDestinationPath.lastIndexOf('/')))) {
                        if (!entryDestinationPath.startsWith("/")) {
                            return new RepositoryException("Paths are not valid unless they start with a forward slash, you provided: " + entryDestinationPath);
                        } else {
                            return new RepositoryException("Unable to find destination " + entryDestinationPath);
                        }
                    }
                    return null;
                }).filter(Objects::nonNull).findFirst();
        if (error.isPresent()) {
            Logger.getLogger(FolderRelocator.class.getName()).log(Level.SEVERE, "Validation error prior to starting move operations: {0}", error.get().getMessage());
            throw error.get();
        }
    }

    private boolean resourceExists(ResourceResolver rr, String path) {
        Resource res = rr.getResource(path);
        return res != null && !Resource.RESOURCE_TYPE_NON_EXISTING.equals(res.getResourceType());
    }

    private void validateAllAcls(ActionManager step1) {
        TreeFilteringResourceVisitor folderVisitor = new TreeFilteringResourceVisitor();
        folderVisitor.setBreadthFirstMode();
        folderVisitor.setResourceVisitor((resource, level) -> step1.deferredWithResolver(rr -> checkNodeAcls(rr, resource.getPath(), requiredFolderPrivileges)));
        folderVisitor.setLeafVisitor((resource, level) -> step1.deferredWithResolver(rr -> checkNodeAcls(rr, resource.getPath(), requiredNodePrivileges)));
        sourceToDestination.keySet().forEach(sourcePath -> beginStep(step1, sourcePath, folderVisitor));
    }

    private Privilege[] getPrivilegesFromNames(Session session, String[] names) throws RepositoryException {
        AccessControlManager acm = session.getAccessControlManager();
        Privilege[] prvlgs = new Privilege[names.length];
        for (int i = 0; i < names.length; i++) {
            prvlgs[i] = acm.privilegeFromName(names[i]);
        }
        return prvlgs;
    }

    private void checkNodeAcls(ResourceResolver res, String path, Privilege[] prvlgs) throws RepositoryException {
        Actions.setCurrentItem(path);
        Session session = res.adaptTo(Session.class);
        if (!session.getAccessControlManager().hasPrivileges(path, prvlgs)) {
            throw new RepositoryException("Insufficient permissions to permit move operation");
        }
    }

    private void buildTargetFolders(ActionManager step2) {
        TreeFilteringResourceVisitor folderVisitor = new TreeFilteringResourceVisitor();
        folderVisitor.setBreadthFirstMode();
        folderVisitor.setResourceVisitor((res, level) -> {
            String path = res.getPath();
            step2.deferredWithResolver(Actions.retry(5, 100, rr -> buildDestinationFolder(rr, path)));
        });
        sourceToDestination.keySet().forEach(sourcePath -> beginStep(step2, sourcePath, folderVisitor));
    }

    private void abortStep2(List<Failure> errors, ResourceResolver rr) {
        Logger.getLogger(FolderRelocator.class.getName())
                .log(Level.SEVERE, "{0} issues enountered trying to create destination folder structure; aborting process.", errors.size());
        sourceToDestination.keySet().forEach(sourcePath -> {
            try {
                rr.delete(rr.getResource(convertSourceToDestination(sourcePath)));
            } catch (PersistenceException | RepositoryException ex) {
                rr.refresh();
                Logger.getLogger(FolderRelocator.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
    }

    private void buildDestinationFolder(ResourceResolver rr, String sourceFolder) throws PersistenceException, RepositoryException, InterruptedException {
        Session session = rr.adaptTo(Session.class);
        session.getWorkspace().getObservationManager().setUserData("changedByWorkflowProcess");
        Resource source = rr.getResource(sourceFolder);
        String targetPath = convertSourceToDestination(sourceFolder);
        if (!resourceExists(rr, targetPath)) {
            Actions.setCurrentItem(sourceFolder + "->" + targetPath);
            String targetParentPath = targetPath.substring(0, targetPath.lastIndexOf('/'));
            String targetName = targetPath.substring(targetPath.lastIndexOf('/') + 1);
            waitUntilResourceFound(rr, targetParentPath);
            Resource destParent = rr.getResource(targetParentPath);
            Logger.getLogger(FolderRelocator.class.getName()).log(Level.INFO, "Creating target for {0}", sourceFolder);
            rr.create(destParent, targetName, source.getValueMap());
            rr.commit();
            rr.refresh();
        }
        String sourceJcrContent = sourceFolder + "/jcr:content";
        if (resourceExists(rr, sourceJcrContent)) {
            Actions.getCurrentActionManager().deferredWithResolver(Actions.retry(5,50,(rrr)->{
                if (!resourceExists(rrr, targetPath + "/jcr:content")) {
                    waitUntilResourceFound(rrr, targetPath);
                    rrr.copy(sourceJcrContent, targetPath);
                    rrr.commit();
                    rrr.refresh();
                }
            }));
        }
    }

    private void waitUntilResourceFound(ResourceResolver rr, String path) throws InterruptedException, RepositoryException {
        for (int i=0; i < 10; i++) {
            if (resourceExists(rr, path)) {
                return;
            }
            Thread.sleep(100);
            rr.refresh();
        }
        throw new RepositoryException("Resource not found: "+path);
    }

    private String convertSourceToDestination(String path) throws RepositoryException {
        return sourceToDestination.entrySet().stream()
                .filter(entry -> path.startsWith(entry.getKey())).findFirst()
                .map(entry -> path.replaceAll(Pattern.quote(entry.getKey()), entry.getValue()))
                .orElseThrow(() -> new RepositoryException("Cannot determine destination for " + path));
    }

    private void moveNodes(ActionManager step3) {
        ActionBatch batch = new ActionBatch(step3, batchSize);
        batch.setRetryCount(10);
        TreeFilteringResourceVisitor folderVisitor = new TreeFilteringResourceVisitor();
        folderVisitor.setBreadthFirstMode();
        folderVisitor.setLeafVisitor((res, level) -> {
            String path = res.getPath();
            if (!path.endsWith("jcr:content")) {
                batch.add(rr -> moveItem(rr, path));
            }
        });
        sourceToDestination.keySet().forEach(sourcePath -> beginStep(step3, sourcePath, folderVisitor));
        batch.commitBatch();
    }

    private void moveItem(ResourceResolver rr, String path) throws RepositoryException {
        Logger.getLogger(FolderRelocator.class.getName()).log(Level.INFO, "Moving {0}", path);
        Actions.setCurrentItem(path);
        Session session = rr.adaptTo(Session.class);
        // Inhibits some workflows
        session.getWorkspace().getObservationManager().setUserData("changedByWorkflowProcess");
        session.move(path, convertSourceToDestination(path));
    }

    private void removeSourceFolders(ActionManager step4) {
        sourceToDestination.keySet().forEach(sourcePath
                -> step4.deferredWithResolver(Actions.retry(5, 100, rr -> deleteResource(rr, sourcePath)))
        );
    }

    private void deleteResource(ResourceResolver rr, String path) throws PersistenceException {
        Actions.setCurrentItem(path);
        rr.delete(rr.getResource(path));
    }
    
    private void beginStep(ActionManager step, String startingNode, SimpleFilteringResourceVisitor visitor) {
        try {
            step.withResolver(rr -> 
                visitor.accept(rr.getResource(startingNode))
            );
        } catch (Exception ex) {
            Logger.getLogger(FolderRelocator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    public void storeReport(ProcessInstance instance, ResourceResolver rr) {
        // no-op
    }
}
