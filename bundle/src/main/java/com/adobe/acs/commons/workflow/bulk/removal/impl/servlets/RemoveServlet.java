package com.adobe.acs.commons.workflow.bulk.removal.impl.servlets;


import com.adobe.acs.commons.workflow.bulk.removal.WorkflowInstanceRemover;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
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
        label = "ACS AEM Commons - Workflow Instance Remover - Remove Servlet",
        methods = { "POST" },
        resourceTypes = { "acs-commons/components/utilities/workflow-remover" },
        selectors = { "remove" },
        extensions = { "json" }
)
public class RemoveServlet extends SlingAllMethodsServlet {
    private static final Logger log = LoggerFactory.getLogger(RemoveServlet.class);

    private static final String PARAM_WORKFLOW_STATUSES = "statuses";

    private static final String PARAM_WORKFLOW_MODELS = "models";

    private static final String PARAM_WORKFLOW_PAYLOADS = "payloads";

    private static final String PARAM_OLDER_THAN = "olderThan";

    private static final String PARAM_LIMIT = "limit";


    @Reference
    private WorkflowInstanceRemover workflowInstanceRemover;

    @Override
    public final void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        List<String> statuses = new ArrayList<String>();
        List<String> models = new ArrayList<String>();
        List<Pattern> payloads = new ArrayList<Pattern>();
        Calendar olderThan = null;

        try {
            JSONObject params = new JSONObject(request.getParameter("params"));

            JSONArray jsonArray = params.optJSONArray(PARAM_WORKFLOW_STATUSES);
            for (int i = 0; i < jsonArray.length(); i++) {
                statuses.add(jsonArray.getString(i));
            }

            jsonArray = params.optJSONArray(PARAM_WORKFLOW_MODELS);
            for (int i = 0; i < jsonArray.length(); i++) {
                models.add(jsonArray.getString(i));
            }

            jsonArray = params.optJSONArray(PARAM_WORKFLOW_PAYLOADS);
            for (int i = 0; i < jsonArray.length(); i++) {
                final JSONObject tmp = jsonArray.getJSONObject(i);
                final String pattern = tmp.optString("pattern");

                if (StringUtils.isNotBlank(pattern)) {
                    payloads.add(Pattern.compile(pattern));
                }
            }

            final Long ts = params.optLong(PARAM_OLDER_THAN);
            if (ts != null && ts > 0) {
                olderThan = Calendar.getInstance();
                olderThan.setTimeInMillis(ts * 1000);
            }

            final Long limit = params.optLong(PARAM_LIMIT, 1000);

            workflowInstanceRemover.removeWorkflowInstances(request.getResourceResolver(),
                    models,
                    statuses,
                    payloads,
                    olderThan,
                    limit.intValue());

        } catch (Exception e) {
            log.error("An error occurred while attempting to remove workflow instances.", e);

            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            if (response.getWriter() != null) {
                response.getWriter().write(e.getMessage());
            }
        }
    }
}