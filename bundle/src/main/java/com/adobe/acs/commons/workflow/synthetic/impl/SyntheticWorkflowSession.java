/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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

package com.adobe.acs.commons.workflow.synthetic.impl;

import com.adobe.acs.commons.workflow.synthetic.impl.exceptions.SyntheticCompleteWorkflowException;
import com.adobe.acs.commons.workflow.synthetic.impl.exceptions.SyntheticRestartWorkflowException;
import com.adobe.acs.commons.workflow.synthetic.impl.exceptions.SyntheticTerminateWorkflowException;
import com.day.cq.security.Authorizable;
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

import javax.jcr.Session;
import javax.jcr.version.VersionException;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;

public class SyntheticWorkflowSession implements WorkflowSession {
    private static final String UNSUPPORTED_OPERATION_MESSAGE = "Operation not supported by Synthetic Workflow";

    private final Session session;
    private final SyntheticWorkflowRunnerImpl workflowService;

    public SyntheticWorkflowSession(SyntheticWorkflowRunnerImpl workflowService, Session session) {
        this.workflowService = workflowService;
        this.session = session;
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
    public final void terminateWorkflow(final Workflow workflow) throws WorkflowException {
        throw new SyntheticTerminateWorkflowException("Synthetic workflow terminated");
    }

    @Override
    public final void complete(final WorkItem workItem, final Route route) throws WorkflowException {
        throw new SyntheticCompleteWorkflowException("Synthetic workflow completed");
    }

    @Override
    public final void restartWorkflow(final Workflow workflow) throws WorkflowException {
        throw new SyntheticRestartWorkflowException("Synthetic workflow restarted");
    }

    @Override
    public final boolean isSuperuser() {
        return true;
    }

    @Deprecated
    @Override
    public final Authorizable getUser() {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
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
    public final WorkflowModel getModel(final String s) throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
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
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public final List<Route> getRoutes(final WorkItem workItem, final boolean b) throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public final List<Route> getBackRoutes(final WorkItem workItem) throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public final List<Route> getBackRoutes(final WorkItem workItem, final boolean b) throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public final WorkflowData newWorkflowData(final String s, final Object o) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Deprecated
    @Override
    public final List<Authorizable> getDelegatees(final WorkItem workItem) throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Deprecated
    @Override
    public final void delegateWorkItem(final WorkItem workItem, final Authorizable authorizable)
            throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public final List<HistoryItem> getHistory(final Workflow workflow) throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public final void updateWorkflowData(final Workflow workflow, final WorkflowData workflowData) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public final void logout() {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

}
