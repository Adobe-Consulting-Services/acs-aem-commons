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
import com.adobe.granite.workflow.exec.Route;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.Workflow;
import com.adobe.granite.workflow.exec.WorkflowData;
import com.adobe.granite.workflow.model.WorkflowModel;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SyntheticWorkflowSession implements InvocationHandler {
    private static final Logger log = LoggerFactory.getLogger(SyntheticWorkflowSession.class);

    private static final String UNSUPPORTED_OPERATION_MESSAGE = "SYNTHETICWORKFLOW SESSION >> NO IMPLEMENTATION FOR ";

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

    public static SyntheticWorkflowSession createSyntheticWorkflowSession(SyntheticWorkflowRunnerImpl workflowService, Session session) {
        return new SyntheticWorkflowSession(workflowService, session);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        switch (methodName) {
            case "getModel":
                return getModel(args);
            case "terminateWorkflow":
                terminateWorkflow((Workflow) args[0]);
                return new Object();
            case "complete":
                complete((WorkItem) args[0], (Route) args[1]);
                return new Object();
            case "getRoutes":
                return getRoutes();
            case "getBackRoutes":
                return getBackRoutes();
            case "newWorkflowData":
                return newWorkflowData((String) args[0], args[1]);
            case "updateWorkflowData":
                updateWorkflowData((Workflow) args[0], (WorkflowData) args[1]);
                return new Object();
            case "isSuperuser":
                return isSuperuser();
            case "restartWorkflow":
                restartWorkflow((Workflow) args[0]);
                return new Object();
            default:
                throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE + methodName);
        }
    }

    @SuppressWarnings("squid:S1192")
    public final void terminateWorkflow(final Workflow workflow) throws WorkflowException {
        if (workflow instanceof  SyntheticWorkflow) {
            throw new SyntheticTerminateWorkflowException("Synthetic workflow [ " + workflow.getId() + " ] terminated");
        } else {
            throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE + "terminateWorkflow that is not a SyntheticWorkflow");
        }
    }

    @SuppressWarnings("squid:S1192")
    public final void complete(final WorkItem workItem, final Route route) throws WorkflowException {
        if (workItem instanceof SyntheticWorkItem) {
            throw new SyntheticCompleteWorkflowException("Synthetic workflow [ "
                    + workItem.getWorkflow().getId() + " : " + workItem.getId() + " ] completed");
        } else {
            throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE + "complete that does not contain a SyntheticWorkItem");
        }
    }

    @SuppressWarnings("squid:S1192")
    public final void restartWorkflow(final Workflow workflow) throws WorkflowException {
        if (workflow instanceof SyntheticWorkflow) {
            throw new SyntheticRestartWorkflowException("Synthetic workflow [ " + workflow.getId() + " ] restarted");
       } else {
            throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE + "restartWorkflow that does not contain a SyntheticWorkflow");
        }
    }

    public final boolean isSuperuser() {
        return true;
    }

    public final Object getModel(Object[] args) throws WorkflowException {
        if (args.length > 1) {
            throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE + "getModel with multiple arguments");
        }
        return getModel((String) args[0]);
    }

    private WorkflowModel getModel(final String modelId) throws WorkflowException {
        final WorkflowSession workflowSession;
        try {
            workflowSession = this.workflowService.getResourceResolver(this.session).adaptTo(WorkflowSession.class);
            return workflowSession.getModel(modelId);
        } catch (LoginException e) {
            throw new WorkflowException("Could not obtain a Granite Workflow Session");
        }
    }

    public final List<Route> getRoutes() throws WorkflowException {
        log.debug("Synthetic Workflow does not support routes; Defaults to a single Synthetic Route");
        return Collections.unmodifiableList(this.routes);
    }

    public final List<Route> getBackRoutes() throws WorkflowException {
        log.debug("Synthetic Workflow does not back support routes; Defaults to a single Synthetic Route");
        return Collections.unmodifiableList(this.backRoutes);
    }

    public final WorkflowData newWorkflowData(final String payloadType, final Object payload) {
        return new SyntheticWorkflowData(payloadType, payload);
    }

    public final void updateWorkflowData(final Workflow workflow, final WorkflowData workflowData) {
        if (workflow instanceof com.adobe.acs.commons.workflow.synthetic.impl.granite.SyntheticWorkflow) {
            final SyntheticWorkflow syntheticWorkflow = (SyntheticWorkflow) workflow;
            syntheticWorkflow.setWorkflowData(workflowData);
        } else {
            throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE + "updateWorkflowData that does not contain a granite SyntheticWorkflow");
        }
    }

    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        if (ResourceResolver.class.equals(type)) {
            if (this.session != null) {
                try {
                    return (AdapterType) this.workflowService.getResourceResolver(this.session);
                } catch (LoginException e) {
                    log.error("Failed to adapt Synthetic Granite WorkflowSession to ResourceResolver", e);
                }
            }
        } else if (Session.class.equals(type)) {
            return (AdapterType) this.session;
        }

        return null;
    }
}
