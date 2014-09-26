package com.adobe.acs.commons.workflow.bulk.removal.impl.servlets;


import com.adobe.acs.commons.workflow.bulk.removal.WorkflowInstanceRemover;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger log = LoggerFactory.getLogger(RemoveServlet.class);

    private static final String WORKFLOW_STATUSES = "statuses";

    private static final String WORKFLOW_MODELS = "models";

    private static final String WORKFLOW_PAYLOADS = "payloads";

    private static final String OLDER_THAN = "olderThan";

    @Reference
    private WorkflowInstanceRemover workflowInstanceRemover;

    @Override
    public final void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");

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
                final JSONObject tmp = jsonArray.getJSONObject(i);
                final String pattern = tmp.optString("pattern");

                if(StringUtils.isNotBlank(pattern)) {
                    payloads.add(Pattern.compile(pattern));
                }
            }

            final Long ts = params.optLong(OLDER_THAN);
            if (ts != null && ts > 0) {
                olderThan = Calendar.getInstance();
                olderThan.setTimeInMillis(ts * 1000);
            }

            final Long limit = params.optLong("limit", 1000);


            final ResourceResolver resourceResolver = request.getResourceResolver();
            final Resource resource = request.getResource();
            final ModifiableValueMap mvm = resource.adaptTo(ModifiableValueMap.class);

            mvm.put("count", 0);
            mvm.put("state", "running");
            mvm.put("startedAt", Calendar.getInstance());
            mvm.remove("completedAt");

            resourceResolver.commit();

            int count;
            int total = 0;
            int loop = 1;
            do {
                count = workflowInstanceRemover.removeWorkflowInstances(request.getResourceResolver(),
                        models,
                        statuses,
                        payloads,
                        olderThan,
                        limit.intValue());

                total += count;

                mvm.put("count", total);
                resource.getResourceResolver().commit();

                log.trace("Completed loop [ {} ] of the Workflow Instance Remove Request", loop++);
            } while (count != 0 && count <= limit.intValue());

            mvm.put("state", "complete");
            mvm.put("completedAt", Calendar.getInstance());
            resourceResolver.commit();

        } catch (Exception e) {
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(e.getMessage());
        }
    }
}