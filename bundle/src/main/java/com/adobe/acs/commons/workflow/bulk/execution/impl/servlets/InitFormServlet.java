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
import com.adobe.acs.commons.workflow.bulk.execution.impl.TransientWorkflowUtil;
import com.adobe.acs.commons.workflow.bulk.execution.impl.runners.AEMWorkflowRunnerImpl;
import com.adobe.acs.commons.workflow.bulk.execution.impl.runners.FastActionManagerRunnerImpl;
import com.adobe.acs.commons.workflow.bulk.execution.impl.runners.SyntheticWorkflowRunnerImpl;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowService;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.model.WorkflowModel;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
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
    private static final String KEY_RUNNER_TYPES = "runnerTypes";
    private static final String KEY_LABEL = "label";
    private static final String KEY_QUERY_TYPES = "queryTypes";
    private static final String KEY_VALUE = "value";
    private static final String KEY_USER_EVENT_DATA = "userEventData";

    @Reference
    private WorkflowService workflowService;

    @Override
    protected final void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        final JsonObject json = new JsonObject();

        // Runners
        accumulate(json, KEY_RUNNER_TYPES, withLabelValue("AEM Workflow", AEMWorkflowRunnerImpl.class.getName()));
        accumulate(json, KEY_RUNNER_TYPES, withLabelValue("Synthetic Workflow (Single-threaded)", SyntheticWorkflowRunnerImpl.class.getName()));
        accumulate(json, KEY_RUNNER_TYPES, withLabelValue("Synthetic Workflow (Multi-threaded)", FastActionManagerRunnerImpl.class.getName()));

        // Query Types
        accumulate(json, KEY_QUERY_TYPES, withLabelValue("QueryBuilder", "queryBuilder"));
        accumulate(json, KEY_QUERY_TYPES, withLabelValue("List", "list"));
        accumulate(json, KEY_QUERY_TYPES, withLabelValue("xPath", "xpath"));
        accumulate(json, KEY_QUERY_TYPES, withLabelValue("JCR-SQL2", "JCR-SQL2"));
        accumulate(json, KEY_QUERY_TYPES, withLabelValue("JCR-SQL", "JCR-SQL"));

        // User Event Data
        accumulate(json, KEY_USER_EVENT_DATA, withLabelValue("Custom user-event-data", ""));
        accumulate(json, KEY_USER_EVENT_DATA, withLabelValue("changedByWorkflowProcess", "changedByWorkflowProcess"));
        accumulate(json, KEY_USER_EVENT_DATA, withLabelValue("acs-aem-commons.bulk-workflow-manager", "acs-aem-commons.bulk-workflow-manager"));

        // Workflow Models
        final WorkflowSession workflowSession = workflowService.getWorkflowSession(
                request.getResourceResolver().adaptTo(Session.class));

        try {
            final WorkflowModel[] workflowModels = workflowSession.getModels();

            for (final WorkflowModel workflowModel : workflowModels) {
                boolean transientWorkflow = TransientWorkflowUtil.isTransient(request.getResourceResolver(), workflowModel.getId());
                String workflowLabel = workflowModel.getTitle();
                if (transientWorkflow) {
                    workflowLabel += " ( Transient )";
                }
                JsonObject jsonWorkflow = withLabelValue(workflowLabel, workflowModel.getId());
                jsonWorkflow.addProperty("transient", transientWorkflow);
                accumulate(json, "workflowModels", jsonWorkflow);
            }

            response.getWriter().write(json.toString());
        } catch (WorkflowException e) {
            log.error("Could not create workflow model drop-down.", e);

            JSONErrorUtil.sendJSONError(response, SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Could not collect workflows",
                    e.getMessage());
        }
    }


    private JsonObject withLabelValue(String label, String value) {
        JsonObject obj = new JsonObject();
        obj.addProperty(KEY_LABEL, label);
        obj.addProperty(KEY_VALUE, value);
        return obj;
    }

    private JsonObject accumulate(JsonObject obj, String key, JsonElement value) {
        if (obj.has(key)) {
            JsonElement existingValue = obj.get(key);
            if (existingValue instanceof JsonArray) {
                ((JsonArray) existingValue).add(value);
            } else {
                JsonArray array = new JsonArray();
                array.add(existingValue);
                obj.add(key, array);
            }
        } else {
            JsonArray array = new JsonArray();
            array.add(value);
            obj.add(key, array);
        }
        return obj;
    }

}
