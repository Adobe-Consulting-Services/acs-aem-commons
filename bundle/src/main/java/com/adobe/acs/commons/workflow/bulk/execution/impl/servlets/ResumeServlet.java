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

import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_EXTENSIONS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_METHODS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_SELECTORS;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.workflow.bulk.execution.BulkWorkflowEngine;
import com.adobe.acs.commons.workflow.bulk.execution.model.Config;

/**
 * ACS AEM Commons - Bulk Workflow Manager - Resume Servlet
 */
@SuppressWarnings("serial")
@Component(service = Servlet.class, property = {
      SLING_SERVLET_RESOURCE_TYPES + "=" + BulkWorkflowEngine.SLING_RESOURCE_TYPE,
      SLING_SERVLET_SELECTORS + "=resume",
      SLING_SERVLET_METHODS + "=POST",
      SLING_SERVLET_EXTENSIONS + "=json" })
public class ResumeServlet extends SlingAllMethodsServlet {
    private static final Logger log = LoggerFactory.getLogger(ResumeServlet.class);

    @Reference
    private BulkWorkflowEngine bulkWorkflowEngine;

    @Override
    protected final void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        final JSONObject params;
        try {
            params = new JSONObject(request.getParameter("params"));

            final Config config = request.getResource().adaptTo(Config.class);
            int throttle = params.optInt("throttle", -1);
            int interval = params.optInt("interval", -1);

            if (throttle > -1) {
                config.setThrottle(throttle);
                config.commit();
            } else if (interval > -1) {
                config.setInterval(interval);
                config.commit();
            }

            bulkWorkflowEngine.resume(config);

            response.sendRedirect(request.getResourceResolver().map(request, request.getResource().getPath()) + ".status.json");
        } catch (JSONException e) {
            log.error("Could not resume Bulk Workflow due to: {}", e);

            JSONErrorUtil.sendJSONError(response, SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Could not resume Bulk Workflow.",
                    e.getMessage());
        }
    }
}
