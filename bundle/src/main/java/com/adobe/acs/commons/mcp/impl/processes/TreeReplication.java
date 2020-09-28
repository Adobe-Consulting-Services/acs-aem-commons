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
import com.adobe.acs.commons.functions.CheckedFunction;
import com.adobe.acs.commons.mcp.ProcessDefinition;
import com.adobe.acs.commons.mcp.ProcessInstance;
import com.adobe.acs.commons.mcp.form.CheckboxComponent;
import com.adobe.acs.commons.mcp.form.FormField;
import com.adobe.acs.commons.mcp.form.PathfieldComponent;
import com.adobe.acs.commons.mcp.form.SelectComponent;
import com.adobe.acs.commons.mcp.form.TextfieldComponent;
import com.adobe.acs.commons.mcp.model.GenericReport;
import com.adobe.acs.commons.util.visitors.TreeFilteringResourceVisitor;
import com.day.cq.replication.AgentFilter;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationOptions;
import com.day.cq.replication.Replicator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Replace folder thumbnails under a user-definable set of circumstances As a
 * business user, I would like an easy way to scan and repair missing
 * thumbnails, or just regenerate all thumbnails under a given tree of the DAM.
 */
public class TreeReplication extends ProcessDefinition {

    protected static enum ReplicationFilter {
        ALL(r -> true),
        FOLDERS_ONLY(TreeReplication::isFolder);

        CheckedFunction<Resource, Boolean> test;

        @SuppressWarnings("squid:UnusedPrivateMethod")
        private ReplicationFilter(CheckedFunction<Resource, Boolean>... tests) {
            this.test = CheckedFunction.or(tests);
        }

        @SuppressWarnings("squid:S00112")
        public boolean shouldReplicate(Resource r) throws Exception {
            return this.test.apply(r);
        }
    }

    protected static enum QueueMethod {
        USE_PUBLISH_QUEUE, USE_MCP_QUEUE, MCP_AFTER_10K
    }

    public static int ASYNC_LIMIT = 10000;

    private Replicator replicatorService;

    @FormField(name = "Starting Path",
            component = PathfieldComponent.FolderSelectComponent.class,
            description = "This folder and all its subfolders will be published")
    private String startingPath = "/content/dam";

    @FormField(name = "What to Publish",
            component = SelectComponent.EnumerationSelector.class,
            description = "Publish only folders or all content inside of folders",
            options = "default=FOLDERS_ONLY"
    )
    private ReplicationFilter publishFilter = ReplicationFilter.FOLDERS_ONLY;

    @FormField(name = "Agents",
            component = TextfieldComponent.class,
            description = "Publish agents to use, if blank then all default agents will be used. Multiple agents can be listed using commas or regex.")
    private String agents = null;
    List<String> agentList = new ArrayList<>();
    AgentFilter replicationAgentFilter;

    @FormField(name = "Queueing Method",
            component = SelectComponent.EnumerationSelector.class,
            description = "For small publishing tasks, standard is sufficient.  For large folder trees, MCP is recommended.",
            options = "default=USE_MCP_QUEUE")
    QueueMethod queueMethod = QueueMethod.USE_MCP_QUEUE;

    @FormField(name = "Dry Run",
            component = CheckboxComponent.class,
            options = "checked",
            description = "If checked, only generate a report but don't perform the work"
    )
    private boolean dryRun = true;

    public TreeReplication(Replicator replicator) {
        replicatorService = replicator;
    }

    @Override
    public void init() {
        // Nothing to do here
    }

    @Override
    public void buildProcess(ProcessInstance instance, ResourceResolver rr) throws LoginException, RepositoryException {
        instance.getInfo().setDescription(startingPath + "; "
                + publishFilter.name() + "; "
                + queueMethod.name()
                + (dryRun ? " (dry run)" : ""));
        instance.defineCriticalAction("Activate tree structure", rr, this::activateTreeStructure);
        if (publishFilter != ReplicationFilter.FOLDERS_ONLY) {
            instance.defineAction("Activiate content", rr, this::activateContent);
        }
        if (agents == null || agents.isEmpty()) {
            replicationAgentFilter = AgentFilter.DEFAULT;
        } else {
            agentList = Arrays.asList(agents.toLowerCase().split(","));
            replicationAgentFilter = agent -> agentList.stream().anyMatch(p -> p.matches(agent.getId().toLowerCase()));
        }
    }

