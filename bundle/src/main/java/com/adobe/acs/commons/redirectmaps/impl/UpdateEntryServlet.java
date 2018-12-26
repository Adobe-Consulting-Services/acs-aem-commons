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
package com.adobe.acs.commons.redirectmaps.impl;

import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_EXTENSIONS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_METHODS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_SELECTORS;

import java.io.IOException;
import java.util.List;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet for updating a line in the redirect map text file
 */
@Component(service = Servlet.class, property = { SLING_SERVLET_METHODS + "=POST",
        SLING_SERVLET_SELECTORS + "=updateentry", SLING_SERVLET_EXTENSIONS + "=json",
        SLING_SERVLET_RESOURCE_TYPES + "=acs-commons/components/utilities/redirectmappage" })
public class UpdateEntryServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = -1704915461516132101L;
    private static final Logger log = LoggerFactory.getLogger(UpdateEntryServlet.class);

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        log.trace("doPost");

        String source = request.getParameter("edit-source");
        String target = request.getParameter("edit-target");
        int idx = Integer.parseInt(request.getParameter("edit-id"), 10);
        log.debug("Updating entry {} with {} {}", idx, source, target);

        List<String> lines = RedirectEntriesUtils.readEntries(request);

        lines.set(idx, source + " " + target);
        log.debug("Updated entry...");

        log.trace("Saving lines {}", lines);
        RedirectEntriesUtils.updateRedirectMap(request, lines);
        RedirectEntriesUtils.writeEntriesToResponse(request, response,
                "Updated entry " + idx + " to " + source + " " + target);
    }
}
