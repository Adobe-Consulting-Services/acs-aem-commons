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
import com.adobe.acs.commons.mcp.ProcessDefinition;
import com.adobe.acs.commons.mcp.ProcessInstance;
import com.adobe.acs.commons.mcp.form.CheckboxComponent;
import com.adobe.acs.commons.mcp.form.FormField;
import com.adobe.acs.commons.mcp.form.PathfieldComponent;
import com.adobe.acs.commons.mcp.form.RadioComponent;
import com.adobe.acs.commons.mcp.form.TextfieldComponent;
import com.adobe.acs.commons.mcp.model.GenericReport;
import com.adobe.acs.commons.mcp.model.ManagedProcess;
import com.adobe.acs.commons.util.visitors.SimpleFilteringResourceVisitor;
import com.adobe.acs.commons.util.visitors.TreeFilteringResourceVisitor;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.ReplicationOptions;
import com.day.cq.replication.Replicator;
import com.day.cq.wcm.api.NameConstants;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.PageManagerFactory;
import com.day.cq.wcm.commons.ReferenceSearch;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import org.apache.commons.lang.reflect.FieldUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Relocate Pages and/or Sites using a parallelized move process
 */
@Component
@Service(ProcessDefinition.class)
public class PageRelocator implements ProcessDefinition {

    @Reference
    PageManagerFactory pageManagerFactory;

    @Reference
    Replicator replicator;

    public static enum Mode {
        RENAME, MOVE
    };

    public enum PUBLISH_METHOD {
        NONE, SELF_MANAGED, QUEUE
    };

    @FormField(name = "Source page",
            description = "Select page/site to be moved",
            hint = "/content/my-site/en/my-page",
            component = PathfieldComponent.PageSelectComponent.class,
            options = {"base=/content"})
    private String sourcePath;

    @FormField(name = "Destination",
            description = "Destination parent for move, or destination parent folder plus new name for rename",
            hint = "Move: /content/my-site/some-page | Rename: /content/my-site/some-page/new-name",
            component = PathfieldComponent.PageSelectComponent.class,
            options = {"base=/content"})
    private String destinationPath;
    
    @FormField(name = "Max References",
            description = "Limit of how many page references to handle (max per page)",
            hint = "-1 = All, 0 = None, etc.",
            component = TextfieldComponent.class,
            required = false,
            options = {"default=-1"})
    private int maxReferences = -1;        
    
    @FormField(name = "Reference Search Root",
            description = "Root for reference searches.  /content is preferred for simple pages, but might miss stuff in other places like /var",
            hint = "/content or / (same as blank)",
            component = TextfieldComponent.class,
            required = false,
            options = {"default=/content"})
    private String referenceSearchRoot = "/content";

    @FormField(name = "Mode",
            description = "Move relocates the page keeping the original name.  Rename changes the name, optionally moving the page.",
            required = false,
            component = RadioComponent.EnumerationSelector.class,
            options = {"horizontal", "default=MOVE"})
    private Mode mode;

    @FormField(name = "Publish",
            description = "Self-managed handles publishing in-process where as Queue will add it to the system publish queue where progress is not tracked here.",
            required = false,
            component = RadioComponent.EnumerationSelector.class,
            options = {"horizontal", "default=SELF_MANAGED"})
    public PUBLISH_METHOD publishMethod;

    @FormField(name = "Create versions",
            description = "Create versions for anything being replicated",
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

    transient private final String[] requiredPrivilegeNames = {
        Privilege.JCR_READ,
        Privilege.JCR_WRITE,
        Privilege.JCR_REMOVE_CHILD_NODES,
        Privilege.JCR_REMOVE_NODE,
        Replicator.REPLICATE_PRIVILEGE
    };
    Privilege[] requiredPrivileges;

    ReplicatorQueue replicatorQueue = new ReplicatorQueue();
    ReplicationOptions replicationOptions;

    @Override
    public String getName() {
        return "Page Relocator";
    }

    @Override
    public void init() throws RepositoryException {
        if (mode == Mode.MOVE) {
            String nodeName = sourcePath.substring(sourcePath.lastIndexOf('/'));
            destinationPath += nodeName;
        }

        replicationOptions = new ReplicationOptions();
        switch (publishMethod) {
            case SELF_MANAGED:
                replicationOptions.setSynchronous(true);
                break;
            case QUEUE:
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
        if (sourcePath == null) {
            throw new RepositoryException("Source path should not be null");
        }
        if (destinationPath == null) {
            throw new RepositoryException("Destination path should not be null");
        }
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
            } else {
                throw new RepositoryException("Unable to find destination " + destinationPath);
            }
        }
    }

