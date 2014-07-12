package com.adobe.acs.commons.workflow.bulk.impl;

import com.adobe.acs.commons.workflow.bulk.BulkWorkflowManager;
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
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Date;

@SlingServlet(
        label = "ACS AEM Commons - Bulk Workflow Manager Servlet",
        description = "...",
        methods = { "POST", "GET" },
        resourceTypes = { BulkWorkflowManagerServlet.SLING_RESOURCE_TYPE },
        selectors = { "start", "stop", "resume", "status" },
        extensions = { "json" }
)
public class BulkWorkflowManagerServlet extends SlingAllMethodsServlet {
    private static final Logger log = LoggerFactory.getLogger(BulkWorkflowManagerServlet.class);

    public static final String SLING_RESOURCE_TYPE = "acs-commons/components/utilities/bulk-workflow-manager";

    @Reference
    private WorkflowService workflowService;

    @Reference
    private BulkWorkflowManager bulkWorkflowManager;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        try {
            response.getWriter().write(status(request).toString());
        } catch (JSONException e) {
            response.sendError(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error collecting status");
        }
    }

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
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

    private JSONObject start(SlingHttpServletRequest request) throws RepositoryException, JSONException,
            PersistenceException, ServletException {

        final JSONObject params = new JSONObject(request.getParameter("params"));
        final String query = params.getString("query");
        final String workflowModel = params.getString("workflowModel");
        final int batchSize = params.optInt("batchSize", 10);
        final int interval = params.optInt("updateInterval", BulkWorkflowManager.DEFAULT_INTERVAL); // in seconds
        final long estimatedTotal = params.optLong("estimatedTotal", 1000000); // Default to 1 million

        // Validate input

        if(batchSize < 1) {
            throw new ServletException("Batch size must be greater than zero.");
        }

        if(interval < 1) {
            throw new ServletException("Update interval must be greater than zero.");
        }

        final WorkflowSession workflowSession = workflowService.getWorkflowSession(request.getResourceResolver()
                .adaptTo(Session.class));
        try {
            final WorkflowModel model = workflowSession.getModel(workflowModel + "/jcr:content/model");
        } catch (WorkflowException e) {
            throw new ServletException(String.format("Unable to locate workflow at: %s",
                    workflowModel + "/jcr:content/model"));
        }

        bulkWorkflowManager.initialize(request.getResource(), query, estimatedTotal, batchSize, interval,
                workflowModel);
        bulkWorkflowManager.start(request.getResource());

        return this.status(request);
    }

    private JSONObject stop(final SlingHttpServletRequest request) throws PersistenceException, JSONException {
        bulkWorkflowManager.stop(request.getResource());
        return status(request);
    }

    private JSONObject resume(final SlingHttpServletRequest request) throws JSONException {
        bulkWorkflowManager.resume(request.getResource());
        return status(request);
    }

    private JSONObject status(final SlingHttpServletRequest request) throws JSONException {
        final ValueMap properties = request.getResource().adaptTo(ValueMap.class);

        final JSONObject json = new JSONObject();

        int total = properties.get(BulkWorkflowManager.PN_TOTAL, 0);
        int complete = properties.get(BulkWorkflowManager.PN_COMPLETE_COUNT, 0);

        json.put("state", properties.get(BulkWorkflowManager.PN_STATE, BulkWorkflowManager.STATE_NOT_STARTED));
        json.put("query", properties.get(BulkWorkflowManager.PN_QUERY, ""));
        json.put("workflowModel", properties.get(BulkWorkflowManager.PN_WORKFLOW_MODEL, ""));

        json.put("batchSize", properties.get(BulkWorkflowManager.PN_BATCH_SIZE, 0));
        json.put("currentBatch", properties.get(BulkWorkflowManager.PN_CURRENT_BATCH, "Unknown"));
        json.put("autoPurgeWorkflows", properties.get(BulkWorkflowManager.PN_AUTO_PURGE_WORKFLOW, "false"));

        // Counts
        json.put("total", total);
        json.put("complete", complete);
        json.put("remaining", total - complete);
        json.put("percentComplete", Math.round((complete / (total * 1F)) * 100));

        // Times
        json.put("startedAt", properties.get(BulkWorkflowManager.PN_STARTED_AT, Date.class));
        json.put("stoppedAt", properties.get(BulkWorkflowManager.PN_STOPPED_AT, Date.class));
        json.put("completedAt", properties.get(BulkWorkflowManager.PN_COMPLETED_AT, Date.class));


        final Resource currentBatch = bulkWorkflowManager.getCurrentBatch(request.getResource());
        if (currentBatch != null) {
            json.put("currentBatch", currentBatch.getPath());

            for (final Resource child : currentBatch.getChildren()) {
                json.accumulate("active", new JSONObject(child.adaptTo(ValueMap.class)));
            }
        }

        return json;
    }
}
