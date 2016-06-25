/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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

package com.adobe.acs.commons.workflow.process.impl;

import com.adobe.acs.commons.fam.ThrottledTaskRunner;
import com.adobe.acs.commons.util.ParameterUtil;
import com.adobe.acs.commons.util.WorkflowHelper;
import com.adobe.acs.commons.util.visitors.ContentVisitor;
import com.adobe.acs.commons.util.visitors.ResourceRunnable;
import com.adobe.acs.commons.workflow.WorkflowPackageManager;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationOptions;
import com.day.cq.replication.Replicator;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component(
        metatype = true,
        label = "ACS AEM Commons - Workflow Process - Replicate with Options",
        description = "Replicates the content based on the process arg replication configuration using FAM,"
)
@Properties({
        @Property(
                label = "Workflow Label",
                name = "process.label",
                value = "Replicate with Options",
                description = "Replicates the content based on the process arg replication configuration (serial execution)"
        )
})
@Service
public class ReplicateWithOptionsWorkflowProcess implements WorkflowProcess {
    private static final Logger log = LoggerFactory.getLogger(ReplicateWithOptionsWorkflowProcess.class);

    private static final String ARG_TRAVERSE_TREE = "traverseTree";
    private static final String ARG_REPLICATION_ACTION_TYPE = "replicationActionType";
    private static final String ARG_REPLICATION_SYNCHRONOUS = "synchronous";
    private static final String ARG_REPLICATION_SUPPRESS_VERSIONS = "suppressVersions";

    @Reference
    private WorkflowPackageManager workflowPackageManager;

    @Reference
    private Replicator replicator;

    @Reference
    private ThrottledTaskRunner throttledTaskRunner;

    @Reference
    private WorkflowHelper workflowHelper;

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) throws WorkflowException {
        ResourceResolver resourceResolver = null;

        try {
            resourceResolver = workflowHelper.getResourceResolver(workflowSession);
            final String originalPayload = (String) workItem.getWorkflowData().getPayload();
            final List<String> payloads = workflowPackageManager.getPaths(resourceResolver, originalPayload);
            final ProcessArgs processArgs = new ProcessArgs(metaDataMap);

            final AtomicInteger count = new AtomicInteger(0);

            // Anonymous inner class to facilitate counting of processed payloads
            final ResourceRunnable replicatorRunnable = new ResourceRunnable() {
                @Override
                public void run(final Resource resource) throws Exception {
                    throttledTaskRunner.waitForLowCpuAndLowMemory();
                    replicator.replicate(resource.getResourceResolver().adaptTo(Session.class),
                            processArgs.getReplicationActionType(),
                            resource.getPath(),
                            processArgs.getReplicationOptions());
                }
            };

            final ContentVisitor visitor = new ContentVisitor(replicatorRunnable);

            for (final String payload : payloads) {
                final Resource resource = resourceResolver.getResource(payload);

                if (processArgs.isTraverseTree()) {
                    // Traverse the tree
                    visitor.accept(resource);
                } else {
                    // Only execute on the provided payload
                    replicatorRunnable.run(resource);
                }
            }

            log.info("Synthetic Workflow Wrapper processed [ {} ] total payloads", count.get());
        } catch (Exception e) {
            throw new WorkflowException(e);
        }
    }

    /**
     * ProcessArgs parsed from the WF metadata map
     */
    private static class ProcessArgs {
        private ReplicationActionType replicationActionType = null;
        private ReplicationOptions replicationOptions = new ReplicationOptions();
        private boolean traverseTree = false;

        public ProcessArgs(MetaDataMap map) throws WorkflowException {
            String[] lines = StringUtils.split(map.get(WorkflowHelper.PROCESS_ARGS, ""), System.lineSeparator());
            Map<String, String> data = ParameterUtil.toMap(lines, "=");

            traverseTree = Boolean.parseBoolean(data.get(ARG_TRAVERSE_TREE));
            replicationActionType = ReplicationActionType.fromName(data.get(ARG_REPLICATION_ACTION_TYPE));
            if (replicationActionType == null) {
                throw new WorkflowException("Unable to parse the replicationActionType from the Workflow Porcess Args");
            }
            replicationOptions.setSynchronous(Boolean.parseBoolean(data.get(ARG_REPLICATION_SYNCHRONOUS)));
            replicationOptions.setSuppressVersions(Boolean.parseBoolean(data.get(ARG_REPLICATION_SUPPRESS_VERSIONS)));
        }

        public ReplicationActionType getReplicationActionType() {
            return replicationActionType;
        }

        public ReplicationOptions getReplicationOptions() {
            return replicationOptions;
        }

        public boolean isTraverseTree() {
            return traverseTree;
        }
    }
}