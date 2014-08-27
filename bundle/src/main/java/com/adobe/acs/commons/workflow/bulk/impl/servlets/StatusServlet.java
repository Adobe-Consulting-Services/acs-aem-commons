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

import com.adobe.acs.commons.util.TextUtil;
import com.adobe.acs.commons.workflow.bulk.BulkWorkflowEngine;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Date;

@SlingServlet(
        label = "ACS AEM Commons - Bulk Workflow Manager - Status Servlet",
        methods = { "GET" },
        resourceTypes = { BulkWorkflowEngine.SLING_RESOURCE_TYPE },
        selectors = { "status" },
        extensions = { "json" }
)
public class StatusServlet extends SlingAllMethodsServlet {
    private static final Logger log = LoggerFactory.getLogger(StatusServlet.class);

    private static final int DECIMAL_TO_PERCENT = 100;

    @Reference
    private BulkWorkflowEngine bulkWorkflowEngine;

    @Override
    protected final void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        final ValueMap properties = request.getResource().adaptTo(ValueMap.class);

        final JSONObject json = new JSONObject();

        int total = properties.get(BulkWorkflowEngine.KEY_TOTAL, 0);
        int complete = properties.get(BulkWorkflowEngine.KEY_COMPLETE_COUNT, 0);

        try {
            json.put(BulkWorkflowEngine.KEY_STATE,
                    properties.get(BulkWorkflowEngine.KEY_STATE, BulkWorkflowEngine.STATE_NOT_STARTED));


            json.put(BulkWorkflowEngine.KEY_QUERY,
                    properties.get(BulkWorkflowEngine.KEY_QUERY, ""));

            json.put(BulkWorkflowEngine.KEY_WORKFLOW_MODEL,
                    StringUtils.removeEnd(properties.get(BulkWorkflowEngine.KEY_WORKFLOW_MODEL, ""),
                            "/jcr:content/model"));

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

            response.getWriter().write(json.toString());

        } catch (JSONException e) {
            log.error("Could not collect Bulk Workflow status due to: {}", e.getMessage());

            HttpErrorUtil.sendJSONError(response, SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Could not collect Bulk Workflow status.",
                    TextUtil.getFirstNonEmpty(e.getMessage(), "Check to ensure the ACS AEM Commons bundle is "
                            + "installed and active."));

        }
    }
}
