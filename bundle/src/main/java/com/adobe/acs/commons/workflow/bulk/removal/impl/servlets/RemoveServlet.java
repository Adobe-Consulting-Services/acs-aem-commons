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

import com.adobe.acs.commons.workflow.bulk.removal.WorkflowInstanceRemover;
import com.adobe.acs.commons.workflow.bulk.removal.WorkflowRemovalForceQuitException;

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

/**
 * ACS AEM Commons - Workflow Instance Remover - Remove Servlet
 */
@SuppressWarnings("serial")
@SlingServlet(
        methods = { "POST" },
        resourceTypes = { "acs-commons/components/utilities/workflow-remover" },
        selectors = { "remove" },
        extensions = { "json" }
)
public class RemoveServlet extends SlingAllMethodsServlet {
    private static final Logger log = LoggerFactory.getLogger(RemoveServlet.class);

    private static final String PARAM_BATCH_SIZE = "batchSize";
    
    private static final String PARAM_WORKFLOW_STATUSES = "statuses";
    
    private static final String PARAM_WORKFLOW_MODELS = "models";

    private static final String PARAM_WORKFLOW_PAYLOADS = "payloads";

    private static final String PARAM_OLDER_THAN = "olderThan";

    private static final String PARAM_MAX_DURATION = "maxDuration";

    private static final int MS_IN_SECOND = 1000;

    private static final int DEFAULT_BATCH_SIZE = 1000;

    private static final int DEFAULT_MAX_DURATION = 0;

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
                olderThan.setTimeInMillis(ts * MS_IN_SECOND);
            }

            int batchSize = params.optInt(PARAM_BATCH_SIZE);
            if (batchSize < 1) {
                batchSize = DEFAULT_BATCH_SIZE;
            }

            int maxDuration = params.optInt(PARAM_MAX_DURATION);
            if (maxDuration < 1) {
                maxDuration = DEFAULT_MAX_DURATION;
            }

            workflowInstanceRemover.removeWorkflowInstances(request.getResourceResolver(),
                    models,
                    statuses,
                    payloads,
                    olderThan,
                    batchSize,
                    maxDuration);

        } catch (WorkflowRemovalForceQuitException e) {
            response.setStatus(599);
            response.getWriter().write("Workflow removal force quit");

        } catch (Exception e) {
            log.error("An error occurred while attempting to remove workflow instances.", e);

            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            if (response.getWriter() != null) {
                response.getWriter().write(e.getMessage());
            }
        }
    }
}