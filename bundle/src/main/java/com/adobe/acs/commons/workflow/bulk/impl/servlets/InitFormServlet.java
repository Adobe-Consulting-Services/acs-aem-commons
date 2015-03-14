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

package com.adobe.acs.commons.workflow.bulk.impl.servlets;

import com.adobe.acs.commons.workflow.bulk.BulkWorkflowEngine;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowService;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.model.WorkflowModel;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import javax.servlet.ServletException;
import java.io.IOException;

@SlingServlet(
        label = "ACS AEM Commons - Bulk Workflow Manager - Init Form Servlet",
        methods = { "GET" },
        resourceTypes = { BulkWorkflowEngine.SLING_RESOURCE_TYPE },
        selectors = { "init-form" },
        extensions = { "json" }
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

        final WorkflowSession workflowSession = workflowService.getWorkflowSession(
                request.getResourceResolver().adaptTo(Session.class));

        try {
            final WorkflowModel[] workflowModels = workflowSession.getModels();

            for (final WorkflowModel workflowModel : workflowModels) {
                JSONObject jsonWorkflow = new JSONObject();
                try {
                    jsonWorkflow.put("label", workflowModel.getTitle());
                    jsonWorkflow.put("value", workflowModel.getId());
                    json.accumulate("workflowModels", jsonWorkflow);
                } catch (JSONException e) {
                    log.error("Could not add workflow [ {} - {} ] to Workflow Models dropdown JSON object",
                            workflowModel.getTitle(), workflowModel.getId());
                }
            }

            response.getWriter().write(json.toString());

        } catch (WorkflowException e) {
            log.error("Could not create workflow model drop-down due to: {}", e);

            HttpErrorUtil.sendJSONError(response, SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Could not collect Workflows",
                    e.getMessage());
        }
    }
}
