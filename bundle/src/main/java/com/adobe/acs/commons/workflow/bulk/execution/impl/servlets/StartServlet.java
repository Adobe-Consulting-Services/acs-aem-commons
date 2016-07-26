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
import com.adobe.acs.commons.workflow.bulk.execution.impl.runners.FastActionManagerRunnerImpl;
import com.adobe.acs.commons.workflow.bulk.execution.model.Config;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import java.io.IOException;

/**
 * ACS AEM Commons - Bulk Workflow Manager - Start Servlet
 */
@SuppressWarnings("serial")
@SlingServlet(
        methods = {"POST"},
        resourceTypes = {BulkWorkflowEngine.SLING_RESOURCE_TYPE},
        selectors = {"start"},
        extensions = {"json"}
)
public class StartServlet extends SlingAllMethodsServlet {
    private static final Logger log = LoggerFactory.getLogger(StartServlet.class);

    @Reference
    private BulkWorkflowEngine bulkWorkflowEngine;

    @Override
    protected final void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            final JSONObject params = new JSONObject(request.getParameter("params"));
            final ModifiableValueMap properties = request.getResource().adaptTo(ModifiableValueMap.class);

            properties.put("runnerType", params.getString("runnerType"));
            properties.put("queryType", params.getString("queryType"));
            properties.put("queryStatement", params.getString("queryStatement"));
            properties.put("relativePath", StringUtils.removeStart(params.optString("relativePath", ""), "/"));
            properties.put("workflowModel", params.getString("workflowModel"));
            properties.put("interval", params.optInt("interval", 10));
            properties.put("timeout", params.optInt("timeout", 30));
            properties.put("throttle", params.optInt("throttle", 10));
            properties.put("retryCount", params.optInt("retryCount", 0));
            properties.put("batchSize", params.optInt("batchSize", 10));

            properties.put("purgeWorkflow", params.optBoolean("purgeWorkflow", false));
            properties.put("autoThrottle", params.optBoolean("autoThrottle", true));

            // If FAM retires are enabled, then force BatchSize to be 1
            if (FastActionManagerRunnerImpl.class.getName().equals(properties.get("runnerType", "")) && properties.get("retryCount", 0) > 0) {
                properties.put("batchSize", 1);
            }

            request.getResourceResolver().commit();

            Config config = request.getResource().adaptTo(Config.class);

            bulkWorkflowEngine.initialize(config);
            bulkWorkflowEngine.start(config);

            response.sendRedirect(request.getResourceResolver().map(request, request.getResource().getPath()) + ".status.json");

        } catch (JSONException e) {
            log.error("Could not parse HTTP Request params: {}", e);

            JSONErrorUtil.sendJSONError(response, SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Could not initialize Bulk Workflow due to invalid parameters."
                            + " Please review the form and try again.",
                    e.getMessage());
        } catch (RepositoryException e) {
            log.error("Could not initialize Bulk Workflow: {}", e);

            JSONErrorUtil.sendJSONError(response, SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Could not initialize Bulk Workflow.",
                    e.getMessage());

        } catch (IllegalArgumentException e) {
            log.warn("Could not initialize Bulk Workflow due to invalid arguments: {}", e);

            JSONErrorUtil.sendJSONError(response, SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Could not initialize Bulk Workflow due to invalid arguments.",
                    e.getMessage());

        } catch (Exception e) {
            log.error("Could not initialize Bulk Workflow due to unexpected error: {}", e);

            JSONErrorUtil.sendJSONError(response, SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Could not start Bulk Workflow.",
                    e.getMessage());
        }
    }
}
