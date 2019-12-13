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
package com.adobe.acs.commons.mcp.impl.processes.renovator;

import com.adobe.acs.commons.data.Spreadsheet;
import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.fam.actions.Actions;
import com.adobe.acs.commons.mcp.ProcessDefinition;
import com.adobe.acs.commons.mcp.ProcessInstance;
import com.adobe.acs.commons.mcp.form.CheckboxComponent;
import com.adobe.acs.commons.mcp.form.Description;
import com.adobe.acs.commons.mcp.form.FileUploadComponent;
import com.adobe.acs.commons.mcp.form.FormField;
import com.adobe.acs.commons.mcp.form.PathfieldComponent.NodeSelectComponent;
import com.adobe.acs.commons.mcp.form.RadioComponent;
import com.adobe.acs.commons.mcp.form.TextfieldComponent;
import com.adobe.acs.commons.mcp.model.GenericReport;
import com.adobe.acs.commons.mcp.model.ManagedProcess;
import com.adobe.acs.commons.util.visitors.TraversalException;
import com.adobe.acs.commons.util.visitors.TreeFilteringResourceVisitor;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.ReplicationOptions;
import com.day.cq.replication.Replicator;
import com.day.cq.tagging.TagConstants;
import com.day.cq.wcm.api.NameConstants;
import com.day.cq.wcm.api.PageManagerFactory;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.oak.spi.security.authorization.accesscontrol.AccessControlConstants;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;

import static com.adobe.acs.commons.mcp.impl.processes.renovator.Util.*;
import static com.day.cq.commons.jcr.JcrConstants.JCR_PRIMARYTYPE;

/**
 * Relocate Pages and/or Sites using a parallelized move process
 */
public class Renovator extends ProcessDefinition {

    private static final String DESTINATION_COL = "destination";
    private static final String SOURCE_COL = "source";

    public Renovator(PageManagerFactory pageManagerFactory, Replicator replicator) {
        this.pageManagerFactory = pageManagerFactory;
        this.replicator = replicator;
    }

    private final PageManagerFactory pageManagerFactory;
    private final Replicator replicator;

    public enum PublishMethod {
        @Description("No publishing will occur")
        NONE,
        @Description("Publishing will be managed by MCP and the queue is left unaffected so regular publishing can still occur")
        SELF_MANAGED,
        @Description("Publishing is handled by the product publish queue, not recommended very large jobs")
        QUEUE
    }

    @FormField(name = "Multiple moves",
            description = "Excel spreadsheet for performing multiple moves",
            component = FileUploadComponent.class,
            required = false)
    private RequestParameter sourceFile;

    @FormField(name = "Source",
            description = "Select page/site to be moved for single move",
            hint = "/content/my-site/en/my-page",
            component = NodeSelectComponent.class,
            required = false,
            options = {"base=/content"})
    private String sourceJcrPath;

    @FormField(name = "Destination",
            description = "Destination location (must include new name for source node even if same)",
            hint = "Move: /content/new-place/my-page -OR- Rename: /content/new-place/new-name",
            component = NodeSelectComponent.class,
            required = false,
            options = {"base=/content"})
    private String destinationJcrPath;

    @FormField(name = "Max References",
            description = "Limit of how many page references to handle (max per page)",
            hint = "-1 = All, 0 = None, etc.",
            component = TextfieldComponent.class,
            required = false,
            options = {"default=-1"})
    private int maxReferences = -1;

    /*
    @FormField(name = "Reference Search Root",
            description = "Root for reference searches.  Depending on how indexes are set up, / might be the only working value on your system",
            hint = "/ (all), /content, ...",
            component = TextfieldComponent.class,
            required = false,
            options = {"default=/"})
     */
    private String referenceSearchRoot = "/";

    @FormField(name = "Publish",
            description = "Self-managed handles publishing in-process where as Queue will add it to the system publish queue where progress is not tracked here.",
            component = RadioComponent.EnumerationSelector.class,
            options = {"vertical", "default=SELF_MANAGED"})
    public PublishMethod publishMethod = PublishMethod.SELF_MANAGED;

    @FormField(name = "Create versions",
            description = "Create versions for anything being updated/replicated",
            component = CheckboxComponent.class,
            options = {"checked"})
    private boolean createVerionsOnReplicate;

    @FormField(name = "Update status",
            description = "Updates status of content affected by this operation",
            component = CheckboxComponent.class,
            options = {"checked"})
    private boolean updateStatus;

