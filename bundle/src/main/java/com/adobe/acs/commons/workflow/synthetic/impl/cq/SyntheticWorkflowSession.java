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

package com.adobe.acs.commons.workflow.synthetic.impl.cq;

import com.adobe.acs.commons.workflow.synthetic.cq.WrappedSyntheticWorkItem;
import com.adobe.acs.commons.workflow.synthetic.impl.SyntheticWorkflowData;
import com.adobe.acs.commons.workflow.synthetic.impl.SyntheticWorkflowRunnerImpl;
import com.adobe.acs.commons.workflow.synthetic.impl.cq.exceptions.SyntheticCompleteWorkflowException;
import com.adobe.acs.commons.workflow.synthetic.impl.cq.exceptions.SyntheticRestartWorkflowException;
import com.adobe.acs.commons.workflow.synthetic.impl.cq.exceptions.SyntheticTerminateWorkflowException;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowService;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.collection.util.ResultSet;
import com.day.cq.workflow.exec.HistoryItem;
import com.day.cq.workflow.exec.Route;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.Workflow;
import com.day.cq.workflow.exec.WorkflowData;
import com.day.cq.workflow.exec.filter.WorkItemFilter;
import com.day.cq.workflow.model.WorkflowModel;
import com.day.cq.workflow.model.WorkflowModelFilter;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.VersionException;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
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
    public final WorkflowService getWorkflowService() {
        return this.workflowService;
    }

    @Override
    public final Session getSession() {
        return this.session;
    }

    @Override
    public org.apache.jackrabbit.api.security.user.Authorizable getUser() {
        if (this.session != null && this.session instanceof JackrabbitSession) {
            try {
                UserManager userManager = ((JackrabbitSession) this.session).getUserManager();
                return userManager.getAuthorizable(this.session.getUserID());
            } catch (RepositoryException e) {
                log.error("Could not obtain a Jackrabbit UserManager", e);
            }
        }

        return null;
    }

    @Override
    @SuppressWarnings("squid:S1192")
    public final void terminateWorkflow(final Workflow workflow) throws WorkflowException {
        if (workflow instanceof SyntheticWorkflow) {
            throw new SyntheticTerminateWorkflowException("Synthetic workflow [ " + workflow.getId() + " ] terminated");
        } else {
            throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
        }
    }

    @Override
    @SuppressWarnings("squid:S1192")
    public final void complete(final WorkItem workItem, final Route route) throws WorkflowException {
        if (workItem instanceof WrappedSyntheticWorkItem && workItem.getWorkflow() != null) {
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
    public final boolean evaluate(final WorkflowData workflowData, final String s) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
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
        final WorkflowSession workflowSession =
                this.workflowService.getAEMWorkflowService().getWorkflowSession(this.session);
        return workflowSession.getModel(modelId);
    }

    @Override
    public final WorkflowModel getModel(final String s, final String s2) throws WorkflowException, VersionException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public final Workflow startWorkflow(final WorkflowModel workflowModel, final WorkflowData workflowData)
            throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    /**
     * @deprecated deprecated in interface
     */
    @Deprecated
    @Override
    public final Workflow startWorkflow(final WorkflowModel workflowModel, final WorkflowData workflowData,
                                        final Dictionary<String, String> stringStringDictionary)
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
    public final List<Route> getRoutes(final WorkItem workItem) throws WorkflowException {
        log.debug("Synthetic Workflow does not support routes; Defaults to a single Synthetic Route");
        return Collections.unmodifiableList(this.routes);
    }

    @Override
    public final List<Route> getRoutes(final WorkItem workItem, final boolean b) throws WorkflowException {
        log.debug("Synthetic Workflow does not support routes; Defaults to a single Synthetic Route");
        return Collections.unmodifiableList(this.routes);
    }

    @Override
    public final List<Route> getBackRoutes(final WorkItem workItem) throws WorkflowException {
        log.debug("Synthetic Workflow does not support back routes; Defaults to a single Synthetic Route");
        return Collections.unmodifiableList(this.backRoutes);
    }

    @Override
    public final List<Route> getBackRoutes(final WorkItem workItem, final boolean b) throws WorkflowException {
        log.debug("Synthetic Workflow does not back support routes; Defaults to a single Synthetic Route");
        return Collections.unmodifiableList(this.backRoutes);
    }

    @Override
    public final WorkflowData newWorkflowData(final String payloadType, final Object payload) {
        return new SyntheticWorkflowData(payloadType, payload);
    }

    @Override
    public List<org.apache.jackrabbit.api.security.user.Authorizable> getDelegatees(WorkItem item) throws WorkflowException {
        return new ArrayList<org.apache.jackrabbit.api.security.user.Authorizable>();
    }

    @Override
    public void delegateWorkItem(WorkItem item, org.apache.jackrabbit.api.security.user.Authorizable participant) throws WorkflowException, AccessControlException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public final List<HistoryItem> getHistory(final Workflow workflow) throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public final void updateWorkflowData(final Workflow workflow, final WorkflowData workflowData) {
        if (workflow instanceof SyntheticWorkflow) {
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
}