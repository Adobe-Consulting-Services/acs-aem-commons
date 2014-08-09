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

package com.adobe.acs.commons.workflow.bulk.impl;

import com.adobe.acs.commons.workflow.bulk.BulkWorkflowEngine;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowService;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.model.WorkflowModel;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;

@SlingServlet(
        label = "ACS AEM Commons - Bulk Workflow Manager Servlet",
        methods = { "POST", "GET" },
        resourceTypes = { "dev/null" },
        selectors = { "start", "stop", "resume", "status", "form" },
        extensions = { "json" }
)
public class BulkWorkflowManagerServlet extends SlingAllMethodsServlet {
    private static final Logger log = LoggerFactory.getLogger(BulkWorkflowManagerServlet.class);

    private static final int DECIMAL_TO_PERCENT = 100;
    public static final String SLING_RESOURCE_TYPE = "acs-commons/components/utilities/bulk-workflow-manager";

    @Reference
    private WorkflowService workflowService;

    @Reference
    private BulkWorkflowEngine bulkWorkflowEngine;

    @Override
    protected final void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        JSONObject json = new JSONObject();

        try {
            if (StringUtils.equals("form", request.getRequestPathInfo().getSelectorString())) {
                json = this.form(request);
            } else {
                json = this.status(request);
            }
        } catch (JSONException e) {
            response.sendError(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error collecting status");
        }

        response.getWriter().write(json.toString());
    }

    @Override
    protected final void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        JSONObject json = new JSONObject();

        try {
            if (StringUtils.equals("start", request.getRequestPathInfo().getSelectorString())) {
                json = this.start(request);
            } else if (StringUtils.equals("resume", request.getRequestPathInfo().getSelectorString())) {
                json = this.resume(request);
            } else if (StringUtils.equals("stop", request.getRequestPathInfo().getSelectorString())) {
                json = this.stop(request);
            } else {
                response.sendError(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Use proper selector to initiate action.");
            }
        } catch (Exception e) {
            log.error("Error handling POST for bulk workflow management. {}", e.getMessage());

            response.sendError(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    e.getMessage());
        }

        response.getWriter().write(json.toString());
    }

    private JSONObject form(final SlingHttpServletRequest request) {
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
        } catch (WorkflowException e) {
            log.error("Could not create workflow model drop-down due to: {}", e.getMessage());
        }