    @FormField(name = "Extensive ACL checks",
            description = "If checked, this evaluates ALL nodes.  If not checked, it only evaluates pages.",
            component = CheckboxComponent.class)
    private boolean extensiveACLChecks = false;

    @FormField(name = "Dry run",
            description = "This runs the ACL checks but doesn't do any actual work.",
            component = CheckboxComponent.class,
            options = {"checked"})
    private boolean dryRun = true;

    @FormField(name = "Detailed report",
            description = "Record extra details in the report, can be rather extensive.  Not recommended for large jobs.",
            component = CheckboxComponent.class)
    private boolean detailedReport = false;

    private final transient String[] requiredMovePrivilegeNames = {
            Privilege.JCR_READ,
            Privilege.JCR_WRITE,
            Privilege.JCR_REMOVE_CHILD_NODES,
            Privilege.JCR_REMOVE_NODE,
            Replicator.REPLICATE_PRIVILEGE
    };
    Privilege[] requiredMovePrivileges;

    private final transient String[] requiredPublishPrivilegeNames = {
            Privilege.JCR_READ,
            Privilege.JCR_WRITE,
            Replicator.REPLICATE_PRIVILEGE
    };
    Privilege[] requiredPublishPrivileges;

    private final transient String[] requiredUpdatePrivilegeNames = {
            Privilege.JCR_READ,
            Privilege.JCR_WRITE
    };
    Privilege[] requiredUpdatePrivileges;

    ReplicatorQueue replicatorQueue = new ReplicatorQueue();
    ReplicationOptions replicationOptions;
    private final Set<MovingNode> moves = Collections.synchronizedSet(new HashSet<>());
    private final Set<String> additionalTargetFolders = Collections.synchronizedSet(new TreeSet<>());
    final Map<String, String> movePaths = Collections.synchronizedMap(new HashMap<>());

    @Override
    public void init() throws RepositoryException {
        replicationOptions = new ReplicationOptions();
        switch (publishMethod) {
            case SELF_MANAGED:
                replicationOptions.setSynchronous(true);
                break;
            default:
                replicationOptions.setSynchronous(false);
                break;
        }
        replicationOptions.setSuppressVersions(!createVerionsOnReplicate);
        replicationOptions.setSuppressStatusUpdate(!updateStatus);

        if (referenceSearchRoot == null || referenceSearchRoot.trim().isEmpty()) {
            referenceSearchRoot = "/";
        }
    }

    private void validateInputs(ResourceResolver res) throws RepositoryException {
        if (sourceFile != null && sourceFile.getSize() > 0) {
            validateSpreadsheetInput();
        } else {
            validateSingleMoveInput();
        }
        for (Map.Entry<String, String> entry : movePaths.entrySet()) {
            String sourcePath = entry.getKey();
            String destinationPath = entry.getValue();

            validateMovePreconditions(res, sourcePath, destinationPath);
        }
    }

    private void validateMovePreconditions(ResourceResolver res, String sourcePath, String destinationPath) throws RepositoryException {
        if (destinationPath.contains(sourcePath + "/")) {
            throw new RepositoryException("Destination must be outside of source path");
        }
        if (!resourceExists(res, sourcePath)) {
            if (!sourcePath.startsWith("/")) {
                throw new RepositoryException("Paths are not valid unless they start with a forward slash, you provided: " + sourcePath);
            } else {
                throw new RepositoryException("Unable to find source " + sourcePath);
            }
        }
        if (!resourceExists(res, destinationPath.substring(0, destinationPath.lastIndexOf('/')))) {
            if (!destinationPath.startsWith("/")) {
                throw new RepositoryException("Paths are not valid unless they start with a forward slash, you provided: " + destinationPath);
            } else if (!destinationPath.startsWith(DAM_ROOT)) {
                throw new RepositoryException("Unable to find destination " + destinationPath);
            }
        }

        if (sourcePath.startsWith(DAM_ROOT) != destinationPath.startsWith(DAM_ROOT)) {
            throw new RepositoryException("Source and destination are incompatible (if one is in the DAM, then so should the other be in the DAM)");
        }
    }

    private void validateSingleMoveInput() throws RepositoryException {
        if (sourceJcrPath == null) {
            throw new RepositoryException("Source path should not be null if no file provided");
        }
        if (destinationJcrPath == null) {
            throw new RepositoryException("Destination path should not be null if no file provided");
        }
        movePaths.put(sourceJcrPath, destinationJcrPath);
    }