    ManagedProcess instanceInfo;
    @Override
    public void buildProcess(ProcessInstance instance, ResourceResolver rr) throws LoginException, RepositoryException {
        validateInputs(rr);
        instanceInfo = instance.getInfo();
        String desc = dryRun ? "DRY RUN: " : "";
        desc += mode.name().toLowerCase() + " " + sourcePath + " to " + destinationPath;
        desc += "; Publish mode " + publishMethod.name().toLowerCase();
        instance.getInfo().setDescription(desc);
        requiredPrivileges = getPrivilegesFromNames(rr, requiredPrivilegeNames);
        instance.defineCriticalAction("Check ACLs", rr, this::validateAllAcls);
        instance.defineAction("Move Pages", rr, this::movePages);
        if (publishMethod != PUBLISH_METHOD.NONE) {
            instance.defineAction("Activate New", rr, this::activateNew);
            instance.defineAction("Activate References", rr, this::activateReferences);
            instance.defineAction("Deactivate Old", rr, this::deactivateOld);
        }
        instance.defineAction("Remove old pages", rr, this::removeSource);
    }

    protected void validateAllAcls(ActionManager step1) {
        SimpleFilteringResourceVisitor pageVisitor;
        if (extensiveACLChecks) {
            pageVisitor = new SimpleFilteringResourceVisitor();
            pageVisitor.setLeafVisitor((resource, level) -> step1.deferredWithResolver(rr -> checkNodeAcls(rr, resource.getPath(), requiredPrivileges)));
        } else {
            pageVisitor = new TreeFilteringResourceVisitor(NameConstants.NT_PAGE);
        }
        pageVisitor.setBreadthFirstMode();
        pageVisitor.setResourceVisitor((resource, level) -> step1.deferredWithResolver(rr -> checkNodeAcls(rr, resource.getPath(), requiredPrivileges)));
        beginStep(step1, sourcePath, pageVisitor);
    }

    protected void movePages(ActionManager step2) {
        TreeFilteringResourceVisitor pageVisitor = new TreeFilteringResourceVisitor(NameConstants.NT_PAGE);
        pageVisitor.setBreadthFirstMode();
        pageVisitor.setResourceVisitor((resource, level) -> step2.deferredWithResolver(rr -> movePage(rr, resource.getPath())));
        beginStep(step2, sourcePath, pageVisitor);
    }

    protected void activateNew(ActionManager step3) {
        step3.deferredWithResolver(rr -> {
            getAllReplicationPaths().filter(p -> p.startsWith(destinationPath) && !p.startsWith(sourcePath))
                    .forEach(path -> {
                        step3.deferredWithResolver(rr2 -> {
                            performNecessaryReplication(rr2, path);
                        });
                    });
        });
    }

    protected void activateReferences(ActionManager step4) {
        step4.deferredWithResolver(rr -> {
            getAllReplicationPaths().filter(p -> !p.startsWith(destinationPath) && !p.startsWith(sourcePath))
                    .forEach(path -> {
                        step4.deferredWithResolver(rr2 -> {
                            performNecessaryReplication(rr2, path);
                        });
                    });
        });
    }

    protected void deactivateOld(ActionManager step5) {
        step5.deferredWithResolver(rr -> {
            getAllReplicationPaths().filter(p -> p.startsWith(sourcePath))
                    .forEach(path -> {
                        step5.deferredWithResolver(rr2 -> {
                            performNecessaryReplication(rr2, path);
                        });
                    });
        });
    }
    
    protected void removeSource(ActionManager step6) {
        if (instanceInfo.getReportedErrors().isEmpty() && !dryRun) {
            step6.deferredWithResolver(rr -> {
                rr.delete(rr.getResource(sourcePath));
            });
        }
    }    

