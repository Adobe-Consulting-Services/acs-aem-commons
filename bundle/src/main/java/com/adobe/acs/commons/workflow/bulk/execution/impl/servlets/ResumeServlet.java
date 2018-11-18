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
import com.adobe.acs.commons.workflow.bulk.execution.model.Config;
import com.google.gson.JsonObject;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.io.IOException;

import static com.adobe.acs.commons.json.JsonObjectUtil.*;

/**
 * ACS AEM Commons - Bulk Workflow Manager - Resume Servlet
 */
@SuppressWarnings("serial")
@SlingServlet(
        methods = {"POST"},
        resourceTypes = {BulkWorkflowEngine.SLING_RESOURCE_TYPE},
        selectors = {"resume"},
        extensions = {"json"}
)
public class ResumeServlet extends SlingAllMethodsServlet {

    @Reference
    private BulkWorkflowEngine bulkWorkflowEngine;

    @Override
    protected final void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        final JsonObject params = toJsonObject(request.getParameter("params"));

        final Config config = request.getResource().adaptTo(Config.class);
        int throttle = getInteger(params, "throttle", -1);
        int interval = getInteger(params, "interval", -1);

        if (throttle > -1) {
            config.setThrottle(throttle);
            config.commit();
        } else if (interval > -1) {
            config.setInterval(interval);
            config.commit();
        }

        bulkWorkflowEngine.resume(config);

        response.sendRedirect(request.getResourceResolver().map(request, request.getResource().getPath()) + ".status.json");
    }
}
