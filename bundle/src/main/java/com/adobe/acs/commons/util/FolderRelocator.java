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
package com.adobe.acs.commons.util;

import com.adobe.acs.commons.fam.Failure;
import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.fam.ActionManagerFactory;
import com.adobe.acs.commons.fam.ControlledProcess;
import com.adobe.acs.commons.fam.actions.Actions;
import com.adobe.acs.commons.functions.CheckedConsumer;
import com.adobe.acs.commons.util.visitors.TreeFilteringItemVisitor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

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
public class FolderRelocator extends ControlledProcess {

    public static enum Mode {
        RENAME, MOVE
    };
    private final Map<String, String> sourceToDestination;
    private final Mode mode;
    private final String[] requiredFolderPrivilegeNames = {
        Privilege.JCR_READ,
        Privilege.JCR_WRITE,
        Privilege.JCR_REMOVE_CHILD_NODES,
        Privilege.JCR_REMOVE_NODE
    };

    private final String[] requiredNodePrivilegeNames = {
        Privilege.JCR_ALL
    };

    private Privilege[] requiredFolderPrivileges;
    private Privilege[] requiredNodePrivileges;

    private int batchSize = 5;

    /**
     * Prepare a folder relocation for multiple folders to be moved under the
     * same target parent node. Because there are multiple folders being moved
     * under one parent, this assumes the operation is a Move not a Rename.
     *
     * @param amf Action manager factory service
     * @param sourcePaths List of source paths to move
     * @param destinationPath Destination parent path
     * @param processName Process name for tracking
     */
    public FolderRelocator(ActionManagerFactory amf, String[] sourcePaths, String destinationPath, String processName) {
        super(amf, processName);
        sourceToDestination = new HashMap<>();
        this.mode = Mode.MOVE;

        for (String sourcePath : sourcePaths) {
            String nodeName = sourcePath.substring(sourcePath.lastIndexOf('/'));
            String destination = destinationPath + nodeName;
            sourceToDestination.put(sourcePath, destination);
        }
    }

