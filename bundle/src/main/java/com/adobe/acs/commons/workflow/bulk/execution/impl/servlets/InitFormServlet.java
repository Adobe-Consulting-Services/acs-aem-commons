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

package com.adobe.acs.commons.workflow.bulk.execution.impl.servlets;

import com.adobe.acs.commons.workflow.bulk.execution.BulkWorkflowEngine;
import com.adobe.acs.commons.workflow.bulk.execution.impl.runners.AEMWorkflowRunnerImpl;
import com.adobe.acs.commons.workflow.bulk.execution.impl.runners.FastActionManagerRunnerImpl;
import com.adobe.acs.commons.workflow.bulk.execution.impl.runners.SyntheticWorkflowRunnerImpl;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowService;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.model.WorkflowModel;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import javax.servlet.ServletException;
import java.io.IOException;

/**
 * ACS AEM Commons - Bulk Workflow Manager - Init Form Servlet
 */
@SuppressWarnings("serial")
@SlingServlet(
        methods = {"GET"},
        resourceTypes = {BulkWorkflowEngine.SLING_RESOURCE_TYPE},
        selectors = {"init-form"},
        extensions = {"json"}
)
public class InitFormServlet extends SlingAllMethodsServlet {
    private static final Logger log = LoggerFactory.getLogger(InitFormServlet.class);

    @Reference
    private WorkflowService workflowService;

    @Override
    protected final void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        final JSONObject json = new JSONObject();

        // Runners
        try {
            json.accumulate("runnerTypes", new JSONObject().put("label", "AEM Workflow").put("value",
                    AEMWorkflowRunnerImpl.class.getName()));
            json.accumulate("runnerTypes", new JSONObject().put("label", "Synthetic Workflow (Single-threaded)").put("value",
                    SyntheticWorkflowRunnerImpl.class.getName()));
            json.accumulate("runnerTypes", new JSONObject().put("label", "Synthetic Workflow (Multi-threaded)").put("value",
                    FastActionManagerRunnerImpl.class.getName()));
        } catch (JSONException e) {
            log.error("Could not create JSON for Bulk Workflow Runner options");
            throw new ServletException(e);
        }

        // Query Types
        try {
            json.accumulate("queryTypes", new JSONObject().put("label", "QueryBuilder").put("value", "queryBuilder"));
            json.accumulate("queryTypes", new JSONObject().put("label", "List").put("value", "list"));
            json.accumulate("queryTypes", new JSONObject().put("label", "xPath").put("value", "xpath"));
            json.accumulate("queryTypes", new JSONObject().put("label", "JCR-SQL2").put("value", "JCR-SQL2"));
            json.accumulate("queryTypes", new JSONObject().put("label", "JCR-SQL").put("value", "JCR-SQL"));
        } catch (JSONException e) {
            log.error("Could not create JSON for QueryType options", e);
            throw new ServletException(e);
        }

        // User Event Data
        try {
            json.accumulate("userEventData", new JSONObject().put("label", "Custom user-event-data").put("value", ""));
            json.accumulate("userEventData", new JSONObject().put("label", "changedByWorkflowProcess").put("value", "changedByWorkflowProcess"));
            json.accumulate("userEventData", new JSONObject().put("label", "acs-aem-commons.bulk-workflow-manager").put("value", "acs-aem-commons.bulk-workflow-manager"));

        } catch (JSONException e) {
            log.error("Could not create JSON for userEventData options", e);
            throw new ServletException(e);
        }

        // Workflow Models
        final WorkflowSession workflowSession = workflowService.getWorkflowSession(
                request.getResourceResolver().adaptTo(Session.class));

        try {
            final WorkflowModel[] workflowModels = workflowSession.getModels();

            for (final WorkflowModel workflowModel : workflowModels) {
                JSONObject jsonWorkflow = new JSONObject();
                try {
                    boolean transientWorkflow = isTransient(request.getResourceResolver(), workflowModel.getId());
                    String workflowLabel = workflowModel.getTitle();
                    if (transientWorkflow) {
                        workflowLabel += " ( Transient )";
                    }
                    jsonWorkflow.put("label", workflowLabel);
                    jsonWorkflow.put("value", workflowModel.getId());
                    jsonWorkflow.put("transient", transientWorkflow);
                    json.accumulate("workflowModels", jsonWorkflow);
                } catch (JSONException e) {
                    log.error("Could not add workflow [ {} - {} ] to Workflow Models drop-down JSON object",
                            workflowModel.getTitle(), workflowModel.getId());
                    throw new ServletException(e);
                }
            }

            response.getWriter().write(json.toString());
        } catch (WorkflowException e) {
            log.error("Could not create workflow model drop-down.", e);

            JSONErrorUtil.sendJSONError(response, SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Could not collect workflows",
                    e.getMessage());
        }
    }

    protected boolean isTransient(ResourceResolver resourceResolver, String workflowModelId) {
        Resource resource = resourceResolver.getResource(workflowModelId).getParent();
        return resource.getValueMap().get("transient", false);
    }

}