    private void validateSpreadsheetInput() throws RepositoryException {
        Spreadsheet sheet;
        try {
            sheet = new Spreadsheet(sourceFile, SOURCE_COL, DESTINATION_COL).buildSpreadsheet();
        } catch (IOException ex) {
            throw new RepositoryException("Unable to parse spreadsheet", ex);
        }

        if (!sheet.getHeaderRow().contains(SOURCE_COL) || !sheet.getHeaderRow().contains(DESTINATION_COL)) {
            throw new RepositoryException(MessageFormat.format("Spreadsheet should have two columns, respectively named {0} and {1}", SOURCE_COL, DESTINATION_COL));
        }

        sheet.getDataRowsAsCompositeVariants().forEach(row -> {
            movePaths.put(row.get(SOURCE_COL).toString(),
                    row.get(DESTINATION_COL).toString());
        });
    }

    private static final String DAM_ROOT = "/content/dam";

    ManagedProcess instanceInfo;

    @Override
    public void buildProcess(ProcessInstance instance, ResourceResolver rr) throws LoginException, RepositoryException {
        validateInputs(rr);
        instanceInfo = instance.getInfo();
        String desc = dryRun ? "DRY RUN: " : "";
        desc += "Publish mode " + publishMethod.name().toLowerCase();
        instance.getInfo().setDescription(desc);
        requiredMovePrivileges = getPrivilegesFromNames(rr, requiredMovePrivilegeNames);
        requiredUpdatePrivileges = getPrivilegesFromNames(rr, requiredUpdatePrivilegeNames);
        requiredPublishPrivileges = getPrivilegesFromNames(rr, requiredPublishPrivilegeNames);
        instance.defineCriticalAction("Eval Struct", rr, this::identifyStructure);
        instance.defineCriticalAction("Eval Refs", rr, this::identifyReferences);
        instance.defineCriticalAction("Check ACLs", rr, this::validateAllAcls);
        if (!dryRun) {
            instance.defineCriticalAction("Build destination", rr, this::buildStructures);
            instance.defineCriticalAction("Move Tree", rr, this::moveTree);
            if (publishMethod != PublishMethod.NONE) {
                instance.defineAction("Activate Tree", rr, this::activateTreeStructure);
                instance.defineAction("Activate New", rr, this::activateNew);
                instance.defineAction("Activate References", rr, this::activateReferences);
                instance.defineAction("Deactivate Old", rr, this::deactivateOld);
            }
            instance.defineAction("Remove source", rr, this::removeSource);
        }
    }

    protected void identifyStructure(ActionManager manager) {
        manager.deferredWithResolver(rr -> {
            AtomicInteger visitedSourceNodes = new AtomicInteger();
            movePaths.forEach((source, dest) -> {
                manager.deferredWithResolver(rr2 -> {
                    Resource res = rr2.getResource(source);
                    Optional<MovingNode> rootNode = buildMoveNode(res);
                    if (rootNode.isPresent()) {
                        identifyStructureFromRoot(visitedSourceNodes, source, dest, rr2, res, rootNode.get());
                    }
                });
            });
        });
    }

    private void identifyStructureFromRoot(AtomicInteger visitedSourceNodes, String source, String dest, ResourceResolver rr, Resource res, MovingNode root) throws TraversalException {
        root.setDestinationPath(dest);
        if (root instanceof MovingAsset) {
            String destFolder = StringUtils.substringBeforeLast(dest, "/");
            if (!additionalTargetFolders.contains(destFolder) && rr.getResource(destFolder) == null) {
                additionalTargetFolders.add(destFolder);
            }
        }
        moves.add(root);
        note(source, Report.misc, "Root path");
        note(source, Report.target, dest);

        TreeFilteringResourceVisitor visitor = new TreeFilteringResourceVisitor(
                JcrConstants.NT_FOLDER,
                JcrResourceConstants.NT_SLING_FOLDER,
                JcrResourceConstants.NT_SLING_ORDERED_FOLDER,
                NameConstants.NT_PAGE
        );

        visitor.setResourceVisitorChecked((r, level) -> buildMoveTree(r, level, root, visitedSourceNodes));
        visitor.setLeafVisitorChecked((r, level) -> buildMoveTree(r, level, root, visitedSourceNodes));

        visitor.accept(res);
        note("All scanned nodes", Report.misc, "Scanned " + visitedSourceNodes.get() + " source nodes.");
    }

