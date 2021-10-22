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

import com.adobe.acs.commons.workflow.synthetic.granite.WrappedSyntheticWorkflowSession;
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
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SyntheticWorkflowSession implements InvocationHandler, WrappedSyntheticWorkflowSession {
    private static final Logger log = LoggerFactory.getLogger(SyntheticWorkflowSession.class);

    private static final String UNSUPPORTED_OPERATION_MESSAGE = "Operation not supported by Synthetic Workflow";

    private final Session session;

    private final SyntheticWorkflowRunnerImpl workflowService;

    private final List<Route> routes;
    private final List<Route> backRoutes;

    private SyntheticWorkflowSession(SyntheticWorkflowRunnerImpl workflowService, Session session) {
        this.workflowService = workflowService;
        this.session = session;

        this.routes = new ArrayList<Route>();
        this.routes.add(new SyntheticRoute(false));

        this.backRoutes = new ArrayList<Route>();
        this.backRoutes.add(new SyntheticRoute(true));
    }

    public static WrappedSyntheticWorkflowSession createSyntheticWorkflowSession(SyntheticWorkflowRunnerImpl workflowService, Session session) {
        InvocationHandler handler = new SyntheticWorkflowSession(workflowService, session);
        return (WrappedSyntheticWorkflowSession) Proxy.newProxyInstance(WrappedSyntheticWorkflowSession.class.getClassLoader(), new Class[] { WrappedSyntheticWorkflowSession.class, WorkflowSession.class }, handler);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        WorkflowSession workflowSession = (WorkflowSession) proxy;
        switch (methodName) {
            case "deployModel":
                deployModel((WorkflowModel) args[0]);
                return new Object();
            case "createNewModel":
                if (args.length > 1) {
                    return createNewModel((String) args[0], (String) args[1]);
                } else {
                    return createNewModel((String) args[0]);
                }
            case "deleteModel":
                deleteModel((String) args[0]);
                return new Object();
            case "getModels":
                if (args.length == 1) {
                    return getModels((WorkflowModelFilter) args[0]);
                } else if (args.length == 2) {
                    return getModels((Long) args[0], (Long) args[1]);
                } else if (args.length == 3) {
                    return getModels((Long) args[0], (Long) args[1], (WorkflowModelFilter) args[2]);
                }
                return getModels();
            case "getModel":
                if (args.length > 1) {
                    return getModel((String) args[0], (String) args[1]);
                }
                return getModel((String) args[0]);
            case "startWorkflow":
                if (args.length == 3) {
                    return startWorkflow((WorkflowModel) args[0], (WorkflowData) args[1], (Map<String, Object>) args[2]);
                }
                return startWorkflow((WorkflowModel) args[0], (WorkflowData) args[1]);
            case "terminateWorkflow":
                terminateWorkflow((Workflow) args[0]);
                return new Object();
            case "resumeWorkflow":
                resumeWorkflow((Workflow) args[0]);
                return new Object();
            case "suspendWorkflow":
                suspendWorkflow((Workflow) args[0]);
                return new Object();
            case "getActiveWorkItems":
                if (args.length == 2) {
                    return getActiveWorkItems((Long) args[0], (Long) args[1]);
                } else if (args.length == 3) {
                    return getActiveWorkItems((Long) args[0], (Long) args[1], (WorkItemFilter) args[2]);
                }
                return getActiveWorkItems();
            case "getActiveInboxItems":
                if (args.length == 4) {
                    return getActiveInboxItems((Long) args[0], (Long) args[1], (String) args[2], (InboxItemFilter) args[3]);
                }
                return getActiveInboxItems((Long) args[0], (Long) args[1], (InboxItemFilter) args[2]);
            case "getAllWorkItems":
                if (args.length > 1) {
                    return getAllWorkItems((Long) args[0], (Long) args[1]);
                }
                return getAllWorkItems();
            case "getWorkItem":
                return getWorkItem((String) args[0]);
            case "getWorkflows":
                if (args.length == 3) {
                    return getWorkflows((String[]) args[0], (Long) args[1], (Long) args[2]);
                }
                return getWorkflows((String[]) args[0]);
            case "getAllWorkflows":
                return getAllWorkflows();
            case "getWorkflow":
                return getWorkflow((String) args[0]);
            case "complete":
                complete((WorkItem) args[0], (Route) args[1]);
                return new Object();
            case "getRoutes":
                return getRoutes((WorkItem) args[0], (Boolean) args[1]);
            case "getBackRoutes":
                return getBackRoutes((WorkItem) args[0], (Boolean) args[1]);
            case "newWorkflowData":
                return newWorkflowData((String) args[0], args[1]);
            case "getDelegates":
                return getDelegates((WorkItem) args[0]);
            case "delegateWorkItem":
                delegateWorkItem((WorkItem) args[0], (Participant) args[1]);
                return new Object();
            case "getHistory":
                return getHistory((Workflow) args[0]);
            case "updateWorkflowData":
                updateWorkflowData((Workflow) args[0], (WorkflowData) args[1]);
                return new Object();
            case "logout":
                logout();
                return new Object();
            case "isSuperuser":
                return isSuperuser();
            case "restartWorkflow":
                restartWorkflow((Workflow) args[0]);
                return new Object();
            default:
                log.error("SYNTHETICWORKFLOW SESSION >> NO IMPLEMENTATION FOR {}", methodName);
                throw new UnsupportedOperationException();
        }
    }

    @SuppressWarnings("squid:S1192")
    public final void terminateWorkflow(final Workflow workflow) throws WorkflowException {
        if (workflow instanceof  SyntheticWorkflow) {
            throw new SyntheticTerminateWorkflowException("Synthetic workflow [ " + workflow.getId() + " ] terminated");
        } else {
            throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
        }
    }

    @SuppressWarnings("squid:S1192")
    public final void complete(final WorkItem workItem, final Route route) throws WorkflowException {
        if (workItem instanceof SyntheticWorkItem) {
            throw new SyntheticCompleteWorkflowException("Synthetic workflow [ "
                    + workItem.getWorkflow().getId() + " : " + workItem.getId() + " ] completed");
        } else {
            throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
        }
    }

    @SuppressWarnings("squid:S1192")
    public final void restartWorkflow(final Workflow workflow) throws WorkflowException {
        if (workflow instanceof SyntheticWorkflow) {
            throw new SyntheticRestartWorkflowException("Synthetic workflow [ " + workflow.getId() + " ] restarted");
       } else {
            throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
        }
    }

    public final boolean isSuperuser() {
        return true;
    }

    public final void deployModel(final WorkflowModel workflowModel) throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    public final WorkflowModel createNewModel(final String s) throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    public final WorkflowModel createNewModel(final String s, final String s2) throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    public final void deleteModel(final String s) throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    public final WorkflowModel[] getModels() throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    public final WorkflowModel[] getModels(final WorkflowModelFilter workflowModelFilter) throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    public final ResultSet<WorkflowModel> getModels(final long l, final long l2) throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    public final ResultSet<WorkflowModel> getModels(final long l, final long l2,
                                                    final WorkflowModelFilter workflowModelFilter)
            throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    public final WorkflowModel getModel(final String modelId) throws WorkflowException {
        final WorkflowSession workflowSession;
        try {
            workflowSession = this.workflowService.getResourceResolver(this.session).adaptTo(WorkflowSession.class);
            return workflowSession.getModel(modelId);
        } catch (LoginException e) {
            throw new WorkflowException("Could not obtain a Granite Workflow Session");
        }
    }

    public final WorkflowModel getModel(final String s, final String s2) throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    public final Workflow startWorkflow(final WorkflowModel workflowModel, final WorkflowData workflowData)
            throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    public final Workflow startWorkflow(final WorkflowModel workflowModel, final WorkflowData workflowData,
                                        final Map<String, Object> stringObjectMap) throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    public final void resumeWorkflow(final Workflow workflow) throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    public final void suspendWorkflow(final Workflow workflow) throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    public final WorkItem[] getActiveWorkItems() throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    public final ResultSet<WorkItem> getActiveWorkItems(final long l, final long l2) throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    public final ResultSet<WorkItem> getActiveWorkItems(final long l, final long l2,
                                                        final WorkItemFilter workItemFilter)
            throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    public ResultSet<InboxItem> getActiveInboxItems(long l, long l1, InboxItemFilter inboxItemFilter) throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    public ResultSet<InboxItem> getActiveInboxItems(long l, long l1, String s, InboxItemFilter inboxItemFilter) throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    public final WorkItem[] getAllWorkItems() throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    public final ResultSet<WorkItem> getAllWorkItems(final long l, final long l2) throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    public final WorkItem getWorkItem(final String s) throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    public final Workflow[] getWorkflows(final String[] strings) throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    public final ResultSet<Workflow> getWorkflows(final String[] strings, final long l, final long l2)
            throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    public final Workflow[] getAllWorkflows() throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    public final Workflow getWorkflow(final String s) throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    public final List<Route> getRoutes(final WorkItem workItem, final boolean b) throws WorkflowException {
        log.debug("Synthetic Workflow does not support routes; Defaults to a single Synthetic Route");
        return this.routes;
    }

    public final List<Route> getBackRoutes(final WorkItem workItem, final boolean b) throws WorkflowException {
        log.debug("Synthetic Workflow does not back support routes; Defaults to a single Synthetic Route");
        return this.backRoutes;
    }

    public final WorkflowData newWorkflowData(final String payloadType, final Object payload) {
        return new SyntheticWorkflowData(payloadType, payload);
    }

    public Iterator<Participant> getDelegates(WorkItem workItem) throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    public void delegateWorkItem(WorkItem workItem, Participant participant) throws WorkflowException, AccessControlException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    public final List<HistoryItem> getHistory(final Workflow workflow) throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    public final void updateWorkflowData(final Workflow workflow, final WorkflowData workflowData) {
        if (workflow instanceof com.adobe.acs.commons.workflow.synthetic.impl.granite.SyntheticWorkflow) {
            final SyntheticWorkflow syntheticWorkflow = (SyntheticWorkflow) workflow;
            syntheticWorkflow.setWorkflowData(workflowData);
        } else {
            throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
        }
    }

    public final void logout() {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    public Object adaptTo(Class<?> type) {
        if (Session.class == type) {
            return session;
        }

        return null;
    }
}