    private void beginStep(ActionManager step, String startingNode, SimpleFilteringResourceVisitor visitor) {
        try {
            step.withResolver(rr -> visitor.accept(rr.getResource(startingNode))
            );
        } catch (Exception ex) {
            Logger.getLogger(FolderRelocator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    enum REPORT {
        target, acl_check, all_references, published_references, move_time, activate_time, deactivate_time
    };
    final private Map<String, EnumMap<REPORT, Object>> reportData = new TreeMap<>();

    private void note(String page, REPORT col, Object value) {
        synchronized (reportData) {
            if (!reportData.containsKey(page)) {
                reportData.put(page, new EnumMap<>(REPORT.class));
            }
            reportData.get(page).put(col, value);
        }
    }

    @Override
    public void storeReport(ProcessInstance instance, ResourceResolver rr) throws RepositoryException, PersistenceException {
        GenericReport report = new GenericReport();
        report.setRows(reportData, "Source", REPORT.class);
        report.persist(rr, instance.getPath() + "/jcr:content/report");
    }

    //--- Utility functions
    private boolean resourceExists(ResourceResolver rr, String path) {
        Resource res = rr.getResource(path);
        return res != null && !Resource.RESOURCE_TYPE_NON_EXISTING.equals(res.getResourceType());
    }

    private void waitUntilResourceFound(ResourceResolver rr, String path) throws InterruptedException, RepositoryException {
        for (int i = 0; i < 10; i++) {
            if (resourceExists(rr, path)) {
                return;
            }
            Thread.sleep(100);
            rr.refresh();
        }
        throw new RepositoryException("Resource not found: " + path);
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

    private void checkNodeAcls(ResourceResolver res, String path, Privilege[] prvlgs) throws RepositoryException {
        Actions.setCurrentItem(path);
        Session session = res.adaptTo(Session.class);
        boolean report = res.getResource(path).getResourceType().equals(NameConstants.NT_PAGE);
        if (!session.getAccessControlManager().hasPrivileges(path, prvlgs)) {
            note(path, REPORT.acl_check, "FAIL");
            throw new RepositoryException("Insufficient permissions to permit move operation");
        } else if (report) {
            note(path, REPORT.acl_check, "PASS");
        }
    }

    private void movePage(ResourceResolver rr, String sourcePage) throws Exception {
        PageManager manager = pageManagerFactory.getPageManager(rr);
        Field replicatorField = FieldUtils.getDeclaredField(manager.getClass(), "replicator", true);
        FieldUtils.writeField(replicatorField, manager, replicatorQueue);
        String destination = convertSourceToDestination(sourcePage);
        String destinationParent = destination.substring(0, destination.lastIndexOf('/'));
        note(sourcePage, REPORT.target, destination);
        String beforeName = "";
        long start = System.currentTimeMillis();
        
        String contentPath = sourcePage + "/jcr:content";
        List<String> refs = new ArrayList<>();
        List<String> publishRefs = new ArrayList<>();
        
        if (maxReferences != 0 && resourceExists(rr, contentPath)) {
            ReferenceSearch refSearch = new ReferenceSearch();
            refSearch.setExact(true);
            refSearch.setHollow(true);
            refSearch.setMaxReferencesPerPage(maxReferences);
            refSearch.setSearchRoot(referenceSearchRoot);
            refSearch.search(rr, sourcePath).values().stream()
                            .peek(p->refs.add(p.getPagePath()))
                            .filter(this::needsToBePublished).map(ReferenceSearch.Info::getPagePath)
                            .collect(Collectors.toCollection(()->publishRefs));
        }
        note(sourcePage, REPORT.all_references, refs.size());
        note(sourcePage, REPORT.published_references, refs.size());        
        
        if (!dryRun) {
            Actions.retry(10, 500, res -> {
                waitUntilResourceFound(res, destinationParent);
                Resource source = rr.getResource(sourcePage);
                if (resourceExists(res, contentPath)) {
                    manager.move(source, destination, beforeName, true, true, listToStringArray(refs), listToStringArray(publishRefs));
                } else {
                    Map<String, Object> props = new HashMap<>();
                    Resource parent = res.getResource(destinationParent);
                    res.create(parent, source.getName(), source.getValueMap());                    
                }
                res.commit();
                res.refresh();

                source = rr.getResource(sourcePage);
                if (source != null && source.hasChildren()) {
                    for (Resource child : source.getChildren()) {
                        res.move(child.getPath(), destination);
                    }
                    res.commit();
                }
            }).accept(rr);
        }
        long end = System.currentTimeMillis();
        note(sourcePage, REPORT.move_time, end - start);

    }

    private String convertSourceToDestination(String path) {
        return path.replaceAll(Pattern.quote(sourcePath), destinationPath);
    }

    private String reversePathLookup(String path) {
        if (path.startsWith(destinationPath)) {
            return path.replaceAll(Pattern.quote(destinationPath), sourcePath);
        } else {
            return path;
        }
    }

    private Stream<String> getAllReplicationPaths() {
        Stream s1 = replicatorQueue.activateOperations.keySet().stream();
        Stream s2 = replicatorQueue.deactivateOperations.keySet().stream();
        return Stream.concat(s1, s2);
    }

    private void performNecessaryReplication(ResourceResolver rr, String path) throws ReplicationException {
        ReplicationActionType action;
        if (path.startsWith(sourcePath)) {
            action = ReplicationActionType.DEACTIVATE;
        } else {
            action = ReplicationActionType.ACTIVATE;
        }
        long start = System.currentTimeMillis();
        if (!dryRun) {
            replicator.replicate(rr.adaptTo(Session.class), action, path);
        }
        long end = System.currentTimeMillis();
        if (path.startsWith(sourcePath)) {
            note(path, REPORT.deactivate_time, end - start);
        } else {
            note(reversePathLookup(path), REPORT.activate_time, end - start);
        }
    }
    
    private boolean needsToBePublished(ReferenceSearch.Info pageInfo) {
        return false;
    }
    
    private String[] listToStringArray(List<String> values) {
        return values.toArray(new String[0]);
    }
}
