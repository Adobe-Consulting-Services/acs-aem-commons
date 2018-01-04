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

package com.adobe.acs.commons.workflow.synthetic.impl.granite;

import com.adobe.acs.commons.workflow.synthetic.impl.SyntheticWorkflowData;
import com.adobe.acs.commons.workflow.synthetic.impl.SyntheticWorkflowRunnerImpl;
import com.adobe.acs.commons.workflow.synthetic.impl.granite.exceptions.SyntheticCompleteWorkflowException;
import com.adobe.acs.commons.workflow.synthetic.impl.granite.exceptions.SyntheticRestartWorkflowException;
import com.adobe.acs.commons.workflow.synthetic.impl.granite.exceptions.SyntheticTerminateWorkflowException;
import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.collection.util.ResultSet;
import com.adobe.granite.workflow.exec.HistoryItem;
import com.adobe.granite.workflow.exec.InboxItem;
import com.adobe.granite.workflow.exec.Participant;
import com.adobe.granite.workflow.exec.Route;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.Workflow;
import com.adobe.granite.workflow.exec.WorkflowData;
import com.adobe.granite.workflow.exec.filter.InboxItemFilter;
import com.adobe.granite.workflow.exec.filter.WorkItemFilter;
import com.adobe.granite.workflow.model.WorkflowModel;
import com.adobe.granite.workflow.model.WorkflowModelFilter;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SyntheticWorkflowSession implements WorkflowSession {
    private static final Logger log = LoggerFactory.getLogger(SyntheticWorkflowSession.class);

    private static final String UNSUPPORTED_OPERATION_MESSAGE = "Operation not supported by Synthetic Workflow";

    private final Session session;

    private final SyntheticWorkflowRunnerImpl workflowService;

    private final List<Route> routes;
    private final List<Route> backRoutes;

    public SyntheticWorkflowSession(SyntheticWorkflowRunnerImpl workflowService, Session session) {
        this.workflowService = workflowService;
        this.session = session;

        this.routes = new ArrayList<Route>();
        this.routes.add(new SyntheticRoute(false));

        this.backRoutes = new ArrayList<Route>();
        this.backRoutes.add(new SyntheticRoute(true));
    }

    @Override
    @SuppressWarnings("squid:S1192")
    public final void terminateWorkflow(final Workflow workflow) throws WorkflowException {
        if (workflow instanceof  SyntheticWorkflow) {
            throw new SyntheticTerminateWorkflowException("Synthetic workflow [ " + workflow.getId() + " ] terminated");
        } else {
            throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
        }
    }

    @Override
    @SuppressWarnings("squid:S1192")
    public final void complete(final WorkItem workItem, final Route route) throws WorkflowException {
        if (workItem instanceof SyntheticWorkItem) {
            throw new SyntheticCompleteWorkflowException("Synthetic workflow [ "
                    + workItem.getWorkflow().getId() + " : " + workItem.getId() + " ] completed");
        } else {
            throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
        }
    }

    @Override
    @SuppressWarnings("squid:S1192")
    public final void restartWorkflow(final Workflow workflow) throws WorkflowException {
        if (workflow instanceof SyntheticWorkflow) {
            throw new SyntheticRestartWorkflowException("Synthetic workflow [ " + workflow.getId() + " ] restarted");
       } else {
            throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
        }
    }

    @Override
    public final boolean isSuperuser() {
        return true;
    }

    @Override
    public final void deployModel(final WorkflowModel workflowModel) throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public final WorkflowModel createNewModel(final String s) throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public final WorkflowModel createNewModel(final String s, final String s2) throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public final void deleteModel(final String s) throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public final WorkflowModel[] getModels() throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public final WorkflowModel[] getModels(final WorkflowModelFilter workflowModelFilter) throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public final ResultSet<WorkflowModel> getModels(final long l, final long l2) throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public final ResultSet<WorkflowModel> getModels(final long l, final long l2,
                                                    final WorkflowModelFilter workflowModelFilter)
            throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public final WorkflowModel getModel(final String modelId) throws WorkflowException {
        final WorkflowSession workflowSession;
        try {
            workflowSession = this.workflowService.getResourceResolver(this.session).adaptTo(WorkflowSession.class);
            return workflowSession.getModel(modelId);
        } catch (LoginException e) {
            throw new WorkflowException("Could not obtain a Granite Workflow Session");
        }
    }

    @Override
    public final WorkflowModel getModel(final String s, final String s2) throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public final Workflow startWorkflow(final WorkflowModel workflowModel, final WorkflowData workflowData)
            throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public final Workflow startWorkflow(final WorkflowModel workflowModel, final WorkflowData workflowData,
                                        final Map<String, Object> stringObjectMap) throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public final void resumeWorkflow(final Workflow workflow) throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public final void suspendWorkflow(final Workflow workflow) throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public final WorkItem[] getActiveWorkItems() throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public final ResultSet<WorkItem> getActiveWorkItems(final long l, final long l2) throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public final ResultSet<WorkItem> getActiveWorkItems(final long l, final long l2,
                                                        final WorkItemFilter workItemFilter)
            throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public ResultSet<InboxItem> getActiveInboxItems(long l, long l1, InboxItemFilter inboxItemFilter) throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public ResultSet<InboxItem> getActiveInboxItems(long l, long l1, String s, InboxItemFilter inboxItemFilter) throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public final WorkItem[] getAllWorkItems() throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public final ResultSet<WorkItem> getAllWorkItems(final long l, final long l2) throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public final WorkItem getWorkItem(final String s) throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public final Workflow[] getWorkflows(final String[] strings) throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public final ResultSet<Workflow> getWorkflows(final String[] strings, final long l, final long l2)
            throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public final Workflow[] getAllWorkflows() throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public final Workflow getWorkflow(final String s) throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public final List<Route> getRoutes(final WorkItem workItem, final boolean b) throws WorkflowException {
        log.debug("Synthetic Workflow does not support routes; Defaults to a single Synthetic Route");
        return this.routes;
    }

    @Override
    public final List<Route> getBackRoutes(final WorkItem workItem, final boolean b) throws WorkflowException {
        log.debug("Synthetic Workflow does not back support routes; Defaults to a single Synthetic Route");
        return this.backRoutes;
    }

    @Override
    public final WorkflowData newWorkflowData(final String payloadType, final Object payload) {
        return new SyntheticWorkflowData(payloadType, payload);
    }

    @Override
    public Iterator<Participant> getDelegates(WorkItem workItem) throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public void delegateWorkItem(WorkItem workItem, Participant participant) throws WorkflowException, AccessControlException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public final List<HistoryItem> getHistory(final Workflow workflow) throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public final void updateWorkflowData(final Workflow workflow, final WorkflowData workflowData) {
        if (workflow instanceof com.adobe.acs.commons.workflow.synthetic.impl.granite.SyntheticWorkflow) {
            final SyntheticWorkflow syntheticWorkflow = (SyntheticWorkflow) workflow;
            syntheticWorkflow.setWorkflowData(workflowData);
        } else {
            throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
        }
    }

    @Override
    public final void logout() {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        if (ResourceResolver.class == type) {
            if (this.session != null) {
                try {
                    return (AdapterType) workflowService.getResourceResolver(session);
                } catch (LoginException e) {
                    log.error("Failed to adapt Synthetic Granite WorkflowSession to ResourceResolver", e);
                }
            }
        } else if (Session.class == type) {
            return (AdapterType) this.session;
        }

        return null;
    }
}