    private void buildMoveTree(Resource r, int level, MovingNode root, AtomicInteger visitedSourceNodes) throws RepositoryException {
        if (level > 0) {
            Actions.setCurrentItem(r.getPath());
            Optional<MovingNode> node = buildMoveNode(r);
            if (node.isPresent()) {
                MovingNode childNode = node.get();
                String parentPath = StringUtils.substringBeforeLast(r.getPath(), "/");
                MovingNode parent = root.findByPath(parentPath)
                        .orElseThrow(() -> new RepositoryException("Unable to find data structure for node " + parentPath));
                parent.addChild(childNode);
                if (detailedReport) {
                    note(childNode.getSourcePath(), Report.target, childNode.getDestinationPath());
                }
                visitedSourceNodes.addAndGet(1);
            }
        }
    }

    private Optional<MovingNode> buildMoveNode(Resource res) throws RepositoryException {
        String type = res.getValueMap().get(JCR_PRIMARYTYPE, String.class);
        MovingNode node = null;
        switch (type) {
            case JcrConstants.NT_FOLDER:
            case JcrResourceConstants.NT_SLING_FOLDER:
            case JcrResourceConstants.NT_SLING_ORDERED_FOLDER:
                node = new MovingFolder();
                break;
            case NameConstants.NT_PAGE:
                node = new MovingPage(pageManagerFactory);
                break;
            case DamConstants.NT_DAM_ASSET:
                node = new MovingAsset();
                break;
            case JcrConstants.NT_UNSTRUCTURED:
                if (res.getName().equals(JcrConstants.JCR_CONTENT)) {
                    return Optional.empty();
                } else {
                    node = new MovingResource();
                }
                break;
            case "cq:CommentAttachment":
                node = new MovingResource();
                break;
            case AccessControlConstants.NT_REP_ACL:
                node = new MovingResource();
                break;
            case "cq:PageContent":
                // Page content is moved with the page, so ignore it here
                break;
            case TagConstants.NT_TAG:
            default:
                throw new RepositoryException("Type " + type + " is not supported at this time!");
        }

        if (node == null) {
            return Optional.empty();
        } else {
            node.setSourcePath(res.getPath());
            return Optional.of(node);
        }
    }

    public void findReferences(ResourceResolver rr, MovingNode node) throws IllegalAccessException {
        node.findReferences(rr, referenceSearchRoot, maxReferences);
    }

    protected void identifyReferences(ActionManager manager) {
        AtomicInteger discoveredReferences = new AtomicInteger();
        manager.deferredWithResolver(rr -> {
            moves.forEach(node -> {
                manager.deferredWithResolver(rr2 -> {
                    node.visit(childNode -> {
                        if (childNode.isSupposedToBeReferenced()) {
                            manager.deferredWithResolver(rr3 -> {
                                Actions.setCurrentItem("Looking for references to " + childNode.getSourcePath());
                                findReferences(rr3, childNode);
                                discoveredReferences.addAndGet(childNode.getAllReferences().size());
                                if (detailedReport) {
                                    note(childNode.getSourcePath(), Report.all_references, childNode.getAllReferences().size());
                                    note(childNode.getSourcePath(), Report.published_references, childNode.getPublishedReferences().size());
                                }
                            });
                        }
                    });
                });
            });
        });
        manager.onFinish(() -> {
            note("All discovered references", Report.misc, "Discovered " + discoveredReferences.get() + " references.");
        });
    }

    protected void validateAllAcls(ActionManager manager) {
        manager.deferredWithResolver(rr -> {
            moves.forEach(node -> {
                manager.deferredWithResolver(rr2 -> {
                    node.visit(childNode -> {
                        manager.deferredWithResolver(rr3 -> {
                            validateAcls(childNode, rr3);
                        });
                    });
                });
            });
        });
    }

    private void validateAcls(MovingNode childNode, ResourceResolver rr3) throws RepositoryException {
        try {
            Actions.setCurrentItem("Checking ACLs on " + childNode.getSourcePath());
            checkNodeAcls(rr3, childNode.getSourcePath(), requiredMovePrivileges);
            for (String ref : childNode.getAllReferences()) {
                Actions.setCurrentItem("Checking ACLs on " + ref + " which references " + childNode.getSourcePath());
                validateAclsForReference(childNode, rr3, ref);
            }
            if (detailedReport) {
                note(childNode.getSourcePath(), Report.acl_check, "Passed");
            }
        } catch (Exception e) {
            note(childNode.getSourcePath(), Report.acl_check, "Failed");
            throw e;
        }
    }

