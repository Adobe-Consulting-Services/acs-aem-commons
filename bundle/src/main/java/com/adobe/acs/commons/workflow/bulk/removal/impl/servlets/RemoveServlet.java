package com.adobe.acs.commons.workflow.bulk.removal.impl.servlets;


import com.adobe.acs.commons.workflow.bulk.removal.WorkflowInstanceRemover;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
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
        label = "ACS AEM Commons - Workflow Remover - Remove Servlet",
        methods = { "POST" },
        resourceTypes = { "acs-commons/components/utilities/workflow-remover" },
        selectors = { "remove" },
        extensions = { "json" }
)
public class RemoveServlet extends SlingAllMethodsServlet {
    private static final String WORKFLOW_STATUSES = "statuses";

    private static final String WORKFLOW_MODELS = "models";

    private static final String WORKFLOW_PAYLOADS = "payloads";

    private static final String OLDER_THAN = "olderThan";

    @Reference
    private WorkflowInstanceRemover workflowInstanceRemover;

    @Override
    public final void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        List<String> statuses = new ArrayList<String>();
        List<String> models = new ArrayList<String>();
        List<Pattern> payloads = new ArrayList<Pattern>();
        Calendar olderThan = null;

        try {
            JSONObject params = new JSONObject(request.getParameter("params"));

            JSONArray jsonArray = params.optJSONArray(WORKFLOW_STATUSES);
            for (int i = 0; i < jsonArray.length(); i++) {
                statuses.add(jsonArray.getString(i));
            }

            jsonArray = params.optJSONArray(WORKFLOW_MODELS);
            for (int i = 0; i < jsonArray.length(); i++) {
                models.add(jsonArray.getString(i));
            }

            jsonArray = params.optJSONArray(WORKFLOW_PAYLOADS);
            for (int i = 0; i < jsonArray.length(); i++) {
                payloads.add(Pattern.compile(jsonArray.getString(i)));
            }

            final Long ts = params.optLong(OLDER_THAN);
            if (ts != null) {
                olderThan = Calendar.getInstance();
                olderThan.setTimeInMillis(ts);
            }

            final Long limit = params.optLong("limit", 1000);


            final ResourceResolver resourceResolver = request.getResourceResolver();
            final Resource resource = request.getResource();
            final ModifiableValueMap mvm = resource.adaptTo(ModifiableValueMap.class);

            mvm.put("count", 0);
            mvm.put("status", "running");
            mvm.put("startTime", Calendar.getInstance());
            mvm.remove("endTime");

            resourceResolver.commit();

            int count;
            int total = 0;
            do {
                count = workflowInstanceRemover.removeWorkflowInstances(request.getResourceResolver(),
                        statuses,
                        models,
                        payloads,
                        olderThan,
                        limit.intValue());

                total += count;

                mvm.put("count", total);
                resource.getResourceResolver().commit();

            } while (count > 0);

            mvm.put("status", "complete");
            mvm.put("endTime", Calendar.getInstance());
            resourceResolver.commit();

            response.setContentType("application/json");

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}