    public static enum ReportColumns {
        PATH, ACTION, DESCRIPTION
    }

    List<EnumMap<ReportColumns, String>> reportData = Collections.synchronizedList(new ArrayList<>());

    private void record(String path, String action, String description) {
        EnumMap<ReportColumns, String> row = new EnumMap<>(ReportColumns.class);
        row.put(ReportColumns.PATH, path);
        row.put(ReportColumns.ACTION, action);
        row.put(ReportColumns.DESCRIPTION, description);
        reportData.add(row);
    }

    @Override
    public void storeReport(ProcessInstance instance, ResourceResolver rr) throws RepositoryException, PersistenceException {
        GenericReport report = new GenericReport();
        report.setName("Tree Replication " + startingPath);
        report.setRows(reportData, ReportColumns.class);
        report.persist(rr, instance.getPath() + "/jcr:content/report");
    }

    // Should match nt:folder, sling:OrderedFolder, sling:UnorderedFolder, etc
    public static Boolean isFolder(Resource res) {
        String primaryType = String.valueOf(res.getResourceMetadata().get("jcr:primaryType"));
        return (primaryType.toLowerCase().contains("folder"));
    }

    private void activateTreeStructure(ActionManager t) {
        TreeFilteringResourceVisitor visitor = new TreeFilteringResourceVisitor();
        visitor.setResourceVisitorChecked((resource, u) -> {
            String path = resource.getPath();
            if (publishFilter.shouldReplicate(resource)) {
                t.deferredWithResolver(rr -> performReplication(t, path));
            } else {
                record(path, "Skip", "Skipping folder");
            }
        });
        t.deferredWithResolver(rr -> visitor.accept(rr.getResource(startingPath)));
    }

    private void activateContent(ActionManager t) {
        TreeFilteringResourceVisitor visitor = new TreeFilteringResourceVisitor();
        visitor.setLeafVisitorChecked((resource, u) -> {
            String path = resource.getPath();
            if (publishFilter.shouldReplicate(resource)) {
                t.deferredWithResolver(rr -> performReplication(t, path));
            } else {
                record(path, "Skip", "Skipping content");
            }
        });
        t.deferredWithResolver(rr -> visitor.accept(rr.getResource(startingPath)));
    }

    public AtomicInteger replicationCount = new AtomicInteger();

    private void performReplication(ActionManager t, String path) {
        int counter = replicationCount.incrementAndGet();
        if (queueMethod == QueueMethod.USE_MCP_QUEUE
                || (queueMethod == QueueMethod.MCP_AFTER_10K && counter >= ASYNC_LIMIT)) {
            performSynchronousReplication(t, path);
        } else {
            performAsynchronousReplication(t, path);
        }
    }

    private void performSynchronousReplication(ActionManager t, String path) {
        ReplicationOptions options = new ReplicationOptions();
        options.setSynchronous(true);
        options.setFilter(replicationAgentFilter);
        scheduleReplication(t, options, path);
        record(path, "Replicate", "Synchronous replication");
    }

    private void performAsynchronousReplication(ActionManager t, String path) {
        ReplicationOptions options = new ReplicationOptions();
        options.setSynchronous(false);
        options.setFilter(replicationAgentFilter);
        scheduleReplication(t, options, path);
        record(path, "Replicate", "Asynchronous replication");
    }

    private void scheduleReplication(ActionManager t, ReplicationOptions options, String path) {
        if (!dryRun) {
            t.deferredWithResolver(rr -> {
                Session session = rr.adaptTo(Session.class);
                replicatorService.replicate(session, ReplicationActionType.ACTIVATE, path, options);
            });
        }
    }
}
