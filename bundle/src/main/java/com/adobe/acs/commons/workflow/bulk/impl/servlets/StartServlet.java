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

import com.adobe.acs.commons.workflow.bulk.BulkWorkflowEngine;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.HashMap;

@SlingServlet(
        label = "ACS AEM Commons - Bulk Workflow Manager - Start Servlet",
        methods = { "POST" },
        resourceTypes = { BulkWorkflowEngine.SLING_RESOURCE_TYPE },
        selectors = { "start" },
        extensions = { "json" }
)
public class StartServlet extends SlingAllMethodsServlet {
    private static final Logger log = LoggerFactory.getLogger(StartServlet.class);

    @Reference
    private BulkWorkflowEngine bulkWorkflowEngine;


    @Override
    protected final void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");

        try {
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

            response.sendRedirect(request.getResourceResolver().map(request.getResource().getPath()) + ".status.json");

        } catch (JSONException e) {
            log.error("Could not parse HTTP Request params: {}", e.getMessage());
            response.sendError(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Could not parse HTTP Request params: " +  e.getMessage());
        } catch (RepositoryException e) {
            log.error("Could not initialize Bulk Workflow: {}", e.getMessage());
            response.sendError(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Could not initialize Bulk Workflow: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Could not initialize Bulk Workflow due to invalid arguments: {}", e.getMessage());
            response.sendError(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "!!!" + e.getMessage());
        } catch (Exception e) {
            log.error("Could not initialize Bulk Workflow due to unexpected error: {}", e.getMessage());
            response.sendError(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error: " + e.getMessage());
        }
    }
}
