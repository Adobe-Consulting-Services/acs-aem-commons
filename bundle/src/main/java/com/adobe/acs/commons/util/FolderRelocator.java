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
import com.adobe.acs.commons.fam.DeferredActions;
import com.adobe.acs.commons.util.impl.TreeFilteringItemVisitor;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
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
 * process.
 * <ul>
 * <li>Step 1: Evaluate the requirements, check for possible authorization
 * issues; Abort sequence halts other work</li>
 * <li>Step 2: Prepare destination folder structure; Abort sequence is to remove
 * any folders created already</li>
 * <li>Step 3: Relocate the contents of the folders</li>
 * <li>Step 4: Remove the old folder structures</li>
 * </ul>
 */
public class FolderRelocator {

    public static enum Mode {
        RENAME, MOVE
    };
    private final String sourcePath;
    private final String destinationPath;
    private final String processName;
    private ActionManager step1;
    private ActionManager step2;
    private ActionManager step3;
    private ActionManager step4;
    private final TreeFilteringItemVisitor folderVisitor;
    private final Mode mode;

    public FolderRelocator(String sourcePath, String destinationPath, String processName, Mode processMode) {
        this.sourcePath = sourcePath;
        this.processName = processName;
        this.mode = processMode;

        folderVisitor = new TreeFilteringItemVisitor();
        folderVisitor.setBreadthFirst(true);

        if (mode == Mode.MOVE) {
            String nodeName = sourcePath.substring(sourcePath.indexOf('/'));
            this.destinationPath = destinationPath + nodeName;
        } else {
            this.destinationPath = destinationPath;
        }
    }

    String[] requiredFolderPrivilegeNames = {
        Privilege.JCR_READ,
        Privilege.JCR_WRITE,
        Privilege.JCR_REMOVE_CHILD_NODES,
        Privilege.JCR_REMOVE_NODE
    };

    String[] requiredNodePrivilegeNames = {
        Privilege.JCR_ALL
    };

    Privilege[] requiredFolderPrivileges;
    Privilege[] requiredNodePrivileges;
    ActionManagerFactory managerFactory;

    public void startWork(ActionManagerFactory amf, ResourceResolver res) throws LoginException, RepositoryException {
        managerFactory = amf;
        validateInputs(res);

        step1 = amf.createTaskManager(processName + "- Step 1", res, 1);
        step2 = amf.createTaskManager(processName + "- Step 2", res, 1);
        step3 = amf.createTaskManager(processName + "- Step 3", res, 1);
        step4 = amf.createTaskManager(processName + "- Step 4", res, 1);

        requiredFolderPrivileges = getPrivilegesFromNames(res.adaptTo(Session.class), requiredFolderPrivilegeNames);
        requiredNodePrivileges = getPrivilegesFromNames(res.adaptTo(Session.class), requiredNodePrivilegeNames);

        step1.onFailure(this::abortStep1);
        step1.onSuccess(this::startStep2);

        step2.onFailure(this::abortStep2);
        step2.onSuccess(this::startStep3);

        step3.onFailure(this::recordError);
        step3.onSuccess(this::startStep4);

        step4.onFailure(this::recordError);
        step4.onFinish(this::success);

        startStep1();
    }

    private void validateInputs(ResourceResolver res) throws RepositoryException {
        if (sourcePath == null) {
            throw new RepositoryException("Source path should not be null");
        }
        if (destinationPath == null) {
            throw new RepositoryException("Destination path should not be null");
        }
        if (destinationPath.contains(sourcePath + "/")) {
            throw new RepositoryException("Destination must be outside of source folder");
        }
        if (!resourceExists(res, sourcePath)) {
            throw new RepositoryException("Unable to find source " + sourcePath);
        }
        if (!resourceExists(res, destinationPath.substring(0, destinationPath.lastIndexOf('/')))) {
            throw new RepositoryException("Unable to find destination " + destinationPath);
        }
    }

