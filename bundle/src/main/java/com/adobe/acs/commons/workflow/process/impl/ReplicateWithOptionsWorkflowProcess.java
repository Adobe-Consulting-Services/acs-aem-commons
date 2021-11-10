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
import com.adobe.acs.commons.replication.AgentIdsAgentFilter;
import com.adobe.acs.commons.replication.BrandPortalAgentFilter;
import com.adobe.acs.commons.util.ParameterUtil;
import com.adobe.acs.commons.util.WorkflowHelper;
import com.adobe.acs.commons.util.visitors.ContentVisitor;
import com.adobe.acs.commons.util.visitors.ResourceRunnable;
import com.adobe.acs.commons.workflow.WorkflowPackageManager;
import org.apache.jackrabbit.JcrConstants;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationOptions;
import com.day.cq.replication.Replicator;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import java.util.ArrayList;
import java.util.Arrays;
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
    private static final String ARG_THROTTLE = "throttle";
    private static final String ARG_AGENTS = "agents";

    private static final String BRAND_PORTAL_AGENTS = "BRAND_PORTAL_AGENTS";

    // comma delimited list of agent names to use for dispatcher flush, need to have these agents enabled before running workflow
    private static final String ARG_AGENTS_DISPATCHER = "agentsDispatcher";
    // specify to flush dispatcher for the payload
    private static final String ARG_FLUSH_DISPATCHER = "flushDispatcher";
    // flush parent resource type, find ancestor/parent with this resource type and use it for dispatcher flush.
    private static final String ARG_FLUSH_PARENT_RESOURCE_TYPE =
        "flushParentResourceType";
    // specify which sub child item to replicate
    private static final String ARG_REPLICATE_CHILD_PATH = "replicateChildPath";

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
        final long start = System.currentTimeMillis();

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
                    // if we are throttling
                    if (processArgs.isThrottle()) {
                        throttledTaskRunner.waitForLowCpuAndLowMemory();
                    }

                    // get the path for child to replicate, empty means replicate current payload path
                    String replicateChildPath = processArgs.getReplicateChildPath();
                    // use current payload resource as default for replication
                    Resource replicateResource = resource;
                    // allow skipping replicaiton of current payload
                    Boolean skip = false;
                    
                    // do we need to replicate child path
                    if (processArgs.isReplicateChild()) {
                        if (!replicateChildPath.startsWith("/")) {
                            replicateChildPath = "/" + replicateChildPath;
                        }
                        
                        // get the child path
                        Resource childResource = resource.getResourceResolver().getResource(resource.getPath() + replicateChildPath);
                        
                        // if child path does not exist, log a warning and skip processing payload
                        if (childResource != null && !ResourceUtil.isNonExistingResource(childResource)) {
                            // when child resource exist set it as the replication resource, this is what need replication.
                            replicateResource = childResource;
                        } else {
                            log.info("Could not find child path [{}] in page {}, skipping.", replicateChildPath, replicateResource.getPath());
                            skip = true;
                        }
                    }

                    // if not skipping do main processing
                    if (!skip) {
                        // replicate payload resource
                        replicator.replicate(
                            resource.getResourceResolver().adaptTo(Session.class),
                            processArgs.getReplicationActionType(),
                            replicateResource.getPath(),
                            processArgs.getReplicationOptions(replicateResource)
                        );

                        // do we need to flush dispatcher
                        if (processArgs.isFlushDispatcher()) {
                            // get parent resource type
                            String flushParentResourceType = processArgs.getFlushParentResourceType();
                            // get resource path for the current replication resource
                            String flushPath = StringUtils.defaultString(replicateResource.getPath(), "");

                            // if parent resource type has been set
                            if (StringUtils.isNoneEmpty(flushParentResourceType)) {
                                // search for the parent
                                Resource parentResource = processArgs.getContainingResource(replicateResource.getResourceResolver(), replicateResource, flushParentResourceType);
                                // if we found the parent resource
                                if (parentResource != null && !ResourceUtil.isNonExistingResource(parentResource)) {
                                    // update flush path to match parent
                                    flushPath = parentResource.getPath();
                                }
                            }

                            // if we have a valid flush path
                            if (StringUtils.isNoneEmpty(flushPath)) {
                                // send the replicate event to the flush agent
                                replicator.replicate(
                                    replicateResource.getResourceResolver().adaptTo(Session.class),
                                    processArgs.getReplicationActionType(),
                                    flushPath,
                                    processArgs.getReplicationOptionsDispatcher(replicateResource)
                                );
                            }
                        }
                    }
                    // tick the counter
                    count.incrementAndGet();
                }
            };

            final ContentVisitor visitor = new ContentVisitor(replicatorRunnable);

            // for all payload paths send them to content visitor
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

            log.info("Replicate with Options processed [ {} ] total payloads in {} ms", count.get(), System.currentTimeMillis() - start);
        } catch (Exception e) {
            throw new WorkflowException(e);
        }
    }

    /**
     * ProcessArgs parsed from the WF metadata map
     */
    protected static class ProcessArgs {
        private ReplicationActionType replicationActionType;
        private ReplicationOptions replicationOptions = new ReplicationOptions();
        private boolean traverseTree;
        private boolean throttle;
        private List<String> agents;

        // specify which sub child item to replicate
        private String replicateChildPath;
        // specify to flush dispatcher for the path
        private boolean flushDispatcher;
        //flush parent resource type, find ancestor/parent with this resource type and use it for dispatcher flush.
        private String flushParentResourceType;
        // comma delimited list of agent names to use for dispatcher flush, need to have these agents enabled before running workflow
        private List<String> agentsDispatcher;

        public ProcessArgs(MetaDataMap map) throws WorkflowException {
            final String[] lines = StringUtils.split(map.get(WorkflowHelper.PROCESS_ARGS, ""), System.lineSeparator());
            final Map<String, String> data = ParameterUtil.toMap(lines, "=");

            throttle = Boolean.parseBoolean(data.get(ARG_THROTTLE));
            traverseTree = Boolean.parseBoolean(data.get(ARG_TRAVERSE_TREE));
            replicationActionType = ReplicationActionType.fromName(data.get(ARG_REPLICATION_ACTION_TYPE));
            if (replicationActionType == null) {
                throw new WorkflowException("Unable to parse the replicationActionType from the Workflow Process Args");
            }
            replicationOptions.setSynchronous(Boolean.parseBoolean(data.get(ARG_REPLICATION_SYNCHRONOUS)));
            replicationOptions.setSuppressVersions(Boolean.parseBoolean(data.get(ARG_REPLICATION_SUPPRESS_VERSIONS)));

            agents = Arrays.asList(StringUtils.split(data.get(ARG_AGENTS), ","));

            replicateChildPath = StringUtils.defaultIfEmpty(data.get(ARG_REPLICATE_CHILD_PATH), "");
            flushDispatcher = Boolean.parseBoolean(data.get(ARG_FLUSH_DISPATCHER));
            flushParentResourceType = StringUtils.defaultIfEmpty(data.get(ARG_FLUSH_PARENT_RESOURCE_TYPE),"");
            agentsDispatcher = Arrays.asList(StringUtils.split(StringUtils.defaultString(data.get(ARG_AGENTS_DISPATCHER), ""),","));
        }

        public ReplicationActionType getReplicationActionType() {
            return replicationActionType;
        }

        public ReplicationOptions getReplicationOptions(Resource content) {
            if (agents.size() == 1 && BRAND_PORTAL_AGENTS.equals(agents.get(0))) {
                replicationOptions.setFilter(new BrandPortalAgentFilter(content));
            } else {
                replicationOptions.setFilter(new AgentIdsAgentFilter(agents));
            }

            return replicationOptions;
        }

        public boolean isTraverseTree() {
            return traverseTree;
        }

        public boolean isThrottle() {
            return throttle;
        }

        public ReplicationOptions getReplicationOptionsDispatcher(Resource content) {
            ReplicationOptions replicationOptionsDispatcher = new ReplicationOptions();
            replicationOptionsDispatcher.setSynchronous(
                replicationOptionsDispatcher.isSynchronous()
            );
            replicationOptionsDispatcher.setSuppressVersions(
                replicationOptionsDispatcher.isSuppressVersions()
            );
            replicationOptionsDispatcher.setFilter(
                new AgentIdsAgentFilter(agentsDispatcher)
            );
            return replicationOptionsDispatcher;
        }

        public boolean isFlushDispatcher() {
            return flushDispatcher;
        }

        public String getFlushParentResourceType() {
            return flushParentResourceType;
        }

        public Resource getContainingResource(ResourceResolver resourceResolver,Resource child,String parentResourceType) {
            Resource parent = null;
            if (child == null || StringUtils.isEmpty(parentResourceType)) {
                return parent;
            }

            String path = child.getPath();
            int idx = path.lastIndexOf("/" + JcrConstants.JCR_CONTENT);
            if (idx >= 0) {
                path = path.substring(0, idx);
            }

            //walk the tree up until we find the parent resource type.
            parent = resourceResolver.getResource(path);
            while (parent != null && !ResourceUtil.isNonExistingResource(parent)) {
                if (parent.isResourceType(parentResourceType)) {
                return parent;
                }
                parent = parent.getParent();
            }
            return parent;
        }

        public boolean isReplicateChild() {
            return StringUtils.isNotEmpty(replicateChildPath);
        }

        public String getReplicateChildPath() {
            return replicateChildPath;
        }
    }
}
