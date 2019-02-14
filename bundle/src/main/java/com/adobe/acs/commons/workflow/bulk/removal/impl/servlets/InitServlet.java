/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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

package com.adobe.acs.commons.workflow.bulk.removal.impl.servlets;

import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowService;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.model.WorkflowModel;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;

import javax.jcr.Session;
import javax.servlet.ServletException;

import java.io.IOException;
import java.util.Arrays;

/**
 * ACS AEM Commons - Workflow Instance Remover - Init Servlet
 */
@SuppressWarnings("serial")
@SlingServlet(
        methods = { "GET" },
        resourceTypes = { "acs-commons/components/utilities/workflow-remover" },
        selectors = { "init" },
        extensions = { "json" }
)
public class InitServlet extends SlingSafeMethodsServlet {

    private static final String[] WORKFLOW_STATUSES = new String[]{"COMPLETED", "ABORTED", "RUNNING",
            "SUSPENDED", "STALE"};

    @Reference
    private WorkflowService workflowService;

    @Override
    public final void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");


        final JsonObject json = new JsonObject();

        try {
            // Only populate the form if removal is not running.
            json.add("form", this.getFormJSONObject(request.getResourceResolver()));
            Gson gson = new Gson();
            gson.toJson(json, response.getWriter());
        } catch (Exception e) {
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(e.getMessage());
        }
    }

    /**
     * Get the JSON data to populate the Workflow Removal form.
     *
     * @param resourceResolver
     * @return
     * @throws WorkflowException
     * @throws JSONException
     */
    private JsonObject getFormJSONObject(final ResourceResolver resourceResolver) throws WorkflowException {

        final JsonObject json = new JsonObject();

        final WorkflowSession workflowSession = workflowService.getWorkflowSession(
                resourceResolver.adaptTo(Session.class));

        final WorkflowModel[] workflowModels = workflowSession.getModels();

        JsonArray models = new JsonArray();
        json.add("workflowModels", models);
        for (final WorkflowModel workflowModel : workflowModels) {
            final JsonObject jsonWorkflow = new JsonObject();
            jsonWorkflow.addProperty("title", workflowModel.getTitle());
            jsonWorkflow.addProperty("id", workflowModel.getId());
            models.add(jsonWorkflow);
        }

        Gson gson = new Gson();
        json.add("statuses", gson.toJsonTree(Arrays.asList(WORKFLOW_STATUSES)));

        return json;
    }
}