    private void validateAclsForReference(MovingNode childNode, ResourceResolver rr, String ref) throws RepositoryException {
        if (publishMethod != PublishMethod.NONE
                && childNode.getPublishedReferences().contains(ref)) {
            checkNodeAcls(rr, childNode.getSourcePath(), requiredPublishPrivileges);
        } else {
            checkNodeAcls(rr, childNode.getSourcePath(), requiredUpdatePrivileges);
        }
    }

    // Try to create as much of the folder structures ahead of time (for assets, etc)
    protected void buildStructures(ActionManager manager) {
        manager.deferredWithResolver(rr -> {
            moves.forEach(node -> {
                manager.deferredWithResolver(rr2 -> {
                    node.visit(childNode -> {
                        manager.deferredWithResolver(rr3 -> {
                            Actions.setCurrentItem("Building structure for " + childNode.getSourcePath());
                            childNode.move(replicatorQueue, rr3);
                        });
                    }, null, MovingNode::isCopiedBeforeMove);
                });
            });
            additionalTargetFolders.forEach(path -> {
                manager.deferredWithResolver(rr2 -> {
                    Actions.setCurrentItem("Building structure for " + path);
                    performNecessaryReplicationOnAncestors(rr2, path);
                    ResourceUtil.getOrCreateResource(rr2, path, Collections.EMPTY_MAP, "sling:Folder", false);
                    if (detailedReport) {
                        note(path, Report.misc, "Created additional destination folder");
                    }
                });
            });
        });
    }

    // Move assets and pages, and in some cases folders that were not already moved in the previous step
    protected void moveTree(ActionManager manager) {
        manager.deferredWithResolver(rr -> {
            moves.forEach(node -> {
                manager.deferredWithResolver(rr2 -> {
                    node.visit(childNode -> {
                        if (!childNode.isCopiedBeforeMove() || !resourceExists(rr2, childNode.getDestinationPath())) {
                            manager.deferredWithResolver(rr3 -> {
                                Actions.setCurrentItem("Moving " + childNode.getSourcePath());
                                childNode.move(replicatorQueue, rr3);
                            });
                        }
                    });
                });
            });
        });
    }

    protected void activateTreeStructure(ActionManager manager) {
        manager.deferredWithResolver(rr -> {
            moves.forEach(node -> {
                manager.deferredWithResolver(rr2 -> {
                    node.visit(childNode -> {
                        manager.deferredWithResolver(rr3 -> {
                            Actions.setCurrentItem("Replicating " + childNode.getDestinationPath());
                            performNecessaryReplication(rr3, childNode.getDestinationPath());
                        });
                    }, null, MovingNode::isCopiedBeforeMove);
                });
            });
        });
    }

    protected void activateNew(ActionManager step3) {
        step3.deferredWithResolver(rr -> {
            getAllActivationPaths().filter(this::isActivationPath)
                    .forEach(path -> {
                        step3.deferredWithResolver(rr2 -> {
                            Actions.setCurrentItem("Replicating " + path);
                            performNecessaryReplication(rr2, path);
                        });
                    });
        });
    }

    protected void activateReferences(ActionManager step4) {
        step4.deferredWithResolver(rr -> {
            getAllReplicationPaths().filter(this::isForeignPath)
                    .forEach(path -> {
                        step4.deferredWithResolver(rr2 -> {
                            Actions.setCurrentItem("Replicating references " + path);
                            performNecessaryReplication(rr2, path);
                        });
                    });
        });
    }

    protected void deactivateOld(ActionManager step5) {
        step5.deferredWithResolver(rr -> {
            getAllReplicationPaths().filter(this::isDeactivationPath)
                    .forEach(path -> {
                        step5.deferredWithResolver(rr2 -> {
                            Actions.setCurrentItem("Deactivating " + path);
                            performNecessaryReplication(rr2, path);
                        });
                    });
        });
    }

    protected boolean isDeactivationPath(String path) {
        boolean result = false;
        for (Map.Entry<String, String> mapping : movePaths.entrySet()) {
            String sourcePath = mapping.getKey();
            String destinationPath = mapping.getValue();
            if (path.startsWith(sourcePath)) {
                result = true;
            } else if (path.startsWith(destinationPath)) {
                return false;
            }
        }
        return result;
    }

    protected boolean isActivationPath(String path) {
        return !isDeactivationPath(path);
    }