    /**
     * Prepare a folder relocation for a single folder.
     *
     * @param amf Action manager factory service
     * @param sourcePath Source node to be moved
     * @param destinationPath Destination path, which is either the parent (if
     * mode is MOVE) or the desired final path for the node (if mode is RENAME)
     * @param processName Process name for tracking
     * @param processMode MOVE if node name stays the same and needs to be under
     * a new parent; RENAME if the node needs to change its name and destination
     * contains that new name.
     */
    public FolderRelocator(ActionManagerFactory amf, String sourcePath, String destinationPath, String processName, Mode processMode) {
        super(amf, processName);
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
    public void buildProcess(ResourceResolver rr) throws LoginException, RepositoryException {
        validateInputs(rr);
        Session ses = rr.adaptTo(Session.class);
        requiredFolderPrivileges = getPrivilegesFromNames(ses, requiredFolderPrivilegeNames);
        requiredNodePrivileges = getPrivilegesFromNames(ses, requiredNodePrivilegeNames);
        defineCriticalAction("Validate ACLs", rr, this::validateAllAcls);
        defineCriticalAction("Build target folders", rr, this::buildTargetFolders)
                .onFailure(this::abortStep2);
        defineCriticalAction("Move nodes", rr, this::moveNodes);
        defineCriticalAction("Remove old folders", rr, this::removeSourceFolders);
    }

    private void validateInputs(ResourceResolver res) throws RepositoryException {
        Optional<RepositoryException> error
                = sourceToDestination.entrySet().stream().map((pair) -> {
                    String sourcePath = pair.getKey();
                    String destinationPath = pair.getValue();
                    if (sourcePath == null) {
                        return new RepositoryException("Source path should not be null");
                    }
                    if (destinationPath == null) {
                        return new RepositoryException("Destination path should not be null");
                    }
                    if (destinationPath.contains(sourcePath + "/")) {
                        return new RepositoryException("Destination must be outside of source folder");
                    }
                    if (!resourceExists(res, sourcePath)) {
                        return new RepositoryException("Unable to find source " + sourcePath);
                    }
                    if (!resourceExists(res, destinationPath.substring(0, destinationPath.lastIndexOf('/')))) {
                        return new RepositoryException("Unable to find destination " + destinationPath);
                    }
                    return null;
                }).filter(Objects::nonNull).findFirst();
        if (error.isPresent()) {
            Logger.getLogger(FolderRelocator.class.getName()).log(Level.SEVERE, "Validation error prior to starting move operations: {0}", error.get().getMessage());
            throw error.get();
        }
    }

    private boolean resourceExists(ResourceResolver rr, String path) {
        Resource res = rr.resolve(path);
        return !Resource.RESOURCE_TYPE_NON_EXISTING.equals(res.getResourceType());
    }

    LinkedBlockingQueue<CheckedConsumer<ResourceResolver>> currentBatch = new LinkedBlockingQueue<>();

    private void addToBatch(ActionManager actionManager, int retries, long retryDelay, CheckedConsumer<ResourceResolver> action) throws InterruptedException {
        currentBatch.put(action);
        if (currentBatch.size() >= batchSize) {
            commitBatch(actionManager, retries, retryDelay);
        }
    }

    private void commitBatch(ActionManager actionManager, int retries, long retryDelay) {
        final List<CheckedConsumer<ResourceResolver>> consumers = new ArrayList<>();
        int count = currentBatch.drainTo(consumers);
        if (count > 0) {
            actionManager.deferredWithResolver(
                    Actions.retry(retries, retryDelay, (ResourceResolver rr) -> {
                        Logger.getLogger(FolderRelocator.class.getName()).log(Level.INFO, "Executing {0} actions", count);
                        for (CheckedConsumer<ResourceResolver> consumer : consumers) {
                            consumer.accept(rr);
                        }
                        Logger.getLogger(FolderRelocator.class.getName()).log(Level.INFO, "Commiting {0} actions", count);
                        rr.commit();
                        Logger.getLogger(FolderRelocator.class.getName()).log(Level.INFO, "Commit successful");
                    })
            );
        }
    }

    private void validateAllAcls(ActionManager step1) {
        TreeFilteringItemVisitor folderVisitor = new TreeFilteringItemVisitor();
        folderVisitor.setBreadthFirst(true);
        folderVisitor.onEnterNode((node, level) -> step1.deferredWithResolver(rr -> checkNodeAcls(rr, node.getPath(), requiredFolderPrivileges)));
        folderVisitor.onVisitChild((node, level) -> step1.deferredWithResolver(rr -> checkNodeAcls(rr, node.getPath(), requiredNodePrivileges)));
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
        Session session = res.adaptTo(Session.class);
        if (!session.getAccessControlManager().hasPrivileges(path, prvlgs)) {
            throw new RepositoryException("Insufficient permissions to permit move operation");
        }
    }

    private void buildTargetFolders(ActionManager step2) {
        TreeFilteringItemVisitor folderVisitor = new TreeFilteringItemVisitor();
        folderVisitor.setBreadthFirst(true);
        folderVisitor.onEnterNode((node, level) -> {
            String path = node.getPath();
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

    private void buildDestinationFolder(ResourceResolver rr, String sourceFolder) throws PersistenceException, RepositoryException {
        Session session = rr.adaptTo(Session.class);
        session.getWorkspace().getObservationManager().setUserData("changedByWorkflowProcess");
        Resource source = rr.getResource(sourceFolder);
        String targetPath = convertSourceToDestination(sourceFolder);
        if (!resourceExists(rr, targetPath)) {
            Actions.setCurrentItem(sourceFolder + "->" + targetPath);
            String targetParentPath = targetPath.substring(0, targetPath.lastIndexOf('/'));
            String targetName = targetPath.substring(targetPath.lastIndexOf('/') + 1);
            Resource destParent = rr.getResource(targetParentPath);
            if (destParent.isResourceType(Resource.RESOURCE_TYPE_NON_EXISTING)) {
                throw new RepositoryException("Unable to find target folder " + targetParentPath);
            }
            Logger.getLogger(FolderRelocator.class.getName()).log(Level.INFO, "Creating target for {0}", sourceFolder);
            rr.create(destParent, targetName, source.getValueMap());
        }
        if (resourceExists(rr, sourceFolder + "/jcr:content") && !resourceExists(rr, targetPath + "/jcr:content")) {
            rr.commit();
            rr.refresh();
            Logger.getLogger(FolderRelocator.class.getName()).log(Level.INFO, "Copying {0}/jcr:content", sourceFolder);
            rr.copy(sourceFolder + "/jcr:content", targetPath + "/jcr:content");
        }
    }

    private String convertSourceToDestination(String path) throws RepositoryException {
        return sourceToDestination.entrySet().stream()
                .filter(entry -> path.startsWith(entry.getKey())).findFirst()
                .map(entry -> path.replaceAll(Pattern.quote(entry.getKey()), entry.getValue()))
                .orElseThrow(() -> new RepositoryException("Cannot determine destination for " + path));
    }

    private void moveNodes(ActionManager step3) {
        TreeFilteringItemVisitor folderVisitor = new TreeFilteringItemVisitor();
        folderVisitor.setBreadthFirst(true);
        folderVisitor.onVisitChild((node, level) -> {
            String path = node.getPath();
            if (!path.endsWith("jcr:content")) {
                addToBatch(step3, 15, 250, rr -> moveItem(rr, path));
            }
        });
        sourceToDestination.keySet().forEach(sourcePath -> beginStep(step3, sourcePath, folderVisitor));
        commitBatch(step3, 15, 250);
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
                -> step4.deferredWithResolver(rr -> rr.delete(rr.getResource(sourcePath)))
        );
    }

    private void beginStep(ActionManager step, String startingNode, ItemVisitor visitor) {
        try {
            step.withResolver(rr -> {
                rr.getResource(startingNode).adaptTo(Node.class).accept(visitor);
            });
        } catch (Exception ex) {
            Logger.getLogger(FolderRelocator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