    private boolean resourceExists(ResourceResolver rr, String path) {
        Resource res = rr.resolve(path);
        return !Resource.RESOURCE_TYPE_NON_EXISTING.equals(res.getResourceType());
    }

    private void startStep1() {
        folderVisitor.onEnterNode((node, level) -> step1.deferredWithResolver(rr -> checkNodeAcls(rr, node.getPath(), requiredFolderPrivileges)));
        folderVisitor.onVisitChild((node, level) -> step1.deferredWithResolver(rr -> checkNodeAcls(rr, node.getPath(), requiredNodePrivileges)));
        beginStep(step1, sourcePath);
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

    private void abortStep1(List<Failure> errors, ResourceResolver res) {
        recordError(errors, res);
    }

    private void startStep2(ResourceResolver res) {
        folderVisitor.onEnterNode((node, level) -> step2.deferredWithResolver(
                DeferredActions.retry(5, 100, rr -> buildDestinationFolder(rr, node.getPath()))
        ));
        folderVisitor.onVisitChild(null);
        beginStep(step2, sourcePath);
    }

    private void abortStep2(List<Failure> errors, ResourceResolver res) {
        recordError(errors, res);
        try {
            ActionManager step2cleanup = managerFactory.createTaskManager(processName + "- Step 2 Cleanup", res, 1);
            folderVisitor.onEnterNode((node, level) -> step2cleanup.deferredWithResolver(rr -> rr.delete(rr.resolve(node.getPath()))));
            folderVisitor.onVisitChild(null);
            beginStep(step2cleanup, convertSourceToDestination(sourcePath));
        } catch (LoginException ex) {
            Logger.getLogger(FolderRelocator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void buildDestinationFolder(ResourceResolver rr, String sourceFolder) throws PersistenceException, RepositoryException {
        Resource source = rr.getResource(sourceFolder);
        String targetPath = convertSourceToDestination(sourceFolder);
        String targetParentPath = targetPath.substring(0, targetPath.lastIndexOf('/'));
        String targetName = targetPath.substring(targetPath.lastIndexOf('/') + 1);
        Resource destParent = rr.getResource(targetParentPath);
        if (destParent.isResourceType(Resource.RESOURCE_TYPE_NON_EXISTING)) {
            throw new RepositoryException("Unable to find target folder " + targetParentPath);
        }
        rr.create(destParent, targetName, source.getValueMap());
    }

    private String convertSourceToDestination(String source) {
        return source.replaceAll(Pattern.quote(sourcePath), destinationPath);
    }

    private void startStep3(ResourceResolver res) {
        folderVisitor.onEnterNode(null);
        folderVisitor.onVisitChild((node, level) -> step3.deferredWithResolver(
                DeferredActions.retry(5, 100, rr -> moveItem(rr, node.getPath()))
        ));
        beginStep(step3, sourcePath);
    }

    private void moveItem(ResourceResolver rr, String path) throws RepositoryException {
        Session session = rr.adaptTo(Session.class);
        session.move(path, convertSourceToDestination(path));
    }

    private void startStep4(ResourceResolver res) {
        folderVisitor.onEnterNode((node, level) -> step4.deferredWithResolver(
                DeferredActions.retry(5, 100, rr -> rr.delete(rr.resolve(node.getPath())))
        ));
        folderVisitor.onVisitChild(null);
        beginStep(step4, sourcePath);
    }

    private void recordError(List<Failure> errors, ResourceResolver res) {
        haltWork();
    }

    private void beginStep(ActionManager step, String startingNode) {
        step.deferredWithResolver(rr -> {
            Node source = rr.getResource(startingNode).adaptTo(Node.class);
            source.accept(folderVisitor);
        });
    }

    private void success() {
        haltWork();
    }

    private void haltWork() {
        step1.closeAllResolvers();
        step2.closeAllResolvers();
        step3.closeAllResolvers();
        step4.closeAllResolvers();
    }
}