    protected boolean isForeignPath(String path) {
        for (Map.Entry<String, String> mapping : movePaths.entrySet()) {
            String sourcePath = mapping.getKey();
            String destinationPath = mapping.getValue();
            if (path.startsWith(sourcePath) || path.startsWith(destinationPath)) {
                return false;
            }
        }
        return true;
    }

    protected void removeSource(ActionManager manager) {
        manager.deferredWithResolver(rr -> {
            for (MovingNode node : moves) {
                //TODO: DOUBLE-CHECK NOT TO DELETE ANYTHING?
                rr.delete(rr.resolve(node.getSourcePath()));
            }
        });
    }

    @SuppressWarnings("squid:S00115")
    enum Report {
        misc, target, acl_check, all_references, published_references, move_time, activate_time, deactivate_time
    }

    private final Map<String, EnumMap<Report, Object>> reportData = new LinkedHashMap<>();

    private void note(String page, Report col, Object value) {
        synchronized (reportData) {
            if (!reportData.containsKey(page)) {
                reportData.put(page, new EnumMap<>(Report.class));
            }
            reportData.get(page).put(col, value);
        }
    }

    @Override
    public void storeReport(ProcessInstance instance, ResourceResolver rr) throws RepositoryException, PersistenceException {
        GenericReport report = new GenericReport();
        report.setRows(reportData, SOURCE_COL, Report.class);
        report.persist(rr, instance.getPath() + "/jcr:content/report");
    }

    private Privilege[] getPrivilegesFromNames(ResourceResolver res, String[] names) throws RepositoryException {
        Session session = res.adaptTo(Session.class);
        AccessControlManager acm = session.getAccessControlManager();
        Privilege[] prvlgs = new Privilege[names.length];
        for (int i = 0; i < names.length; i++) {
            prvlgs[i] = acm.privilegeFromName(names[i]);
        }
        return prvlgs;
    }

    public void checkNodeAcls(ResourceResolver res, String path, Privilege[] prvlgs) throws RepositoryException {
        Actions.setCurrentItem(path);
        Session session = res.adaptTo(Session.class);
        boolean report = res.getResource(path).getResourceType().equals(NameConstants.NT_PAGE);
        if (!session.getAccessControlManager().hasPrivileges(path, prvlgs)) {
            note(path, Report.acl_check, "FAIL");
            throw new RepositoryException("Insufficient permissions to permit move operation");
        } else if (report) {
            note(path, Report.acl_check, "PASS");
        }
    }

    private String reversePathLookup(String path) {
        for (Map.Entry<String, String> mapping : movePaths.entrySet()) {
            String sourcePath = mapping.getKey();
            String destinationPath = mapping.getValue();
            if (path.startsWith(destinationPath)) {
                return path.replaceAll(Pattern.quote(destinationPath), sourcePath);
            } else {
                return path;
            }
        }
        return null;
    }

    private Stream<String> getAllActivationPaths() {
        Set<String> allPaths = new TreeSet<>();
        moves.forEach((n) -> {
            n.visit(node -> {
                allPaths.addAll(node.getPublishedReferences());
            });
        });
        allPaths.addAll(replicatorQueue.getActivateOperations().keySet());
        return allPaths.stream();
    }

    private Stream<String> getAllReplicationPaths() {
        return Stream.concat(
                replicatorQueue.getActivateOperations().keySet().stream(),
                replicatorQueue.getDeactivateOperations().keySet().stream()
        ).distinct();
    }

    private void performNecessaryReplication(ResourceResolver rr, String path) throws ReplicationException {
        ReplicationActionType action;
        boolean isDeactivation = isDeactivationPath(path);
        if (isDeactivation) {
            action = ReplicationActionType.DEACTIVATE;
        } else {
            action = ReplicationActionType.ACTIVATE;
        }
        long start = System.currentTimeMillis();
        if (!dryRun) {
            replicator.replicate(rr.adaptTo(Session.class), action, path);
        }
        long end = System.currentTimeMillis();
        if (isDeactivation) {
            note(path, Report.deactivate_time, end - start);
        } else {
            note(reversePathLookup(path), Report.activate_time, end - start);
        }
    }

    private void performNecessaryReplicationOnAncestors(ResourceResolver rr, String path) throws ReplicationException {
        String checkPath = "";
        for (String part : path.split(Pattern.quote("/"))) {
            if (part.isEmpty()) {
                continue;
            }
            checkPath += "/" + part;
            if (rr.getResource(checkPath) == null) {
                performNecessaryReplication(rr, checkPath);
            }
        }
    }
}
