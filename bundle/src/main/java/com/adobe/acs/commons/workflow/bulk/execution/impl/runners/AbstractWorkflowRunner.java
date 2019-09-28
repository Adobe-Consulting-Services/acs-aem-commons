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

package com.adobe.acs.commons.workflow.bulk.execution.impl.runners;

import com.adobe.acs.commons.util.QueryHelper;
import com.adobe.acs.commons.workflow.bulk.execution.BulkWorkflowRunner;
import com.adobe.acs.commons.workflow.bulk.execution.model.Status;
import com.adobe.acs.commons.workflow.bulk.execution.model.SubStatus;
import com.adobe.acs.commons.workflow.bulk.execution.model.Config;
import com.adobe.acs.commons.workflow.bulk.execution.model.Payload;
import com.adobe.acs.commons.workflow.bulk.execution.model.PayloadGroup;
import com.adobe.acs.commons.workflow.bulk.execution.model.Workspace;
import com.day.cq.commons.jcr.JcrUtil;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public abstract class AbstractWorkflowRunner implements BulkWorkflowRunner {
    private static final Logger log = LoggerFactory.getLogger(AbstractWorkflowRunner.class);
    private static final int SAVE_THRESHOLD = 1000;

    protected static final String SERVICE_NAME = "bulk-workflow-runner";
    protected static final Map<String, Object> AUTH_INFO;

    static {
        AUTH_INFO = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, (Object) SERVICE_NAME);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(QueryHelper queryHelper, Config config) throws
            PersistenceException, RepositoryException {

        // Query for all candidate resources
        final ResourceResolver resourceResolver = config.getResourceResolver();
        final List<Resource> resources = queryHelper.findResources(resourceResolver,
                config.getQueryType(),
                config.getQueryStatement(),
                config.getRelativePath());

        int total = 0;

        // Create Workspace Node
        Node workspace = JcrUtils.getOrAddNode(config.getResource().adaptTo(Node.class), Workspace.NN_WORKSPACE, Workspace.NT_UNORDERED);
        // Create the PayloadGroups Launchpad node; this simply points to the first to process
        Node currentPayloadGroup = JcrUtils.getOrCreateByPath(workspace, Workspace.NN_PAYLOADS_LAUNCHPAD, true, Workspace.NT_UNORDERED, Workspace.NT_UNORDERED, false);
        // Set the first Payload Group to be the launchpad node
        JcrUtil.setProperty(workspace, Workspace.PN_ACTIVE_PAYLOAD_GROUPS, new String[]{PayloadGroup.dereference(currentPayloadGroup.getPath())});


        // No begin populating the actual PayloadGroup nodes
        ListIterator<Resource> itr = resources.listIterator();

        while (itr.hasNext()) {
            // Increment to a new PayloadGroup as needed
            if (total % config.getBatchSize() == 0 && itr.hasNext()) {
                // payload group is complete; save...
                Node nextPayloadGroup = JcrUtils.getOrCreateByPath(workspace, Workspace.NN_PAYLOADS, true, Workspace.NT_UNORDERED, Workspace.NT_UNORDERED, false);
                JcrUtil.setProperty(currentPayloadGroup, PayloadGroup.PN_NEXT, PayloadGroup.dereference(nextPayloadGroup.getPath()));
                currentPayloadGroup = nextPayloadGroup;
            }

            // Process the payload
            Resource payload = itr.next();
            Node payloadNode = JcrUtils.getOrCreateByPath(currentPayloadGroup, Payload.NN_PAYLOAD, true, Workspace.NT_UNORDERED, Workspace.NT_UNORDERED, false);
            JcrUtil.setProperty(payloadNode, "path", Payload.dereference(payload.getPath()));
            log.debug("Created payload with search result [ {} ]", payload.getPath());

            if (++total % SAVE_THRESHOLD == 0 || !itr.hasNext()) {
                resourceResolver.commit();
            }
        } // while

        if (total > 0) {
            config.getWorkspace().getRunner().initialize(config.getWorkspace(), total);
            config.commit();

            log.info("Completed initialization of Bulk Workflow Manager");
        } else {
            throw new IllegalArgumentException("Query returned zero results.");
        }
    }


    public void initialize(Workspace workspace, int totalCount) throws PersistenceException {
        workspace.setInitialized(true);
        workspace.setTotalCount(totalCount);
        workspace.commit();
    }

    @Override
    public void start(Workspace workspace) throws PersistenceException {
        workspace.setStatus(Status.RUNNING);
        if (workspace.getStartedAt() == null) {
            workspace.setStartedAt(Calendar.getInstance());
        }
        workspace.commit();
    }

    @Override
    public void stopping(Workspace workspace) throws PersistenceException {
        workspace.setStatus(Status.RUNNING, SubStatus.STOPPING);
        workspace.commit();
    }

    @Override
    public void stop(Workspace workspace) throws PersistenceException {
        workspace.setStatus(Status.STOPPED);
        workspace.setStoppedAt(Calendar.getInstance());
        workspace.commit();
    }

    @Override
    public void stop(Workspace workspace, SubStatus subStatus) throws PersistenceException {
        if (subStatus != null) {
            workspace.setStatus(Status.STOPPED, subStatus);
        } else {
            workspace.setStatus(Status.STOPPED);
        }
        workspace.setStoppedAt(Calendar.getInstance());
        workspace.commit();
    }

    @Override
    public void stopWithError(Workspace workspace) throws PersistenceException {
        workspace.setStatus(Status.STOPPED, SubStatus.ERROR);
        workspace.setStoppedAt(Calendar.getInstance());
        workspace.commit();
    }

    @Override
    public void complete(Workspace workspace) throws PersistenceException {
        workspace.setStatus(Status.COMPLETED);
        workspace.setCompletedAt(Calendar.getInstance());
        workspace.commit();
    }

    @Override
    public void complete(Workspace workspace, Payload payload) throws Exception {
        // Remove active payload
        if (workspace != null) {
            workspace.removeActivePayload(payload);

            // Increment the complete count
            workspace.incrementCompleteCount();
        } else {
            log.warn("Unable to processing complete for payload [ {} ~> {} ]", payload.getPath(), payload.getPayloadPath());
        }
    }

    @Override
    public void run(Workspace workspace, Payload payload) {
        payload.setStatus(Status.RUNNING);
    }

    public void fail(Workspace workspace, Payload payload) throws Exception {
        payload.setStatus(Status.FAILED);

        // Remove active payload
        workspace.removeActivePayload(payload);

        // Increment the fail count
        workspace.incrementFailCount();

        // Track the failure details
        workspace.addFailure(payload);
    }

    @Override
    public abstract void forceTerminate(Workspace workspace, Payload payload) throws Exception;
}
