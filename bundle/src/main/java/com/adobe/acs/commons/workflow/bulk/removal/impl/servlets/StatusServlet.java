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

import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_EXTENSIONS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_METHODS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_SELECTORS;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.workflow.bulk.removal.WorkflowInstanceRemover;
import com.adobe.acs.commons.workflow.bulk.removal.WorkflowRemovalStatus;

/**
 * ACS AEM Commons - Workflow Instance Remover - Status Servlet
 */
@SuppressWarnings("serial")
@Component(service = Servlet.class, property = {
SLING_SERVLET_RESOURCE_TYPES + "=acs-commons/components/utilities/workflow-remover",
SLING_SERVLET_SELECTORS + "=status",
SLING_SERVLET_METHODS + "=GET",
SLING_SERVLET_EXTENSIONS + "=json" })
public class StatusServlet extends SlingSafeMethodsServlet {
    private static final Logger log = LoggerFactory.getLogger(StatusServlet.class);

    @Reference
    private WorkflowInstanceRemover workflowInstanceRemover;
    
    @Override
    public final void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            if (workflowInstanceRemover.getStatus() != null) {
                response.getWriter().write(workflowInstanceRemover.getStatus().getJSON().toString());
            } else {
                WorkflowRemovalStatus workflowStatus = new WorkflowRemovalStatus(request.getResourceResolver());
                workflowStatus.setRunning(false);

                response.getWriter().write(workflowStatus.getJSON().toString());
            }
        } catch (Exception e) {
            log.error("Unable to create Workflow Removal status", e);
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(e.getMessage());
        }
    }
}