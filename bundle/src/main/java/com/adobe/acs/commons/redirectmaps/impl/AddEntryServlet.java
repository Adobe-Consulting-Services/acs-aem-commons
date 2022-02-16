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

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.util.RequireAem;

/**
 * Servlet for adding a line into the redirect map text file
 */
@SlingServlet(methods = { "POST" }, resourceTypes = {
        "acs-commons/components/utilities/redirectmappage" }, selectors = {
                "addentry" }, extensions = { "json" }, metatype = false)
public class AddEntryServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = -1704915461516132101L;
    private static final Logger log = LoggerFactory.getLogger(AddEntryServlet.class);
    
    // Disable this feature on AEM as a Cloud Service
    @Reference(target="(distribution=classic)")
    transient RequireAem requireAem;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        log.trace("doPost");

        String source = request.getParameter("source");
        String target = request.getParameter("target");
        log.debug("Adding entry with {} {}", source, target);

        List<String> lines = RedirectEntriesUtils.readEntries(request);

        lines.add(source + " " + target);
        log.debug("Added entry...");

        RedirectEntriesUtils.updateRedirectMap(request, lines);
        RedirectEntriesUtils.writeEntriesToResponse(request, response, "Added entry " + source + " " + target);
    }
}