        return json;
    }

    private JSONObject start(SlingHttpServletRequest request) throws RepositoryException, JSONException,
            PersistenceException, ServletException {

        final JSONObject params = new JSONObject(request.getParameter("params"));

        final ValueMap map = new ValueMapDecorator(new HashMap<String, Object>());

        map.put(BulkWorkflowEngine.KEY_QUERY,
                params.getString(BulkWorkflowEngine.KEY_QUERY));

        map.put(BulkWorkflowEngine.KEY_RELATIVE_PATH,
                StringUtils.removeStart(params.optString(BulkWorkflowEngine.KEY_RELATIVE_PATH, ""), "/"));

        map.put(BulkWorkflowEngine.KEY_WORKFLOW_MODEL,
                params.getString(BulkWorkflowEngine.KEY_WORKFLOW_MODEL));

        map.put(BulkWorkflowEngine.KEY_BATCH_SIZE,
                params.optInt(BulkWorkflowEngine.KEY_BATCH_SIZE, BulkWorkflowEngine.DEFAULT_BATCH_SIZE));

        map.put(BulkWorkflowEngine.KEY_INTERVAL,
                params.optInt(BulkWorkflowEngine.KEY_INTERVAL, BulkWorkflowEngine.DEFAULT_INTERVAL));

        map.put(BulkWorkflowEngine.KEY_BATCH_TIMEOUT,
                params.optInt(BulkWorkflowEngine.KEY_BATCH_TIMEOUT, BulkWorkflowEngine.DEFAULT_BATCH_TIMEOUT));

        map.put(BulkWorkflowEngine.KEY_ESTIMATED_TOTAL,
                params.optLong(BulkWorkflowEngine.KEY_ESTIMATED_TOTAL, BulkWorkflowEngine.DEFAULT_ESTIMATED_TOTAL));

        map.put(BulkWorkflowEngine.KEY_PURGE_WORKFLOW,
                params.optBoolean(BulkWorkflowEngine.KEY_PURGE_WORKFLOW, BulkWorkflowEngine.DEFAULT_PURGE_WORKFLOW));

        bulkWorkflowEngine.initialize(request.getResource(), map);
        bulkWorkflowEngine.start(request.getResource());

        return this.status(request);
    }

    private JSONObject stop(final SlingHttpServletRequest request) throws PersistenceException, JSONException {
        bulkWorkflowEngine.stop(request.getResource());
        return status(request);
    }

    private JSONObject resume(final SlingHttpServletRequest request) throws JSONException, PersistenceException {
        final JSONObject params = new JSONObject(request.getParameter("params"));
        final long interval = params.optLong(BulkWorkflowEngine.KEY_INTERVAL, -1);

        if (interval < 1) {
            bulkWorkflowEngine.resume(request.getResource());
        } else {
            bulkWorkflowEngine.resume(request.getResource(), interval);
        }

        return status(request);
    }

    private JSONObject status(final SlingHttpServletRequest request) throws JSONException {
        final ValueMap properties = request.getResource().adaptTo(ValueMap.class);

        final JSONObject json = new JSONObject();

        int total = properties.get(BulkWorkflowEngine.KEY_TOTAL, 0);
        int complete = properties.get(BulkWorkflowEngine.KEY_COMPLETE_COUNT, 0);

        json.put(BulkWorkflowEngine.KEY_STATE,
                properties.get(BulkWorkflowEngine.KEY_STATE, BulkWorkflowEngine.STATE_NOT_STARTED));

        json.put(BulkWorkflowEngine.KEY_QUERY,
                properties.get(BulkWorkflowEngine.KEY_QUERY, ""));

        json.put(BulkWorkflowEngine.KEY_WORKFLOW_MODEL,
                StringUtils.removeEnd(properties.get(BulkWorkflowEngine.KEY_WORKFLOW_MODEL, ""), "/jcr:content/model"));

        json.put(BulkWorkflowEngine.KEY_BATCH_SIZE,
                properties.get(BulkWorkflowEngine.KEY_BATCH_SIZE, BulkWorkflowEngine.DEFAULT_BATCH_SIZE));

        json.put(BulkWorkflowEngine.KEY_CURRENT_BATCH,
                properties.get(BulkWorkflowEngine.KEY_CURRENT_BATCH, "Unknown"));

        json.put(BulkWorkflowEngine.KEY_PURGE_WORKFLOW,
                properties.get(BulkWorkflowEngine.KEY_PURGE_WORKFLOW, BulkWorkflowEngine.DEFAULT_PURGE_WORKFLOW));

        json.put(BulkWorkflowEngine.KEY_INTERVAL,
                properties.get(BulkWorkflowEngine.KEY_INTERVAL, BulkWorkflowEngine.DEFAULT_INTERVAL));

        json.put(BulkWorkflowEngine.KEY_BATCH_TIMEOUT,
                properties.get(BulkWorkflowEngine.KEY_BATCH_TIMEOUT, BulkWorkflowEngine.DEFAULT_BATCH_TIMEOUT));

        // Counts
        json.put(BulkWorkflowEngine.KEY_TOTAL, total);
        json.put(BulkWorkflowEngine.KEY_COMPLETE_COUNT, complete);
        json.put("remaining", total - complete);
        json.put("percentComplete", Math.round((complete / (total * 1F)) * DECIMAL_TO_PERCENT));

        json.put(BulkWorkflowEngine.KEY_FORCE_TERMINATED_COUNT,
                properties.get(BulkWorkflowEngine.KEY_FORCE_TERMINATED_COUNT, 0));

        // Times
        json.put(BulkWorkflowEngine.KEY_STARTED_AT,
                properties.get(BulkWorkflowEngine.KEY_STARTED_AT, Date.class));

        json.put(BulkWorkflowEngine.KEY_STOPPED_AT,
                properties.get(BulkWorkflowEngine.KEY_STOPPED_AT, Date.class));

        json.put(BulkWorkflowEngine.KEY_COMPLETED_AT,
                properties.get(BulkWorkflowEngine.KEY_COMPLETED_AT, Date.class));

        final Resource currentBatch = bulkWorkflowEngine.getCurrentBatch(request.getResource());
        final ValueMap currentBatchProperties = currentBatch.adaptTo(ValueMap.class);

        if (currentBatch != null) {
            json.put(BulkWorkflowEngine.KEY_CURRENT_BATCH, currentBatch.getPath());

            for (final Resource child : currentBatch.getChildren()) {
                json.accumulate("currentBatchItems", new JSONObject(child.adaptTo(ValueMap.class)));
            }
        }

        json.put(BulkWorkflowEngine.KEY_BATCH_TIMEOUT_COUNT, currentBatchProperties.get(BulkWorkflowEngine
                .KEY_BATCH_TIMEOUT_COUNT, 1));

        return json;
    }
}
