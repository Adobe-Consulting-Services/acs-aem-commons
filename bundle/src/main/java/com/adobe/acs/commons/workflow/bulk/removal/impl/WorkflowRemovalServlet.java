package com.adobe.acs.commons.workflow.bulk.removal.impl;


import com.adobe.acs.commons.workflow.bulk.removal.WorkflowInstanceRemover;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Pattern;

@SlingServlet(
        label = "Samples - Sling Servlet",
        description = "...",
        paths = {"/services/all-sample"},
        methods = {"GET", "POST"},
        resourceTypes = {},
        selectors = {"print.a4"},
        extensions = {"html", "htm"}
)
public class WorkflowRemovalServlet extends SlingAllMethodsServlet {
    private static final String WORKFLOW_STATUSES = "statuses";
    private static final String WORKFLOW_MODELS = "models";
    private static final String WORKFLOW_PAYLOADS = "payloads";
    private static final String OLDER_THAN = "olderThan";

    @Reference
    private WorkflowInstanceRemover workflowInstanceRemover;
    
    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException { 

        List<String> statuses = new ArrayList<String>();
        List<String> models = new ArrayList<String>();
        List<Pattern> payloads = new ArrayList<Pattern>();
        Calendar olderThan = null;

        try {
            JSONObject params = new JSONObject(request.getParameter("params"));

            JSONArray jsonArray = params.optJSONArray(WORKFLOW_STATUSES);
            for(int i = 0; i < jsonArray.length(); i++) {
                statuses.add(jsonArray.getString(i));
            }

            jsonArray = params.optJSONArray(WORKFLOW_MODELS);
            for(int i = 0; i < jsonArray.length(); i++) {
                models.add(jsonArray.getString(i));
            }

            jsonArray = params.optJSONArray(WORKFLOW_PAYLOADS);
            for(int i = 0; i < jsonArray.length(); i++) {
                payloads.add(Pattern.compile(jsonArray.getString(i)));
            }

            final Long ts = params.optLong(OLDER_THAN);
            if(ts != null) {
                olderThan = Calendar.getInstance();
                olderThan.setTimeInMillis(ts);
            }

            int count = workflowInstanceRemover.removeWorkflowInstances(request.getResourceResolver(),
                    statuses,
                    models,
                    payloads,
                    olderThan);

            response.setContentType("application/json");

            JSONObject jsonResponse = new JSONObject();
            jsonResponse.put("status", "success");
            jsonResponse.put("count", count);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}