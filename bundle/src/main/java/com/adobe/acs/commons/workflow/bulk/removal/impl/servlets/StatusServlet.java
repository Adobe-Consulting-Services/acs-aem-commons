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
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowService;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.model.WorkflowModel;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

import javax.jcr.Session;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Arrays;

/**
 * ACS AEM Commons - Workflow Instance Remover - Status Servlet
 */
@SuppressWarnings("serial")
@SlingServlet(
        methods = { "GET" },
        resourceTypes = { "acs-commons/components/utilities/workflow-remover" },
        selectors = { "status" },
        extensions = { "json" }
)
public class StatusServlet extends SlingSafeMethodsServlet {

    @Reference
    private WorkflowInstanceRemover workflowInstanceRemover;
    
    @Override
    public final void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        JSONObject json = null;

        try {
            boolean running = workflowInstanceRemover.isRunning();
            
            ValueMap properties = request.getResourceResolver().getResource(WorkflowInstanceRemover.WORKFLOW_REMOVAL_STATUS_PATH).adaptTo(ValueMap.class);
            json = new JSONObject(properties);
            
            json.put(WorkflowInstanceRemover.PN_RUNNING, running);
            
            if(running) {
                if(StringUtils.equalsIgnoreCase(properties.get(WorkflowInstanceRemover.PN_STATUS, String.class),  WorkflowInstanceRemover.Status.COMPLETE.name())) {
                    json.put(WorkflowInstanceRemover.PN_STATUS, WorkflowInstanceRemover.Status.ERROR.name());
                }
            }

            response.getWriter().write(json.toString());
        } catch (Exception e) {
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(e.getMessage());
        }
    }
    
}